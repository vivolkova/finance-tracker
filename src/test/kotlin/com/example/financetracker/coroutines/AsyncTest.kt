package com.example.financetracker.coroutines

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AsyncTest {

    private suspend fun loadIncome(): Int {
        delay(SHORT); return 500
    }

    private suspend fun loadExpense(): Int {
        delay(SHORT); return 300
    }

    @Test
    fun `async returns immediately without blocking`() = runTest {
        val deferred = async { loadIncome() }

        assertFalse(deferred.isCompleted)

        val income = deferred.await()
        assertTrue(deferred.isCompleted)
        assertEquals(500, income)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `two async run in parallel`() = runTest {
        val a = async { loadIncome() }
        val b = async { loadExpense() }
        val sum = a.await() + b.await()

        assertEquals(800, sum)
        assertEquals(100, currentTime)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sequential`() = runTest {
        val start = currentTime
        val a = loadIncome()
        val b = loadExpense()
        assertEquals(800, a + b)

        assertEquals(200, currentTime - start)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `async for list`() = runTest {
        val start = currentTime
        val deferred: List<Deferred<Int>> = (1..5).map { n ->
            async {
                delay(SHORT)
                n * 10
            }
        }
        deferred.awaitAll()
        assertEquals(100, currentTime - start)
    }

    @Test
    fun `async with timeout`() = runTest {
        val slow = async {
            delay(LONG)
        }
        val result =
            withTimeoutOrNull(TIMEOUT) { slow.await() }

        assertNull(result)
        if (result == null) slow.cancel()
        assertTrue(slow.isCancelled)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `lazy async does not start until await`() = runTest {
        var started = false
        val lazy = async(start = CoroutineStart.LAZY) {
            started = true
            42
        }

        advanceUntilIdle()
        assertFalse(started, "lazy async не должен стартовать до await/start")

        val result = lazy.await()
        assertTrue(started, "после await тело должно выполниться")
        assertEquals(42, result)
    }

    @Test
    fun `async with exception`() = runTest {
        supervisorScope {
            val failing = async { throw IllegalStateException("error inside coroutine") }

            val ex = assertFailsWith<IllegalStateException> {
                failing.await()
            }
            assertEquals("error inside coroutine", ex.message)
        }
    }

}