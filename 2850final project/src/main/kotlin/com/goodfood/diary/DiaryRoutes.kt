package com.goodfood.diary

import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.goals.GoalService
import com.goodfood.messages.MessageService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.diaryRoutes() {
    get("/diary") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val dateStr = call.request.queryParameters["date"]
        val date = if (dateStr != null) LocalDate.parse(dateStr) else LocalDate.now()
        val entries = DiaryService.getEntriesForDate(session.userId, date)
        val summary = DiaryService.getDailySummary(session.userId, date)
        val goals = GoalService.getGoals(session.userId)
        val unread = MessageService.getUnreadCount(session.userId)
        val meals = listOf("breakfast", "lunch", "snack", "dinner").map { meal ->
            val me = entries.filter { it["mealType"] == meal }; mapOf("type" to meal, "entries" to me, "calories" to me.sumOf { it["calories"] as BigDecimal })
        }
        call.respond(ThymeleafContent("subscriber/diary", model(
            "session" to session, "date" to date, "dateFormatted" to date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            "prevDate" to date.minusDays(1), "nextDate" to date.plusDays(1), "meals" to meals, "summary" to summary,
            "goals" to (goals ?: emptyMap()), "unreadMessages" to unread, "activePage" to "diary")))
    }

    post("/diary/add") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val params = call.receiveParameters()
        val foodItemId = params["foodItemId"]?.toIntOrNull() ?: return@post call.respondRedirect("/diary")
        val mealType = params["mealType"] ?: "breakfast"; val grams = params["grams"]?.toBigDecimalOrNull() ?: BigDecimal("100")
        val dateStr = params["date"]; val date = if (dateStr != null) LocalDate.parse(dateStr) else LocalDate.now()
        DiaryService.addEntry(session.userId, foodItemId, mealType, grams, date, params["notes"]?.takeIf { it.isNotBlank() })
        call.respondRedirect("/diary?date=$date")
    }

    post("/diary/delete/{id}") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val entryId = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondRedirect("/diary")
        DiaryService.deleteEntry(entryId, session.userId)
        call.respondRedirect("/diary?date=${call.request.queryParameters["date"] ?: LocalDate.now()}")
    }

    get("/api/food-search") {
        val q = call.request.queryParameters["q"] ?: ""; call.respond(DiaryService.searchFood(q))
    }
}
