package com.goodfood.auth

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [UserService].
 *
 * Job stories covered (see Wiki: Job-Stories — Subscriber & Professional auth):
 *  - "When I am new, I want to register with email + password so that I can start tracking my food."
 *  - "When I return to the site, I want to log in so that I can see my own diary."
 *
 * Acceptance criteria exercised:
 *  - AC-AUTH-1  A new subscriber can register with a valid email + password.   [registerCreatesNewUser]
 *  - AC-AUTH-2  Registration is rejected when the email already exists.        [registerRejectsDuplicateEmail]
 *  - AC-AUTH-3  Login returns the user record for a correct password.          [authenticateReturnsUserForCorrectPassword]
 */
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