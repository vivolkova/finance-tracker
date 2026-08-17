package com.example.financetracker.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Обработка пачки со счётом успехов/провалов.
 *
 * Ключевая мысль: coroutineScope при падении одного ребёнка ОТМЕНЯЕТ остальных и
 * пробрасывает исключение наверх. Чтобы посчитать успехи/провалы и НЕ потерять
 * остальные элементы — ошибку ловим ВНУТРИ каждой корутины (try/catch или runCatching).
 * Тогда наружу ничего не летит, coroutineScope никого не отменяет, обрабатываются ВСЕ.
 *
 * Нужна реальная многопоточность -> runBlocking + Dispatchers.Default.
 */
class BatchProcessingTest {

    private val batchSize = 1000

    // Детерминированно: чётные id "падают", нечётные — успех.
    private suspend fun process(id: Int) {
        if (id % 2 == 0) throw IllegalStateException("item $id failed")
    }

    @Test
    fun `counts successes and failures without one failure cancelling the batch`() = runBlocking {
        // AtomicInteger, а НЕ обычный Int++: корутины на разных потоках инкрементят
        // параллельно -> обычный ++ дал бы гонку и потерянные обновления.
        val succeeded = AtomicInteger(0)
        val failed = AtomicInteger(0)

        coroutineScope {
            (1..batchSize).forEach { id ->
                launch(Dispatchers.Default) {
                    try {
                        process(id)
                        succeeded.incrementAndGet()
                    } catch (e: Exception) {
                        failed.incrementAndGet()   // падение изолировано в этой корутине
                    }
                }
            }
        }   // выйдем, когда ВСЕ обработаны

        assertEquals(500, succeeded.get(), "нечётные обработаны успешно")
        assertEquals(500, failed.get(), "чётные упали, но батч не прервался")
        assertEquals(batchSize, succeeded.get() + failed.get(), "обработаны ВСЕ элементы")
    }

    @Test
    fun `aggregates results via runCatching and awaitAll`() = runBlocking {
        val results: List<Result<Unit>> = coroutineScope {
            (1..batchSize).map { id ->
                async(Dispatchers.Default) {
                    runCatching { process(id) }   // успех/провал -> Result, исключение не вылетает
                }
            }.awaitAll()                          // дождались всех
        }

        // подсчёт ПОСЛЕ завершения, в один поток -> атомики не нужны
        assertEquals(500, results.count { it.isSuccess })
        assertEquals(500, results.count { it.isFailure })
    }

    /**
     * Когда в этом сценарии нужен Mutex.
     *
     * AtomicInteger умеет только счётчик. Если надо собрать СПИСОК упавших id
     * (непотокобезопасная структура), то параллельные add() — это гонка.
     * Mutex сериализует доступ: add() идёт по одному, ничего не теряется.
     */
    @Test
    fun `mutex protects a shared list while collecting failed ids`() = runBlocking {
        val failedIds = mutableListOf<Int>()   // НЕ потокобезопасный список
        val mutex = Mutex()

        coroutineScope {
            (1..batchSize).forEach { id ->
                launch(Dispatchers.Default) {
                    try {
                        process(id)
                    } catch (e: Exception) {
                        mutex.withLock { failedIds.add(id) }   // add ПОД замком
                    }
                }
            }
        }

        assertEquals(500, failedIds.size, "все упавшие id собраны, без потерь из-за гонки")
        assertEquals((2..batchSize step 2).toList(), failedIds.sorted(), "это ровно чётные id")
    }

    /**
     * Составная секция (read-modify-write), которую AtomicInteger НЕ выразит.
     *
     * Задача: суммировать обработанные суммы ПО СЧЕТУ.
     * totals[accId] = (totals[accId] ?: 0) + amount — это ТРИ шага: прочитать по ключу,
     * прибавить, записать. Между ними может влезть другая корутина -> потерянные обновления
     * (и порча самой mutableMap). Atomic атомарен лишь для ОДНОГО инкремента одной переменной,
     * а тут единица согласованности — весь get-modify-put по ключу. Нужен Mutex.
     */
    @Test
    fun `mutex protects a compound read-modify-write on a shared map`() = runBlocking {
        val totals = mutableMapOf<Int, Int>()
        val mutex = Mutex()

        val accounts = 10
        val perAccount = 100          // 100 операций на каждый счёт
        val amount = 5

        coroutineScope {
            repeat(accounts * perAccount) { i ->
                val accId = i % accounts
                launch(Dispatchers.Default) {
                    mutex.withLock {
                        totals[accId] = (totals[accId] ?: 0) + amount   // read-modify-write под замком
                    }
                }
            }
        }

        assertEquals(accounts, totals.size)
        totals.values.forEach {
            assertEquals(perAccount * amount, it, "каждый счёт: 100 * 5 = 500, без потерь")
        }
    }
}
