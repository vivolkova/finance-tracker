package com.example.financetracker.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Тесты синхронизации (Mutex / AtomicInteger).
 *
 * Здесь нужна РЕАЛЬНАЯ многопоточность, поэтому runBlocking + Dispatchers.Default
 * (пул потоков), а НЕ runTest (там один поток и виртуальное время — гонки/дедлока не будет).
 *
 * Проверяем, что фиксы дают КОРРЕКТНЫЙ детерминированный результат. Саму гонку
 * (unsafe-вариант) не тестируем: она вероятностная, такой тест был бы нестабильным.
 */
class MutexTest {

    private val iterations = 1000
    private val delay1ms = Duration.parse("1ms")

    @Test
    fun `mutex protects concurrent increments`() = runBlocking {
        var counter = 0
        val mutex = Mutex()
        coroutineScope {
            repeat(iterations) {
                launch(Dispatchers.Default) { mutex.withLock { counter++ } }
            }
        }
        assertEquals(iterations, counter, "withLock serializes ++, no lost updates")
    }

    @Test
    fun `atomic protects concurrent increments`() = runBlocking {
        val counter = AtomicInteger(0)
        coroutineScope {
            repeat(iterations) {
                launch(Dispatchers.Default) { counter.incrementAndGet() }
            }
        }
        assertEquals(iterations, counter.get(), "incrementAndGet is atomic")
    }

    @Test
    fun `thread-safe collection keeps all elements under concurrency`() = runBlocking {
        val list = Collections.synchronizedList(mutableListOf<Int>())
        coroutineScope {
            repeat(iterations) { i ->
                launch(Dispatchers.Default) { list.add(i) }
            }
        }
        assertEquals(iterations, list.size, "a thread-safe collection does not lose elements.")
    }

    @Test
    fun `nested withLock deadlocks because mutex is not reentrant`() = runBlocking {
        val mutex = Mutex()
        val timeout = delay200ms

        val result = withTimeoutOrNull(timeout) {
            mutex.withLock {
                mutex.withLock {            // повторный захват тем же владельцем → deadlock
                    "reached"
                }
            }
        }
        assertNull(result, "Nested withLock hangs - Mutex is not reentrant")
    }

    // Составная критическая секция: read -> suspend -> write. Атомик её не выразит, нужен Mutex.
    private class Wallet(var balance: Int) {
        val mutex = Mutex()
    }

    private suspend fun withdrawSafe(wallet: Wallet) {
        wallet.mutex.withLock {
            val current = wallet.balance
            delay(delay1ms)                     // suspend ВНУТРИ критической секции
            if (current >= 1) wallet.balance = current - 1
        }
    }

    @Test
    fun `mutex keeps compound section correct across suspend`() = runBlocking {
        val wallet = Wallet(iterations)
        coroutineScope {
            repeat(iterations) {
                launch(Dispatchers.Default) { withdrawSafe(wallet) }
            }
        }
        assertEquals(0, wallet.balance, "Mutex makes read->suspend->write atomic, writes are not lost")
    }
}
