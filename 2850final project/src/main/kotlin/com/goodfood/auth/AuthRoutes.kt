package com.goodfood.auth

import com.goodfood.config.UserSession
import com.goodfood.config.model
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*

fun Route.authRoutes() {
    get("/login") {
        val session = call.sessions.get<UserSession>()
        if (session != null) {
            call.respondRedirect(if (session.role == "professional") "/pro/dashboard" else "/dashboard")
            return@get
        }
        call.respond(ThymeleafContent("auth/login", model("error" to null)))
    }

    post("/login") {
        val params = call.receiveParameters()
        val email = params["email"] ?: ""; val password = params["password"] ?: ""
        val user = UserService.authenticate(email, password)
        if (user != null) {
            call.sessions.set(UserSession(user["id"] as Int, user["fullName"] as String, user["email"] as String, user["role"] as String))
            if (user["role"] == "professional") call.respondRedirect("/pro/dashboard") else call.respondRedirect("/dashboard")
        } else {
            call.respond(ThymeleafContent("auth/login", model("error" to "Invalid email or password")))
        }
    }

    post("/register") {
        val params = call.receiveParameters()
        val fullName = params["fullName"] ?: ""; val email = params["email"] ?: ""
        val password = params["password"] ?: ""; val confirmPassword = params["confirmPassword"] ?: ""
        val role = params["role"] ?: "subscriber"
        if (password != confirmPassword) { call.respond(ThymeleafContent("auth/login", model("error" to "Passwords do not match", "tab" to "register"))); return@post }
        if (fullName.isBlank() || email.isBlank() || password.length < 6) { call.respond(ThymeleafContent("auth/login", model("error" to "Please fill all fields (password min 6 chars)", "tab" to "register"))); return@post }
        val userId = UserService.register(fullName, email, password, role)
        if (userId != null) {
            call.sessions.set(UserSession(userId, fullName, email, role))
            if (role == "professional") call.respondRedirect("/pro/dashboard") else call.respondRedirect("/dashboard")
        } else {
            call.respond(ThymeleafContent("auth/login", model("error" to "Email already registered", "tab" to "register")))
        }
    }

    get("/logout") { call.sessions.clear<UserSession>(); call.respondRedirect("/login") }
}
