package com.goodfood.recipes

import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.messages.MessageService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*

fun Route.recipeRoutes() {
    get("/recipes") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val query = call.request.queryParameters["q"]; val difficulty = call.request.queryParameters["difficulty"]
        val recipes = RecipeService.searchRecipes(query, difficulty)
        // Only show the Featured strip on the unfiltered landing — when the user
        // is searching, hide it so the page is just their results.
        val featured = if (query.isNullOrBlank() && (difficulty.isNullOrBlank() || difficulty == "all"))
            RecipeService.getFeatured(3) else emptyList()
        val unread = MessageService.getUnreadCount(session.userId)
        call.respond(ThymeleafContent("subscriber/recipes", model(
            "session" to session, "recipes" to recipes, "featured" to featured,
            "query" to (query ?: ""), "difficulty" to (difficulty ?: "all"),
            "unreadMessages" to unread, "activePage" to "recipes")))
    }

    get("/recipes/{id}") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val recipeId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondRedirect("/recipes")
        val detail = RecipeService.getRecipeDetail(recipeId) ?: return@get call.respondRedirect("/recipes")
        val isFav = RecipeService.isFavourite(session.userId, recipeId)
        val unread = MessageService.getUnreadCount(session.userId)
        call.respond(ThymeleafContent("subscriber/recipe-detail", model(
            "session" to session, "recipe" to detail, "isFavourite" to isFav, "unreadMessages" to unread, "activePage" to "recipes")))
    }

    post("/recipes/{id}/favourite") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val recipeId = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondRedirect("/recipes")
        RecipeService.toggleFavourite(session.userId, recipeId); call.respondRedirect("/recipes/$recipeId")
    }

    post("/recipes/{id}/rate") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val recipeId = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondRedirect("/recipes")
        val params = call.receiveParameters()
        RecipeService.addRating(session.userId, recipeId, params["rating"]?.toIntOrNull()?.coerceIn(1, 5) ?: 5, params["comment"]?.takeIf { it.isNotBlank() })
        call.respondRedirect("/recipes/$recipeId")
    }
}
