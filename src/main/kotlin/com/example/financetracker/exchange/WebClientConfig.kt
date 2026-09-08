package com.example.financetracker.exchange

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun exchangeRateWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("https://open.er-api.com/v6")
            .build()
}