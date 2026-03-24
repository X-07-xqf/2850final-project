package com.goodfood.diary

import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.goals.GoalService
import com.goodfood.messages.MessageService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.dashboardRoutes() {
    get("/dashboard") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val today = LocalDate.now()
        val entries = DiaryService.getEntriesForDate(session.userId, today)
        val summary = DiaryService.getDailySummary(session.userId, today)
        val goals = GoalService.getGoals(session.userId)
        val unread = MessageService.getUnreadCount(session.userId)
        val meals = listOf("breakfast", "lunch", "snack", "dinner").map { meal ->
            val me = entries.filter { it["mealType"] == meal }; mapOf("type" to meal, "entries" to me, "calories" to me.sumOf { it["calories"] as BigDecimal })
        }
        fun pct(current: BigDecimal, goal: BigDecimal?): Int {
            if (goal == null || goal == BigDecimal.ZERO) return 0
            return current.multiply(BigDecimal(100)).divide(goal, 0, RoundingMode.HALF_UP).toInt().coerceAtMost(100)
        }
        call.respond(ThymeleafContent("subscriber/dashboard", model(
            "session" to session, "date" to today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), "meals" to meals,
            "totalCalories" to summary["calories"], "totalProtein" to summary["protein"], "totalCarbs" to summary["carbs"], "totalFat" to summary["fat"],
            "goalCalories" to (goals?.get("calories") ?: BigDecimal("2000")), "goalProtein" to (goals?.get("protein") ?: BigDecimal("80")),
            "goalCarbs" to (goals?.get("carbs") ?: BigDecimal("250")), "goalFat" to (goals?.get("fat") ?: BigDecimal("65")),
            "pctCalories" to pct(summary["calories"]!!, goals?.get("calories") ?: BigDecimal("2000")),
            "pctProtein" to pct(summary["protein"]!!, goals?.get("protein") ?: BigDecimal("80")),
            "pctCarbs" to pct(summary["carbs"]!!, goals?.get("carbs") ?: BigDecimal("250")),
            "pctFat" to pct(summary["fat"]!!, goals?.get("fat") ?: BigDecimal("65")),
            "unreadMessages" to unread, "activePage" to "dashboard")))
    }
}
