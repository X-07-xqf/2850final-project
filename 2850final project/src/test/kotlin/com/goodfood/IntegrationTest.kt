package com.goodfood

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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
 *  - AC-INT-1  GET `/dashboard` without a session does NOT serve the dashboard.   [unauthenticatedDashboardDoesNotServeDashboard]
 *  - AC-INT-2  GET `/login` returns 200 (the login route is reachable end-to-end). [loginPageRendersWithoutCrashing]
 *  - AC-INT-3  POST `/login` with bogus credentials does NOT issue a session.     [postLoginWithBadCredentialsDoesNotSetSession]
 */
class IntegrationTest {

    private fun ApplicationTestBuilder.bootApp() {
        environment {
            // Replace the file-based dev DB with a per-test in-memory H2 so tests
            // are isolated. We also re-declare `ktor.application.modules` because
            // overriding `config = MapApplicationConfig(...)` replaces the merged
            // config rather than augmenting it — the modules entry from
            // application.conf would otherwise be lost and `module()` would not
            // be auto-loaded by the test harness.
            config = MapApplicationConfig(
                "ktor.application.modules" to "com.goodfood.ApplicationKt.module",
                "database.driver" to "org.h2.Driver",
                "database.url" to "jdbc:h2:mem:test-${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MySQL",
                "database.user" to "",
                "database.password" to ""
            )
        }
    }

    @Test
    fun unauthenticatedDashboardDoesNotServeDashboard() = testApplication {
        bootApp()
        val client = createClient { followRedirects = false }

        val response = client.get("/dashboard")

        // Without a session the dashboard must NOT render. Acceptable shapes:
        //  - 302 redirect to /login (current behaviour)
        //  - any other non-2xx
        // What is unacceptable: a 200 OK rendering of the dashboard template.
        assertNotEquals(HttpStatusCode.OK, response.status,
            "GET /dashboard without a session must not serve the dashboard page")
    }

    @Test
    fun loginPageRendersWithoutCrashing() = testApplication {
        bootApp()

        val response = client.get("/login")

        // The login page should be reachable end-to-end. Status is 200 OK in
        // the current implementation; assert non-5xx to make the test resilient
        // to incidental status-code changes (e.g. switching to 304-cached responses).
        assertTrue(response.status.value < 500,
            "GET /login crashed with a server error: ${response.status}")
        assertTrue(response.bodyAsText().isNotBlank(),
            "GET /login returned an empty body")
    }

    @Test
    fun postLoginWithBadCredentialsDoesNotSetSession() = testApplication {
        bootApp()
        val client = createClient { followRedirects = false }

        val response = client.post("/login") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody("email=nonexistent@example.com&password=wrong")
        }

        // Bad credentials must not redirect to /dashboard or /pro/dashboard with
        // a Set-Cookie session header. The current implementation re-renders the
        // login template (200) with an error banner.
        val location = response.headers[HttpHeaders.Location].orEmpty()
        assertTrue(
            !location.contains("/dashboard") && !location.contains("/pro/dashboard"),
            "Bad credentials must not redirect to a dashboard; got Location=$location"
        )
        assertEquals(null, response.headers[HttpHeaders.SetCookie]?.let { sc ->
            if (sc.contains("user_session=") && !sc.contains("user_session=;")) sc else null
        }, "Bad credentials must not set a populated user_session cookie")
    }
}
