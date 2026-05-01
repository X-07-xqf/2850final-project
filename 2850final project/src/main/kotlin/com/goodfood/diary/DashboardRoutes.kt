package com.goodfood.diary

import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.goals.GoalService
import com.goodfood.messages.MessageService
import com.goodfood.util.fmt
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
            val me = entries.filter { it["mealType"] == meal }
            val mealCalories = me.sumOf { it["calories"] as BigDecimal }
            mapOf(
                "type" to meal,
                "entries" to me.map { e -> mapOf(
                    "id" to e["id"],
                    "foodName" to e["foodName"],
                    "mealType" to e["mealType"],
                    "quantity" to (e["quantity"] as BigDecimal).fmt(1),
                    "calories" to (e["calories"] as BigDecimal).fmt(0),
                    "protein" to (e["protein"] as BigDecimal).fmt(1),
                    "carbs" to (e["carbs"] as BigDecimal).fmt(1),
                    "fat" to (e["fat"] as BigDecimal).fmt(1),
                    "notes" to e["notes"]
                ) },
                "calories" to mealCalories.fmt(0)
            )
        }
        fun pct(current: BigDecimal, goal: BigDecimal?): Int {
            if (goal == null || goal == BigDecimal.ZERO) return 0
            return current.multiply(BigDecimal(100)).divide(goal, 0, RoundingMode.HALF_UP).toInt().coerceAtMost(100)
        }
        val goalCal = goals?.get("calories") ?: BigDecimal("2000")
        val goalProt = goals?.get("protein") ?: BigDecimal("80")
        val goalCarb = goals?.get("carbs") ?: BigDecimal("250")
        val goalFat = goals?.get("fat") ?: BigDecimal("65")
        call.respond(ThymeleafContent("subscriber/dashboard", model(
            "session" to session, "date" to today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), "meals" to meals,
            "totalCalories" to (summary["calories"] ?: BigDecimal.ZERO).fmt(0),
            "totalProtein" to (summary["protein"] ?: BigDecimal.ZERO).fmt(1),
            "totalCarbs" to (summary["carbs"] ?: BigDecimal.ZERO).fmt(1),
            "totalFat" to (summary["fat"] ?: BigDecimal.ZERO).fmt(1),
            "goalCalories" to goalCal.fmt(0),
            "goalProtein" to goalProt.fmt(1),
            "goalCarbs" to goalCarb.fmt(1),
            "goalFat" to goalFat.fmt(1),
            "pctCalories" to pct(summary["calories"]!!, goalCal),
            "pctProtein" to pct(summary["protein"]!!, goalProt),
            "pctCarbs" to pct(summary["carbs"]!!, goalCarb),
            "pctFat" to pct(summary["fat"]!!, goalFat),
            "unreadMessages" to unread, "activePage" to "dashboard")))
    }
}
