package com.example.financetracker.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Разница между supervisorScope и coroutineScope при падении дочерней корутины.
 *
 *  - supervisorScope: падение одного ребёнка НЕ отменяет братьев и НЕ валит scope.
 *    Необработанное исключение уходит в CoroutineExceptionHandler ребёнка.
 *  - coroutineScope: падение одного ребёнка отменяет братьев и пробрасывается наружу.
 */
class ScopeTest {

    @Test
    fun `supervisorScope isolates a failing child from its sibling`() = runBlocking {
        var siblingDone = false
        var caught: Throwable? = null
        val handler = CoroutineExceptionHandler { _, e -> caught = e }

        supervisorScope {
            // падает с НЕОБРАБОТАННЫМ исключением
            launch(handler) {
                throw IllegalStateException("boom")
            }
            // сосед — должен спокойно доработать
            launch {
                delay(delay50ms)
                siblingDone = true
            }
        }

        assertTrue(siblingDone, "сосед завершился, несмотря на падение другого потомка")
        assertNotNull(caught, "необработанное исключение ушло в CoroutineExceptionHandler")
        assertIs<IllegalStateException>(caught)
        assertEquals("boom", caught!!.message)
    }

    @Test
    fun `coroutineScope cancels the sibling and rethrows when one child fails`() = runBlocking {
        var siblingCompleted = false
        var siblingCancelled = false

        val ex = assertFailsWith<IllegalStateException> {
            coroutineScope {
                // падение этого потомка отменит братьев и завалит весь scope.
                // delay — чтобы сосед гарантированно успел войти в свой delay до падения.
                launch {
                    delay(delay50ms)
                    throw IllegalStateException("boom")
                }
                // сосед не успеет завершиться — его отменят
                launch {
                    try {
                        delay(delay200ms)
                        siblingCompleted = true
                    } catch (e: CancellationException) {
                        siblingCancelled = true
                        throw e   // отмену пробрасываем дальше — обязательное правило
                    }
                }
            }
        }

        assertEquals("boom", ex.message, "исключение потомка пробросилось из coroutineScope")
        assertFalse(siblingCompleted, "сосед был отменён и НЕ завершился")
        assertTrue(siblingCancelled, "сосед получил CancellationException")
    }
}
