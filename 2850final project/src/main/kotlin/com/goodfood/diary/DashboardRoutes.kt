package com.goodfood.diary

import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.goals.GoalService
import com.goodfood.messages.MessageService
import com.goodfood.recipes.RecipeService
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
        val isDayEmpty = entries.isEmpty()

        // Below-the-fold dashboard modules (v0.6.25): weekly summary, streak,
        // featured recipes, and the most recent coaching conversation.
        val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val weeklyRaw = DiaryService.getWeeklySummary(session.userId, monday)
        val goalCalDouble = goalCal.toDouble()
        val weekly = weeklyRaw.map { w ->
            val calsRaw = w["calories"] as BigDecimal
            val cals = calsRaw.toDouble()
            val rowDate = w["date"] as LocalDate
            mapOf(
                "dayName" to (w["dayName"] ?: ""),
                "calories" to calsRaw.fmt(0),
                "pct" to if (goalCalDouble > 0) ((cals / goalCalDouble) * 100).coerceAtMost(100.0).toInt() else 0,
                "isToday" to (rowDate == today),
                "isLogged" to (cals > 0)
            )
        }
        val loggedDays = weekly.count { it["isLogged"] == true }

        val featured = RecipeService.getFeatured(3).map { r ->
            mapOf(
                "id" to (r["id"] ?: 0),
                "title" to (r["title"] ?: ""),
                "coverEmoji" to (r["coverEmoji"] ?: "🍽️"),
                "coverTone" to (r["coverTone"] ?: "sage"),
                "avgRating" to (r["avgRating"] as BigDecimal).fmt(1),
                "reviewCount" to (r["reviewCount"] as Int),
                "calPerServing" to ((r["calPerServing"] as BigDecimal?) ?: BigDecimal.ZERO).fmt(0),
                "totalTime" to (r["totalTime"] as Int),
                "difficulty" to (r["difficulty"] ?: "easy")
            )
        }

        val partners = MessageService.getConversationPartners(session.userId)
        val latestPartner = partners.firstOrNull()

        call.respond(ThymeleafContent("subscriber/dashboard", model(
            "session" to session, "date" to today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), "meals" to meals,
            "isDayEmpty" to isDayEmpty,
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
            "weekly" to weekly, "loggedDays" to loggedDays,
            "featured" to featured, "latestPartner" to latestPartner,
            "unreadMessages" to unread, "activePage" to "dashboard")))
    }
}
