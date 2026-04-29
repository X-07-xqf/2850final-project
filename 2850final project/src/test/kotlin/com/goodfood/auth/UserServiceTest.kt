package com.goodfood.auth

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserServiceTest {

    @Test
    fun registerCreatesNewUser() {
        TestDatabase.setup()

        val userId = UserService.register("Alice Smith", "alice@example.com", "password123", "subscriber")

        assertNotNull(userId)
    }

    @Test
    fun registerRejectsDuplicateEmail() {
        TestDatabase.setup()

        UserService.register("Alice Smith", "alice@example.com", "password123", "subscriber")
        val duplicate = UserService.register("Alice Two", "alice@example.com", "password456", "subscriber")

        assertNull(duplicate)
    }

    @Test
    fun authenticateReturnsUserForCorrectPassword() {
        TestDatabase.setup()

        UserService.register("Alice Smith", "alice@example.com", "password123", "subscriber")

        val result = UserService.authenticate("alice@example.com", "password123")

        assertNotNull(result)
        assertEquals("alice@example.com", result["email"])
    }
}