package com.example.financetracker.exchange

import CurrencyNotFoundException
import ExternalRateException
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ExchangeRateRestClientService(
    private val restClient: RestClient
) {
    fun getRate(from: String, to: String): RateResult {     // ← НЕ Mono! обычный RateResult
        val base = from.uppercase()
        val target = to.uppercase()

        val response = restClient.get()
            .uri("/latest/{base}", base)
            .retrieve()
            .body(ErApiResponse::class.java)                // ← .body(), а не .bodyToMono()
            ?: throw ExternalRateException("Пустой ответ")

        if (response.result != "success" || response.rates == null) {
            throw ExternalRateException("API вернул ошибку для '$base'")
        }
        val rate = response.rates[target]
            ?: throw CurrencyNotFoundException(target)
        return RateResult(base, target, rate)
    }
}