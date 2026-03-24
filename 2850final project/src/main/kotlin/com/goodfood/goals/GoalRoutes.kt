package com.goodfood.goals

import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.diary.DiaryService
import com.goodfood.messages.MessageService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*

fun Route.goalRoutes() {
    get("/goals") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val goals = GoalService.getGoals(session.userId)
        val weekly = DiaryService.getWeeklySummary(session.userId)
        val unread = MessageService.getUnreadCount(session.userId)
        call.respond(ThymeleafContent("subscriber/goals", model(
            "session" to session, "goals" to (goals ?: emptyMap()), "weekly" to weekly, "unreadMessages" to unread, "activePage" to "goals")))
    }

    post("/goals") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val params = call.receiveParameters()
        GoalService.saveGoals(session.userId, params["calories"]?.toBigDecimalOrNull(), params["protein"]?.toBigDecimalOrNull(),
            params["carbs"]?.toBigDecimalOrNull(), params["fat"]?.toBigDecimalOrNull(), params["fiber"]?.toBigDecimalOrNull())
        call.respondRedirect("/goals")
    }
}
