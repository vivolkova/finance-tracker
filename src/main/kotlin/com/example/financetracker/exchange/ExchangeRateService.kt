package com.example.financetracker.exchange

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ExchangeRateService(
    private val webClient: WebClient
) {

    fun getRate(from: String, to: String): Mono<RateResult> {
        return webClient.get()
            .uri("/latest/{base}", from)                 // GET .../v6/latest/USD
            .retrieve()                                   // выполнить запрос
            .bodyToMono(ErApiResponse::class.java)        // тело JSON -> Mono<ErApiResponse>
            .timeout(Duration.ofSeconds(3))               // не ждать дольше 3 сек
            .map { response ->                            // ErApiResponse -> RateResult
                val rate = response.rates[to]
                    ?: throw CurrencyNotFoundException(to)
                RateResult(from = from, to = to, rate = rate)
            }
    }
}

class CurrencyNotFoundException(currency: String) :
    RuntimeException("Курс для валюты '$currency' не найден")