package com.example.financetracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableScheduling
class MySpringBootApplication

fun main(args: Array<String>) {
    runApplication<MySpringBootApplication>(*args)
}

