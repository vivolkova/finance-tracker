package com.example.financetracker.exchange

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun exchangeRateWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("https://open.er-api.com/v6")
            .build()

    @Bean
    fun exchangeRateRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://open.er-api.com/v6")
            .build()
}