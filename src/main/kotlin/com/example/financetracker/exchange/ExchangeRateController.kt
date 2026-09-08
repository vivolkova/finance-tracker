package com.example.financetracker.exchange

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/rates")
@Tag(name = "Exchange rates", description = "Курсы валют через внешний API")
class ExchangeRateController(
    private val exchangeRateService: ExchangeRateService,
    private val restClientService: ExchangeRateRestClientService
) {

    @GetMapping
    @Operation(summary = "Получить курс валюты")
    fun getRate(
        @RequestParam from: String,
        @RequestParam to: String
    ): Mono<RateResult> = exchangeRateService.getRate(from, to)

    @GetMapping("/blocking")
    @Operation(summary = "Курс через RestClient (блокирующий)")
    fun getRateBlocking(
        @RequestParam from: String,
        @RequestParam to: String
    ): RateResult = restClientService.getRate(from, to)
}