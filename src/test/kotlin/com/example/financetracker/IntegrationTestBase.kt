package com.example.financetracker


import com.example.financetracker.user.AuthResponse
import com.example.financetracker.user.RegisterRequest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
abstract class IntegrationTestBase {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    val email = "test@mail.ru"
    val password = "1234"
    lateinit var headers: HttpHeaders

    @BeforeEach
    fun registerUser() {
        val truncateStr =
            "truncate table categories, transactions, users, refresh_tokens, recurring_schedules restart identity cascade"
        jdbcTemplate.execute(truncateStr)

        val user = RegisterRequest(email, password)
        val token = restTemplate.postForEntity("/api/auth/register", user, AuthResponse::class.java).body?.accessToken!!
        headers = HttpHeaders().apply {
            setBearerAuth(token)
        }
    }

    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("finance_tracker_test")
            withUsername("postgres")
            withPassword("postgres")
            start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

}