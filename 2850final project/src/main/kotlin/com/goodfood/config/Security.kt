package com.goodfood.config

import io.ktor.server.application.*
import io.ktor.server.sessions.*

data class UserSession(
    val userId: Int,
    val fullName: String,
    val email: String,
    val role: String
) {
    val initials: String
        get() = fullName.split(" ").filter { it.isNotEmpty() }.map { it.first().uppercase() }.joinToString("")
}

/**
 * Configure session cookies for authenticated users.
 *
 * Defence-in-depth on the cookie itself (closes issue #18):
 *  - httpOnly = true   — JavaScript can no longer read the cookie, so an XSS bug
 *                        alone is not enough to steal a session.
 *  - SameSite = Lax    — browsers will not send the cookie on cross-site POST
 *                        submissions, blocking the standard form-based CSRF path.
 *                        Lax (not Strict) keeps top-level GET navigations from
 *                        external links working as expected.
 *  - Secure is intentionally NOT forced here so `./gradlew run` over plain HTTP
 *    in dev / Codespaces keeps working. For a production deployment behind TLS,
 *    flip `cookie.secure = true` (or gate it on an env flag).
 */
fun Application.configureSecurity() {
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400
            cookie.httpOnly = true
            cookie.extensions["SameSite"] = "Lax"
        }
    }
}
