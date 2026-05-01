package com.goodfood

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * HTTP-level integration tests using Ktor's [testApplication] harness.
 *
 * Each test boots the real [Application.module] against a fresh in-memory H2
 * database (random per-test URL so concurrent execution is safe). The test
 * exercises the full request-response pipeline — routing, sessions, Thymeleaf
 * rendering — not just the service layer.
 *
 * Acceptance criteria exercised:
 *  - AC-INT-1  GET `/dashboard` without a session redirects to `/login`.        [unauthenticatedDashboardRedirectsToLogin]
 *  - AC-INT-2  GET `/login` returns 200 and contains the sign-in form.          [loginPageRenders]
 *  - AC-INT-3  GET `/api/food-search` requires a session.                       [foodSearchRequiresSession]
 */
class IntegrationTest {

    private fun ApplicationTestBuilder.useInMemoryDb() {
        environment {
            config = MapApplicationConfig(
                "database.driver" to "org.h2.Driver",
                "database.url" to "jdbc:h2:mem:test-${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MySQL",
                "database.user" to "",
                "database.password" to ""
            )
        }
    }

    @Test
    fun unauthenticatedDashboardRedirectsToLogin() = testApplication {
        useInMemoryDb()
        val client = createClient { followRedirects = false }

        val response = client.get("/dashboard")

        assertEquals(HttpStatusCode.Found, response.status, "should be a 302 redirect")
        assertEquals("/login", response.headers[HttpHeaders.Location])
    }

    @Test
    fun loginPageRenders() = testApplication {
        useInMemoryDb()

        val response = client.get("/login")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        // The login form has both a Sign-in submit button and the Good Food brand mark.
        assertTrue(body.contains("Good Food"), "login page should include the brand")
        assertTrue(body.contains("Sign in") || body.contains("Login"), "login page should include the sign-in CTA")
    }

    @Test
    fun foodSearchRequiresSession() = testApplication {
        useInMemoryDb()
        val client = createClient { followRedirects = false }

        val response = client.get("/api/food-search?q=apple")

        // No session cookie → must not return 200 with food data. The route's
        // existing behaviour is to redirect to /login (302) for protected routes.
        assertTrue(
            response.status == HttpStatusCode.Found || response.status == HttpStatusCode.Unauthorized,
            "expected 302 or 401, got ${response.status}"
        )
    }
}
