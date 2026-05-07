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

fun Application.configureSecurity() { //cookies for authenticated users
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400
            cookie.httpOnly = true
            cookie.extensions["SameSite"] = "Lax"
        }
    }
}
