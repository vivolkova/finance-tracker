package com.example.financetracker.coroutines


import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class LaunchTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `launch returns immediately and completes after join`() = runTest {
        var done = false
        val job: Job = launch {
            delay(delay100ms)
            done = true
        }

        assertTrue(job.isActive, "coroutine is still active after launch")
        assertFalse(job.isCompleted, "and not finished yet")
        assertFalse(done, "body not completed yet")

        job.join()

        assertTrue(job.isCompleted, "coroutine finished after join")
        assertFalse(job.isActive)
        assertTrue(done, "body finished")
        assertEquals(100L, currentTime, "join waited for delay(100)")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancel stops job before it finishes`() = runTest {
        var steps = 0
        val cancellable = launch {
            repeat(10) {
                steps++
                delay(delay50ms)
            }
        }

        delay(delay120ms)
        cancellable.cancel()
        cancellable.join()

        assertTrue(cancellable.isCancelled, "coroutine cancelled")
        assertTrue(cancellable.isCompleted, "and finished")
        assertFalse(cancellable.isActive)
        assertTrue(steps < 10, "cancel before 10 steps")
        assertEquals(3, steps, "processed only 3 steps")
    }

    /* structured concurrency */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `coroutineScope waits for all children`() = runTest {
        var completed = 0

        coroutineScope {
            launch { delay(delay50ms); completed++ }
            launch { delay(delay50ms); completed++ }
        }

        assertEquals(2, completed, "both coroutines finished")
        assertEquals(50L, currentTime, "both coroutinse work in parallel")
    }

    /* cooperative cancellation */
    @Test
    fun `cpu loop is cancelled because it checks isActive`() = runBlocking {
        var stopped = false
        val worker = launch(Dispatchers.Default) {
            while (isActive) {
                // busy CPU-работа без suspend-точек
            }
            stopped = true
        }

        delay(delay50ms)
        worker.cancelAndJoin()

        assertTrue(worker.isCancelled, "job cancelled")
        assertTrue(stopped, "isActive checked")
    }

    @Test
    fun `launch exception goes to CoroutineExceptionHandler`() = runBlocking {
        var caught: Throwable? = null
        val handler = CoroutineExceptionHandler { _, e -> caught = e }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val failing = scope.launch(handler) {
            throw IllegalStateException("exception in launch")
        }
        failing.join()

        assertTrue(failing.isCancelled)

        assertNotNull(caught, "handler executed")
        assertIs<IllegalStateException>(caught)
        assertEquals("exception in launch", caught!!.message)

        scope.cancel()
    }
}