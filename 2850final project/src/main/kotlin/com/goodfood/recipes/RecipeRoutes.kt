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
        val query = call.request.queryParameters["q"]
        val difficulty = call.request.queryParameters["difficulty"]
        val calories = call.request.queryParameters["calories"]
        val protein = call.request.queryParameters["protein"]
        val time = call.request.queryParameters["time"]
        val recipes = RecipeService.searchRecipes(query, difficulty, calories, protein, time)
        // Featured strip only shows on the truly unfiltered landing — any active
        // filter (search or any of the four dropdowns) hides it so the page is
        // just the user's results.
        val anyFilterActive =
            !query.isNullOrBlank() ||
            (!difficulty.isNullOrBlank() && difficulty != "all") ||
            (!calories.isNullOrBlank() && calories != "all") ||
            (!protein.isNullOrBlank() && protein != "all") ||
            (!time.isNullOrBlank() && time != "all")
        val featured = if (!anyFilterActive) RecipeService.getFeatured(3) else emptyList()
        val unread = MessageService.getUnreadCount(session.userId)
        call.respond(ThymeleafContent("subscriber/recipes", model(
            "session" to session, "recipes" to recipes, "featured" to featured,
            "query" to (query ?: ""),
            "difficulty" to (difficulty ?: "all"),
            "calories" to (calories ?: "all"),
            "protein" to (protein ?: "all"),
            "time" to (time ?: "all"),
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
