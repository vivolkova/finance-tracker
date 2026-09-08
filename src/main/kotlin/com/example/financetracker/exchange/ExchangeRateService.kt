package com.example.financetracker.exchange

import CurrencyNotFoundException
import ExternalRateException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ExchangeRateService(
    private val webClient: WebClient
) {

    fun getRate(from: String, to: String): Mono<RateResult> {
        val base = from.uppercase()          // ← нормализуем регистр (урок с RUB/rub)
        val target = to.uppercase()
        return webClient.get()
            .uri("/latest/{base}", base)
            .retrieve()
            .bodyToMono(ErApiResponse::class.java)
            .timeout(Duration.ofSeconds(3))
            .map { response ->
                if (response.result != "success" || response.rates == null) {
                    throw ExternalRateException("Внешний API вернул ошибку для '$base'")
                }
                val rate = response.rates[target]
                    ?: throw CurrencyNotFoundException(target)
                RateResult(from = base, to = target, rate = rate)
            }
            .onErrorMap(java.util.concurrent.TimeoutException::class.java) {
                ExternalRateException("Сервис курсов не ответил вовремя")
            }
    }
}

