package com.example.financetracker


import com.example.financetracker.category.CategoryDto
import com.example.financetracker.category.CategoryType
import com.example.financetracker.category.CreateCategoryRequest
import com.example.financetracker.transaction.CreateTransactionRequest
import com.example.financetracker.transaction.TransactionDto
import com.example.financetracker.transaction.TransactionType
import com.example.financetracker.user.AuthResponse
import com.example.financetracker.user.RegisterRequest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

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

        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7")).apply {
            withExposedPorts(6379)
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
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    fun addCategory(name: String, type: CategoryType): Pair<CategoryDto, HttpStatusCode>{
        val createRequest = CreateCategoryRequest(name, type)
        val result =  restTemplate.exchange(
            "/api/categories",
            HttpMethod.POST,
            HttpEntity(createRequest, headers),
            CategoryDto::class.java
        )
        return Pair(result.body!!, result.statusCode)
    }

    fun addTransaction(
        categoryName: String,
        categoryType: CategoryType,
        amount: BigDecimal,
        transactionType: TransactionType,
        description: String? = "test transaction",
    ): Long {
        val (categoryResponse, _) = addCategory(categoryName, categoryType)

        val request = CreateTransactionRequest(
            amount = amount,
            description = description,
            date = LocalDate.now(),
            type = transactionType,
            categoryId = categoryResponse.id
        )

        val result = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, HttpEntity(request, headers),
            TransactionDto::class.java
        )

        assertEquals(HttpStatus.CREATED, result.statusCode)
        return result.body!!.id
    }

}