package com.example.financetracker


import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
abstract class IntegrationTestBase {

    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("finance_tracker_test")
            withUsername("postgres")
            withPassword("postgres")
            .start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry){
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }
}