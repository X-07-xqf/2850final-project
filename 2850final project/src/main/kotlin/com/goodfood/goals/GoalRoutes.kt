package com.goodfood.goals

import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.diary.DiaryService
import com.goodfood.messages.MessageService
import com.goodfood.util.fmt
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.goalRoutes() {
    get("/goals") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val today = LocalDate.now()
        val currentMonday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val weekParam = call.request.queryParameters["week"]
        val weekStart: LocalDate = weekParam?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { currentMonday }
        } ?: currentMonday
        // Snap to Monday if the user passed any other day in the URL.
        val monday = weekStart.minusDays(weekStart.dayOfWeek.value.toLong() - 1)
        val goals = GoalService.getGoals(session.userId)
        val weekly = DiaryService.getWeeklySummary(session.userId, monday)
        val unread = MessageService.getUnreadCount(session.userId)
        val displayGoals = goals?.mapValues { (_, v) -> v?.fmt(1) ?: "" } ?: emptyMap()
        // Calorie goal drives the bar scale. If the user hasn't set one, every
        // bar reads as "empty" — pct stays 0 and the dashed empty track shows.
        val goalCals: Double = goals?.get("calories")?.toDouble() ?: 0.0
        val displayWeekly = weekly.map { w ->
            val calsRaw = w["calories"] as BigDecimal
            val cals = calsRaw.toDouble()
            val rowDate = w["date"] as LocalDate
            val pct: Int = if (goalCals > 0) ((cals / goalCals) * 100).coerceAtMost(100.0).toInt() else 0
            // 10% buffer so "right around goal" stays sage-green; only meaningfully-over days flip to clay.
            val isOver = goalCals > 0 && cals > goalCals * 1.10
            val isEmpty = cals == 0.0
            val isToday = rowDate == today
            val rowClass = buildString {
                when {
                    isEmpty -> append(" weekly-chart__row--empty")
                    isOver  -> append(" weekly-chart__row--over")
                }
                if (isToday) append(" weekly-chart__row--today")
            }
            mapOf(
                "date" to rowDate,
                "dayName" to (w["dayName"] ?: ""),
                "calories" to calsRaw.fmt(0),
                "pct" to pct,
                "rowClass" to rowClass
            )
        }
        val labelFmt = DateTimeFormatter.ofPattern("MMM d")
        val sunday = monday.plusDays(6)
        val weekLabel = "${monday.format(labelFmt)} – ${sunday.format(labelFmt)}, ${monday.year}"
        val prevWeek = monday.minusDays(7)
        val nextWeek = monday.plusDays(7)
        val isCurrentWeek = monday == currentMonday
        call.respond(ThymeleafContent("subscriber/goals", model(
            "session" to session, "goals" to displayGoals, "weekly" to displayWeekly,
            "weekLabel" to weekLabel, "prevWeek" to prevWeek, "nextWeek" to nextWeek,
            "isCurrentWeek" to isCurrentWeek,
            "unreadMessages" to unread, "activePage" to "goals")))
    }

    post("/goals") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val params = call.receiveParameters()
        GoalService.saveGoals(session.userId, params["calories"]?.toBigDecimalOrNull(), params["protein"]?.toBigDecimalOrNull(),
            params["carbs"]?.toBigDecimalOrNull(), params["fat"]?.toBigDecimalOrNull(), params["fiber"]?.toBigDecimalOrNull())
        call.respondRedirect("/goals")
    }
}
