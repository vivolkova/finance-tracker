package com.example.financetracker.demo

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

/**
 * launch (НЕ часть приложения — запуск через main).
 *
 * launch запускает корутину «запустил и забыл» — НЕ возвращает результат,
 * возвращает Job (ручку: join дождаться, cancel отменить, статусы).
 * Отличие от async: async возвращает Deferred<T> с результатом (через await).
 */

private val SHORT = Duration.parse("100ms")
private val TICK = Duration.parse("50ms")
private val WAIT = Duration.parse("120ms")

fun main() = runBlocking {

    // ── 1. Базовый launch — «запустил и забыл», возвращает Job ──
    val job: Job = launch {
        delay(SHORT)
        println("1) background work completed")
    }
    println("1) launch returned Job, main continue working")
    job.join()                       // дождались завершения корутины
    println("1) job.isCompleted = ${job.isCompleted}")

    // ── 2. Управление через Job: отмена на середине + статусы ──
    val cancellable = launch {
        repeat(10) { i ->
            println("2) step $i")
            delay(TICK)              // suspend-точка: здесь корутина реагирует на cancel
        }
    }
    delay(WAIT)
    cancellable.cancel()             // отменили, не дожидаясь всех 10 шагов
    cancellable.join()
    println("2) isCancelled = ${cancellable.isCancelled}")

    // ── 3. Несколько launch + structured concurrency ──
    coroutineScope {
        launch { delay(TICK); println("3) task A") }
        launch { delay(TICK); println("3) task B") }
    }
    // сюда придём, только когда ОБЕ завершатся
    println("3) both finished")

    // ── 4. Кооперативная отмена: в CPU-цикле без suspend отмену проверяют вручную ──
    val worker = launch(Dispatchers.Default) {
        var i = 0L
        while (isActive) {           // без этой проверки цикл не остановить отменой
            i++
        }
        println("4) stopped: isActive -> false")
    }
    delay(TICK)
    worker.cancelAndJoin()           // отменить и дождаться завершения
    println("4) worker cancelled: isCancelled=${worker.isCancelled}")

    // ── 5. Исключение в launch всплывает СРАЗУ (в отличие от async — там на await) ──
    // Для launch необработанное исключение идёт в CoroutineExceptionHandler.
    val handler = CoroutineExceptionHandler { _, e ->
        println("5) caught exception: ${e.message}")
    }
    val scope = CoroutineScope(SupervisorJob())
    val failing = scope.launch(handler) {
        throw IllegalStateException("exception in launch")
    }
    failing.join()
    println("5) failing.isCancelled = ${failing.isCancelled}")
    scope.cancel()                   // прибираемся за собой
}
