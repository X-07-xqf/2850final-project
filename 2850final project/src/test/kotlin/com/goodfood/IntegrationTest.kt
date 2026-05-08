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
 *  - AC-INT-1  GET `/dashboard` without a session does NOT serve the dashboard.       [unauthenticatedDashboardDoesNotServeDashboard]
 *  - AC-INT-2  GET `/login` reaches the route layer without a server crash.           [loginPageRendersWithoutCrashing]
 *  - AC-INT-3  POST `/login` with bogus credentials does NOT issue a session.         [postLoginWithBadCredentialsDoesNotSetSession]
 *  - AC-INT-4  Register → reuse session cookie → /dashboard serves 200 OK.            [registerThenSessionCookieGrantsDashboard]
 *  - AC-INT-5  A subscriber cannot access any /pro/* route — role-based redirect.    [subscriberCannotAccessProRoute]
 *  - AC-INT-6  /api/messages/{id}/since/{lastId} without a session returns 401.       [unauthenticatedMessageApiReturns401]
 */
class IntegrationTest {

    private fun ApplicationTestBuilder.bootApp() {
        // Override only the database connection so each test gets an isolated
        // in-memory H2. We deliberately do NOT touch `ktor.application.modules`
        // here — `application { module() }` below registers the module manually,
        // which is the cleanest way for testApplication to load it without
        // having to encode a list-typed config value through MapApplicationConfig.
        environment {
            config = MapApplicationConfig(
                "database.driver" to "org.h2.Driver",
                "database.url" to "jdbc:h2:mem:test-${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MySQL",
                "database.user" to "",
                "database.password" to ""
            )
        }
        application { module() }
    }

    @Test
    fun unauthenticatedDashboardDoesNotServeDashboard() = testApplication {
        bootApp()
        val client = createClient { followRedirects = false }

        val response = client.get("/dashboard")

        // Without a session the dashboard route must NOT serve the dashboard.
        // Acceptable: any non-200 (the current implementation redirects 302 to /login).
        // Unacceptable: a 200 OK rendering of the dashboard template.
        assertNotEquals(HttpStatusCode.OK, response.status,
            "GET /dashboard without a session must not serve the dashboard page")
    }

    @Test
    fun loginPageRendersWithoutCrashing() = testApplication {
        bootApp()

        val response = client.get("/login")

        // The login page must be reachable end-to-end. Assert non-5xx (rather
        // than == 200) so the test stays green if the route ever switches to
        // a 304 cached response.
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

        // Bad credentials must not redirect to /dashboard or /pro/dashboard
        // and must not set a populated user_session cookie.
        val location = response.headers[HttpHeaders.Location].orEmpty()
        assertTrue(
            !location.contains("/dashboard") && !location.contains("/pro/dashboard"),
            "Bad credentials must not redirect to a dashboard; got Location=$location"
        )
        val session = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
            .firstOrNull { it.startsWith("user_session=") && !it.startsWith("user_session=;") }
        assertEquals(null, session,
            "Bad credentials must not set a populated user_session cookie; got: $session")
    }

    @Test
    fun registerThenSessionCookieGrantsDashboard() = testApplication {
        bootApp()
        val client = createClient { followRedirects = false }

        // 1. Register a fresh subscriber. The route both creates the user
        //    and sets the user_session cookie in one round-trip.
        val registerResp = client.post("/register") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody("fullName=Test+User&email=test@example.com&password=Aa1bcd&confirmPassword=Aa1bcd&role=subscriber")
        }
        val sessionCookie = registerResp.headers.getAll(HttpHeaders.SetCookie).orEmpty()
            .firstOrNull { it.startsWith("user_session=") && !it.startsWith("user_session=;") }
        assertTrue(sessionCookie != null, "Register must set a populated user_session cookie")

        // 2. Reuse the cookie to access /dashboard. The session is enough —
        //    no separate /login round-trip needed.
        val dashResp = client.get("/dashboard") {
            header(HttpHeaders.Cookie, sessionCookie!!.split(";").first())
        }
        assertEquals(HttpStatusCode.OK, dashResp.status,
            "Authenticated user must reach /dashboard with 200 OK")
    }

    @Test
    fun subscriberCannotAccessProRoute() = testApplication {
        bootApp()
        val client = createClient { followRedirects = false }

        // Register a fresh subscriber — the registration itself sets the
        // user_session cookie so we can reuse it directly.
        val regResp = client.post("/register") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody("fullName=Alice&email=alice@example.com&password=Aa1bcd&confirmPassword=Aa1bcd&role=subscriber")
        }
        val cookie = regResp.headers.getAll(HttpHeaders.SetCookie).orEmpty()
            .firstOrNull { it.startsWith("user_session=") && !it.startsWith("user_session=;") }!!
            .split(";").first()

        // Alice (subscriber) tries to open /pro/dashboard. Every /pro/* handler
        // checks `session.role == "professional"` and redirects otherwise — this
        // is the role-based defence we want to verify at the route layer.
        val resp = client.get("/pro/dashboard") {
            header(HttpHeaders.Cookie, cookie)
        }
        assertNotEquals(HttpStatusCode.OK, resp.status,
            "Subscriber must not get a 200 from /pro/dashboard")
        val location = resp.headers[HttpHeaders.Location].orEmpty()
        assertTrue(location.endsWith("/dashboard") || location.contains("/dashboard?"),
            "Subscriber must be redirected to /dashboard, got: $location")
    }

    @Test
    fun unauthenticatedMessageApiReturns401() = testApplication {
        bootApp()

        val response = client.get("/api/messages/1/since/0")

        assertEquals(HttpStatusCode.Unauthorized, response.status,
            "GET /api/messages/.../since/... without a session must return 401")
    }
}
