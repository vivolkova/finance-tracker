package com.example.financetracker.demo

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * async и Deferred (НЕ часть приложения — запуск через main).
 *
 * async запускает корутину и СРАЗУ возвращает Deferred<T> — «обещание результата»
 * (сам результат ещё считается). Результат забираешь через await() (suspend).
 * Deferred — это Job с результатом: его можно ждать, отменять, проверять статус.
 */

// Длительности вынесены в константы: Duration с явной единицей ("100ms")
private val SHORT = Duration.parse("100ms")
private val LONG = Duration.parse("5s")
private val TIMEOUT = Duration.parse("200ms")

private suspend fun loadIncome(): Int { delay(SHORT); return 500 }
private suspend fun loadExpense(): Int { delay(SHORT); return 300 }

fun main() = runBlocking {

    // ── 1. Базовый async + await ──
    // async НЕ блокирует: вернул Deferred, а мы пока делаем другое, потом await().
    val deferred: Deferred<Int> = async { loadIncome() }
    println("1) do something while loading...")
    val income = deferred.await()               // дождались результата
    println("1) income = $income")

    // ── 2. Параллельно vs последовательно (замер времени) ──
    // measureTime возвращает Duration; его toString уже с единицей (например, 104ms).
    val parallel = measureTime {
        val a = async { loadIncome() }          // обе стартуют СРАЗУ
        val b = async { loadExpense() }
        println("2) balance = ${a.await() + b.await()}")
    }
    println("2) parallel time ~$parallel")       // ~100ms

    val sequential = measureTime {
        val a = loadIncome()                    // подождали первую...
        val b = loadExpense()                   // ...потом вторую
        println("2) balance = ${a + b}")
    }
    println("2) sequential ~$sequential") // ~200ms

    // ── 3. awaitAll — ждём список Deferred разом ──
    val deferreds: List<Deferred<Int>> = (1..5).map { n -> async { delay(SHORT); n * 10 } }
    val results = deferreds.awaitAll()          // ждём все; результат — List, ~100мс всего
    println("3) awaitAll: $results")            // [10, 20, 30, 40, 50]

    // ── 4. Ждём Deferred с ограничением по времени через withTimeoutOrNull ──
    // withTimeoutOrNull(TIMEOUT) даёт блоку максимум TIMEOUT; не успел → возвращает null.
    val slow = async { delay(LONG); "ready" }
    val result = withTimeoutOrNull(TIMEOUT) { slow.await() }   // ждём максимум 200мс
    if (result == null) slow.cancel()           // не успел — отменяем и саму задачу
    println("4) result=$result, slow.isCancelled=${slow.isCancelled}")

    // ── 5. Ленивый async — стартует только по await()/start() ──
    val lazy = async(start = CoroutineStart.LAZY) { println("5) started lazy"); 42 }
    println("5) before await lazy coroutine not work")
    println("5) lazy await = ${lazy.await()}")  // вот здесь и стартует

    // ── 6. Исключение внутри async всплывает на await() ──
    // supervisorScope, чтобы падение этой async не отменило весь main.
    supervisorScope {
        val failing = async { throw IllegalStateException("error inside coroutine") }
        try {
            failing.await()
        } catch (e: IllegalStateException) {
            println("6) caught on await: ${e.message}")
        }
    }
}
