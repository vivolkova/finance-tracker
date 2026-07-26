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

private const val ITERATIONS = 10_00000

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

    // ── 2. ConcurrentModificationException: одновременная запись и перебор общей коллекции ──
    val list = mutableListOf<Int>()          // НЕ потокобезопасный список
    val cme = AtomicInteger(0)
    supervisorScope {                        // падение одной корутины не рушит остальные
        repeat(ITERATIONS) { i ->
            launch(Dispatchers.Default) {
                try {
                    list.add(i)
                } catch (e: Exception) {
                    // при параллельной записи ArrayList может повредиться
                }
            }
        }
        repeat(20000) {
            launch(Dispatchers.Default) {
                try {
                    list.sum()               // перебор во время add
                } catch (e: ConcurrentModificationException) {
                    cme.incrementAndGet()
                } catch (e: Exception) {
                    // иные повреждения коллекции
                }
            }
        }
    }
    println("2) ConcurrentModificationException caught: ${cme.get()} times")
    println("2) size (expected $ITERATIONS): ${list.size}   <- can be less = loss/damage")

}
