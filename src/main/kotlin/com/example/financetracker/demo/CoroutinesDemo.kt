package com.example.financetracker.demo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Учебный пример корутин (НЕ часть приложения — запускается вручную через main).
 * Показывает: suspend, async/await, launch, structured concurrency, Dispatchers,
 * race condition на общей переменной и два способа её починить.
 */

// suspend-функция может приостанавливаться БЕЗ блокировки потока.
// delay — это не Thread.sleep: на время паузы поток освобождается для другой работы.
private suspend fun loadIncome(): Int {
    delay(100)          // имитация обращения к БД/сервису
    return 500
}

private suspend fun loadExpense(): Int {
    delay(100)
    return 300
}

fun main() = runBlocking {

    // ── 1. async/await: две независимые задачи выполняются ПАРАЛЛЕЛЬНО ──
    // Обе стартуют сразу; общее время ~100 мс, а не 200 мс как при последовательном вызове.
    val income = async { loadIncome() }
    val expense = async { loadExpense() }
    println("Balance: ${income.await() - expense.await()}")   // 200

    // ── 2. launch + structured concurrency ──
    // coroutineScope не завершится, пока не отработают ВСЕ запущенные в нём корутины.
    coroutineScope {
        launch { delay(50); println("Task A ready") }
        launch { delay(30); println("Task B ready") }
    }
    println("Both tasks finished")

    // ── 3. RACE CONDITION: 1000 корутин увеличивают ОБЩУЮ переменную ──
    // unsafe++ = "прочитать, прибавить, записать" — не атомарно. Корутины на разных
    // потоках (Dispatchers.Default) перетирают значения друг друга → потерянные обновления.
    var unsafe = 0
    coroutineScope {
        repeat(1000) {
            launch(Dispatchers.Default) { unsafe++ }
        }
    }
    println("unsafe (expected 1000): $unsafe")   // почти всегда МЕНЬШЕ 1000

    // ── 4a. Фикс через Mutex — корутинная блокировка ──
    // withLock пускает внутрь только одну корутину за раз (критическая секция).
    var safeMutex = 0
    val mutex = Mutex()
    coroutineScope {
        repeat(1000) {
            launch(Dispatchers.Default) { mutex.withLock { safeMutex++ } }
        }
    }
    println("safeMutex: $safeMutex")            // ровно 1000

    // ── 4b. Фикс через AtomicInteger — атомарная операция без блокировки ──
    val safeAtomic = AtomicInteger(0)
    coroutineScope {
        repeat(1000) {
            launch(Dispatchers.Default) { safeAtomic.incrementAndGet() }
        }
    }
    println("safeAtomic: ${safeAtomic.get()}")  // ровно 1000
}
