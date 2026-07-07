package com.example.financetracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableScheduling
@EnableCaching
class MySpringBootApplication {

    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}

fun main(args: Array<String>) {
    runApplication<MySpringBootApplication>(*args)
}

