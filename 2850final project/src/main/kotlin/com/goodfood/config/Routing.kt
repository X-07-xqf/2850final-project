package com.goodfood.config

import com.goodfood.auth.authRoutes
import com.goodfood.diary.dashboardRoutes
import com.goodfood.diary.diaryRoutes
import com.goodfood.goals.goalRoutes
import com.goodfood.messages.messageRoutes
import com.goodfood.professional.professionalRoutes
import com.goodfood.profile.profileRoutes
import com.goodfood.recipes.recipeRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*

fun Application.configureRouting() {
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondRedirect("/login")
        }
    }

    routing {
        staticResources("/static", "static")

        authRoutes()
        dashboardRoutes()
        diaryRoutes()
        recipeRoutes()
        goalRoutes()
        messageRoutes()
        profileRoutes()
        professionalRoutes()

        // The landing page is the entry point for everyone — signed-out and signed-in
        // alike. The template adapts its CTAs based on whether `session` is present
        // (Sign in / Start free vs Go to dashboard / Sign out).
        get("/") {
            val session = call.sessions.get<UserSession>()
            call.respond(ThymeleafContent("landing", model("session" to session)))
        }
    }
}

fun model(vararg pairs: Pair<String, Any?>): Map<String, Any> =
    pairs.filter { it.second != null }.associate { it.first to it.second!! }
