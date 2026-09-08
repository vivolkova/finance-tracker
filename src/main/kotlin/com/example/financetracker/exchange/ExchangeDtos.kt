package com.example.financetracker.exchange

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

// 1. Модель под ОТВЕТ внешнего API (как приходит JSON)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ErApiResponse(
    val result: String,
    @JsonProperty("base_code") val baseCode: String,
    val rates: Map<String, BigDecimal>
)

// 2. Наш аккуратный ответ клиенту
data class RateResult(
    val from: String,
    val to: String,
    val rate: BigDecimal
)