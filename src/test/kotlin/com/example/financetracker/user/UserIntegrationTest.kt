package com.example.financetracker.user

import com.example.financetracker.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserIntegrationTest: IntegrationTestBase(){

    @Test
    fun `get`(){
        val result = restTemplate.getForEntity("/api/users", Array<UserDto>::class.java)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(1, result.body!!.size)
        assertTrue(result.body!!.any { it.email == email })
    }
}