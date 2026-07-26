package com.example.financetracker.demo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.atomic.AtomicInteger

/**
 * Единственное, что НЕ переносится в юнит-тесты — демонстрация САМОЙ гонки.
 *
 * Гонка вероятностна (иногда не проявляется), поэтому ассертить её в тестах нельзя:
 * такой тест был бы flaky. Тесты проверяют КОРРЕКТНОСТЬ ФИКСОВ (см. MutexTest),
 * а здесь можно вручную УВИДЕТЬ проблему.
 *
 * Запуск вручную через main().
 */

private const val ITERATIONS = 1_000_000   // для счётчика (потерянные обновления)
private const val LIST_WRITES = 100_000    // для гонки на коллекции

fun main() = runBlocking {

    // ── 1. Потерянные обновления: counter++ на общей переменной без синхронизации ──
    // ++ = "прочитать -> прибавить -> записать" (не атомарно). Корутины на разных потоках
    // перетирают значения друг друга -> итог почти всегда МЕНЬШЕ ITERATIONS.
    var unsafe = 0
    coroutineScope {
        repeat(ITERATIONS) {
            launch(Dispatchers.Default) { unsafe++ }
        }
    }
    println("1) unsafe counter (expected $ITERATIONS): $unsafe   <- usually less = lost updates")

    // ── 2. Гонка на коллекции: перебор ВО ВРЕМЯ изменения ──
    // Ключ к воспроизведению CME(ConcurrentModificationException.) — широкое окно: читатели перебирают список,
    // ПОКА писатели его наполняют. Тогда перебор и add реально пересекаются во времени.
    val list = mutableListOf<Int>()          // НЕ потокобезопасный список
    val cme = AtomicInteger(0)
    val other = AtomicInteger(0)
    supervisorScope {
        // писатели: параллельно добавляют (write-write гонка -> потери/повреждение)
        val writers = launch {
            repeat(LIST_WRITES) { i ->
                launch(Dispatchers.Default) {
                    try { list.add(i) } catch (e: Exception) { other.incrementAndGet() }
                }
            }
        }
        // читатели: перебирают, ПОКА писатели активны (максимальное перекрытие во времени)
        repeat(4) {
            launch(Dispatchers.Default) {
                while (writers.isActive) {
                    try {
                        list.sum()               // перебор
                    } catch (e: ConcurrentModificationException) {
                        cme.incrementAndGet()
                    } catch (e: Exception) {
                        other.incrementAndGet()  // ArrayIndexOutOfBounds и прочие повреждения
                    }
                }
            }
        }
        writers.join()
    }
    println("2) ConcurrentModificationException caught: ${cme.get()} times")
    println("2) other corruption exceptions: ${other.get()} times")
    println("2) size (expected $LIST_WRITES): ${list.size}   <- can be less = loss/damage")

}
