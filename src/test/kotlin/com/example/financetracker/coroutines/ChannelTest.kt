package com.example.financetracker.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Демо Channel (учебное, не привязано к логике проекта).
 *
 * Channel — ГОРЯЧАЯ очередь между корутинами: одни кладут (send), другие забирают (receive).
 * Unicast: каждое значение достаётся РОВНО ОДНОМУ получателю. close() завершает for-in.
 *
 * Нужна реальная многопоточность -> runBlocking + Dispatchers.Default (не runTest).
 */
class ChannelTest {

    @Test
    fun `producer sends and consumer receives until closed`() = runBlocking {
        val channel = Channel<Int>()
        val received = mutableListOf<Int>()

        coroutineScope {
            launch {                          // производитель
                for (i in 1..5) channel.send(i)
                channel.close()               // сигнал: данные кончились
            }
            launch {                          // потребитель
                for (x in channel) received.add(x)
            }
        }

        assertEquals(listOf(1, 2, 3, 4, 5), received)
    }

    @Test
    fun `fan-out delivers each item to exactly one worker`() = runBlocking {
        val channel = Channel<Int>()
        val processed = AtomicInteger(0)

        coroutineScope {
            launch {                          // производитель: 100 задач
                repeat(100) { channel.send(it) }
                channel.close()
            }
            repeat(4) {                       // 4 воркера разбирают ОДИН канал
                launch(Dispatchers.Default) {
                    for (task in channel) {
                        processed.incrementAndGet()   // каждая задача обработана РОВНО раз
                    }
                }
            }
        }

        // сумма ровно 100: нет дублей (unicast) и нет потерь
        assertEquals(100, processed.get())
    }

    @Test
    fun `buffered channel accepts up to capacity without a receiver`() = runBlocking {
        val channel = Channel<Int>(capacity = 3)

        channel.send(1)          // не блокирует — есть место в буфере
        channel.send(2)
        channel.send(3)
        channel.close()

        val received = mutableListOf<Int>()
        for (x in channel) received.add(x)

        assertEquals(listOf(1, 2, 3), received)
    }
}
