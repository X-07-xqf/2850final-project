package com.goodfood.profile

import com.goodfood.auth.Users
import com.goodfood.auth.UserService
import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.messages.MessageService
import com.goodfood.recipes.RecipeService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*

fun Route.profileRoutes() {
    get("/profile") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val user = UserService.getById(session.userId) ?: return@get call.respondRedirect("/login")
        val favourites = RecipeService.getUserFavourites(session.userId)
        val unread = MessageService.getUnreadCount(session.userId)
        call.respond(ThymeleafContent("subscriber/profile", model(
            "session" to session,
            "user" to mapOf<String, Any>("fullName" to user[Users.fullName], "email" to user[Users.email], "role" to user[Users.role]),
            "favourites" to favourites, "unreadMessages" to unread, "activePage" to "profile")))
    }

    post("/profile") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val params = call.receiveParameters()
        val fullName = params["fullName"] ?: session.fullName; val email = params["email"] ?: session.email
        UserService.updateProfile(session.userId, fullName, email, params["password"]?.takeIf { it.isNotBlank() })
        call.sessions.set(session.copy(fullName = fullName, email = email))
        call.respondRedirect("/profile")
    }
}
