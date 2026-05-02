package com.goodfood.professional

import com.goodfood.auth.Users
import com.goodfood.config.UserSession
import com.goodfood.config.model
import com.goodfood.diary.DiaryService
import com.goodfood.goals.GoalService
import com.goodfood.messages.MessageService
import com.goodfood.util.fmt
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Returns true when [professionalId] currently supervises [subscriberId] via an
 * active row in [ClientRelationships]. Used to gate the `/pro/client/{id}` routes
 * so a professional can only access their assigned clients (issues #16, #17).
 */
private fun hasActiveRelationship(professionalId: Int, subscriberId: Int): Boolean = transaction {
    ClientRelationships.selectAll().where {
        (ClientRelationships.professionalId eq professionalId) and
            (ClientRelationships.subscriberId eq subscriberId) and
            (ClientRelationships.status eq "active")
    }.limit(1).any()
}

fun Route.professionalRoutes() {
    get("/pro/dashboard") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        if (session.role != "professional") return@get call.respondRedirect("/dashboard")
        val unread = MessageService.getUnreadCount(session.userId)
        val clients = transaction {
            ClientRelationships.join(Users, JoinType.INNER, ClientRelationships.subscriberId, Users.id)
                .select(Users.columns + ClientRelationships.columns).where {
                    (ClientRelationships.professionalId eq session.userId) and (ClientRelationships.status eq "active")
                }.map { row ->
                    val clientId = row[Users.id]; val today = LocalDate.now()
                    val summary = DiaryService.getDailySummary(clientId, today); val goals = GoalService.getGoals(clientId)
                    val goalCal = goals?.get("calories") ?: BigDecimal("2000")
                    val pct = if (goalCal > BigDecimal.ZERO) summary["calories"]!!.multiply(BigDecimal(100)).divide(goalCal, 0, RoundingMode.HALF_UP).toInt() else 0
                    mapOf<String, Any>("id" to clientId, "fullName" to row[Users.fullName],
                        "initials" to row[Users.fullName].split(" ").map { it.first() }.joinToString(""),
                        "calories" to (summary["calories"] ?: BigDecimal.ZERO).fmt(0), "goalCalories" to goalCal.fmt(0),
                        "compliance" to pct.coerceAtMost(100), "status" to if (pct >= 60) "On Track" else "Needs Attention")
                }
        }
        call.respond(ThymeleafContent("professional/dashboard", model(
            "session" to session, "clients" to clients, "totalClients" to clients.size,
            "needAttention" to clients.count { it["status"] == "Needs Attention" }, "unreadMessages" to unread, "activePage" to "dashboard")))
    }

    get("/pro/client/{id}") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        if (session.role != "professional") return@get call.respondRedirect("/dashboard")
        val clientId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondRedirect("/pro/dashboard")
        // Authorisation gate: only show this client's data if the professional currently
        // supervises them. Closes #16 (IDOR — any client visible to any professional).
        if (!hasActiveRelationship(session.userId, clientId)) return@get call.respondRedirect("/pro/dashboard")
        val dateStr = call.request.queryParameters["date"]
        val date = if (dateStr != null) LocalDate.parse(dateStr) else LocalDate.now()
        val unread = MessageService.getUnreadCount(session.userId)
        val client = transaction { Users.selectAll().where { Users.id eq clientId }.singleOrNull() } ?: return@get call.respondRedirect("/pro/dashboard")
        val entries = DiaryService.getEntriesForDate(clientId, date); val summary = DiaryService.getDailySummary(clientId, date)
        val goals = GoalService.getGoals(clientId)
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
        val displaySummary = mapOf(
            "calories" to (summary["calories"] ?: BigDecimal.ZERO).fmt(0),
            "protein" to (summary["protein"] ?: BigDecimal.ZERO).fmt(1),
            "carbs" to (summary["carbs"] ?: BigDecimal.ZERO).fmt(1),
            "fat" to (summary["fat"] ?: BigDecimal.ZERO).fmt(1)
        )
        val displayGoals = goals?.mapValues { (_, v) -> v?.fmt(1) ?: "" } ?: emptyMap()
        call.respond(ThymeleafContent("professional/client-detail", model(
            "session" to session,
            "client" to mapOf<String, Any>("id" to client[Users.id], "fullName" to client[Users.fullName],
                "initials" to client[Users.fullName].split(" ").map { it.first() }.joinToString("")),
            "date" to date, "dateFormatted" to date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            "prevDate" to date.minusDays(1), "nextDate" to date.plusDays(1),
            "meals" to meals, "summary" to displaySummary, "goals" to displayGoals,
            "unreadMessages" to unread, "activePage" to "clients")))
    }

    post("/pro/client/{id}/advice") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        if (session.role != "professional") return@post call.respondRedirect("/dashboard")
        val clientId = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondRedirect("/pro/dashboard")
        // Authorisation gate: only let a professional message a current client of theirs.
        // Closes #17 (IDOR — any user could be messaged via the advice endpoint).
        if (!hasActiveRelationship(session.userId, clientId)) return@post call.respondRedirect("/pro/dashboard")
        val message = call.receiveParameters()["message"] ?: ""
        if (message.isNotBlank()) MessageService.sendMessage(session.userId, clientId, message)
        call.respondRedirect("/pro/client/$clientId")
    }
}
