package com.example.financetracker.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Демо Flow (учебное, не привязано к логике проекта).
 *
 * Flow — ХОЛОДНЫЙ поток данных: код внутри flow { } не выполняется, пока нет collect;
 * значения приходят по одному во времени; операторы (map/filter/take) ленивые.
 */
class FlowTest {

    @Test
    fun `flow is cold - body does not run until collect`() = runTest {
        var started = false
        val f = flow {
            started = true
            emit(1)
        }

        assertFalse(started, "тело flow НЕ выполняется просто от создания")
        f.collect { }
        assertTrue(started, "тело выполняется только при collect")
    }

    @Test
    fun `flow restarts from scratch on each collect`() = runTest {
        var runs = 0
        val f = flow {
            runs++
            emit(1);// emit(2)
        }

        f.collect { }
        f.collect { }

        assertEquals(2, runs, "каждый collect запускает flow заново")
    }

    @Test
    fun `operators transform values lazily`() = runTest {
        val result = flowOf(1, 2, 3, 4)
            .filter { it % 2 == 0 }
            .map { it * 10 }
            .toList()

        assertEquals(listOf(20, 40), result)
    }

    @Test
    fun `map and collect interleave per element - not all-then-all`() = runTest {
        val order = mutableListOf<String>()

        flowOf(1, 2, 3)
            .map { order.add("map $it"); it }
            .collect { order.add("collect $it") }

        // именно чередование, а не "map 1,2,3" потом "collect 1,2,3"
        assertEquals(
            listOf("map 1", "collect 1", "map 2", "collect 2", "map 3", "collect 3"),
            order
        )
    }

    @Test
    fun `take stops an infinite flow early - laziness`() = runTest {
        var produced = 0
        val result = flow {
            var i = 1
            while (true) {            // бесконечный источник
                produced++
                emit(i++)
            }
        }.take(3).toList()

        assertEquals(listOf(1, 2, 3), result)
        assertEquals(3, produced, "take(3) произвёл ровно 3 значения, не больше")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `emit suspends until the slow collector is ready - backpressure`() = runTest {
        val times = mutableListOf<Long>()

        flow {
            emit(1)
            emit(2)
        }.collect {
            times.add(currentTime)
            delay(delay100ms)         // медленный потребитель
        }

        // второй emit дождался, пока collector обработает первый (виртуальное время)
        assertEquals(listOf(0L, 100L), times)
    }
}
