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
        val displayWeekly = weekly.map { w -> mapOf(
            "date" to w["date"],
            "dayName" to w["dayName"],
            "calories" to (w["calories"] as BigDecimal).fmt(0)
        ) }
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
