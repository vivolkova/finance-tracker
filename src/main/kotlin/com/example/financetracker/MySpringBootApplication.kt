package com.example.financetracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class MySpringBootApplication

fun main(args: Array<String>) {
    runApplication<MySpringBootApplication>(*args)
}