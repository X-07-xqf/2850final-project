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
 * Was the gate for `/pro/client/{id}` (issues #16/#17 — IDOR protection so a
 * professional could only access their assigned clients). v0.6.31 dropped the
 * gate per product request: every professional now sees every subscriber.
 * Function kept in code so it can be re-wired if a stricter access model is
 * reinstated later.
 */
@Suppress("unused")
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
        // v0.6.31: query every subscriber, not just supervised ones — pros can
        // see the full client base by design.
        val clients = transaction {
            Users.selectAll().where { Users.role eq "subscriber" }
                .orderBy(Users.fullName)
                .map { row ->
                    val clientId = row[Users.id]; val today = LocalDate.now()
                    val summary = DiaryService.getDailySummary(clientId, today); val goals = GoalService.getGoals(clientId)
                    val goalCal = goals?.get("calories") ?: BigDecimal("2000")
                    // v0.6.34 — NOT capped at 100, so over-eating clients (e.g. 150%)
                    // surface as such instead of being silently clamped to 100% / On Track.
                    val pct = if (goalCal > BigDecimal.ZERO) summary["calories"]!!.multiply(BigDecimal(100)).divide(goalCal, 0, RoundingMode.HALF_UP).toInt() else 0
                    // Status band: under 80% = under-eating, over 100% = over-eating —
                    // both flag "Needs Attention". 80–100% is the On-Track sweet spot.
                    val status = if (pct in 80..100) "On Track" else "Needs Attention"
                    mapOf<String, Any>("id" to clientId, "fullName" to row[Users.fullName],
                        "initials" to row[Users.fullName].split(" ").map { it.first() }.joinToString(""),
                        "calories" to (summary["calories"] ?: BigDecimal.ZERO).fmt(0), "goalCalories" to goalCal.fmt(0),
                        "compliance" to pct,
                        "complianceVisual" to pct.coerceAtMost(100),  // for the inline progress bar width
                        "status" to status)
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
        val dateStr = call.request.queryParameters["date"]
        val date = if (dateStr != null) LocalDate.parse(dateStr) else LocalDate.now()
        val unread = MessageService.getUnreadCount(session.userId)
        val client = transaction { Users.selectAll().where { Users.id eq clientId }.singleOrNull() } ?: return@get call.respondRedirect("/pro/dashboard")
        // Defence-in-depth: still require the URL parameter to point at a subscriber
        // (so pros can't scrape pro-on-pro detail pages via this endpoint).
        if (client[Users.role] != "subscriber") return@get call.respondRedirect("/pro/dashboard")
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

        // 7-day weekly ladder so the detail page can show recent calorie pattern.
        val today = LocalDate.now()
        val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val weeklyRaw = DiaryService.getWeeklySummary(clientId, monday)
        val goalCalDouble = (goals?.get("calories"))?.toDouble() ?: 2000.0
        val weekly = weeklyRaw.map { w ->
            val cals = (w["calories"] as BigDecimal).toDouble()
            val rowDate = w["date"] as LocalDate
            mapOf(
                "dayName" to (w["dayName"] ?: ""),
                "calories" to (w["calories"] as BigDecimal).fmt(0),
                "pct" to if (goalCalDouble > 0) ((cals / goalCalDouble) * 100).coerceAtMost(100.0).toInt() else 0,
                "isToday" to (rowDate == today),
                "isLogged" to (cals > 0)
            )
        }

        call.respond(ThymeleafContent("professional/client-detail", model(
            "session" to session,
            "client" to mapOf<String, Any>(
                "id" to client[Users.id],
                "fullName" to client[Users.fullName],
                "email" to client[Users.email],
                "role" to client[Users.role],
                "initials" to client[Users.fullName].split(" ").map { it.first() }.joinToString(""),
                "joinedAt" to client[Users.createdAt].format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            ),
            "date" to date, "dateFormatted" to date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            "prevDate" to date.minusDays(1), "nextDate" to date.plusDays(1),
            "meals" to meals, "summary" to displaySummary, "goals" to displayGoals,
            "weekly" to weekly,
            "unreadMessages" to unread, "activePage" to "clients")))
    }

    post("/pro/client/{id}/advice") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        if (session.role != "professional") return@post call.respondRedirect("/dashboard")
        val clientId = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondRedirect("/pro/dashboard")
        val message = call.receiveParameters()["message"] ?: ""
        if (message.isNotBlank()) MessageService.sendMessage(session.userId, clientId, message)
        call.respondRedirect("/pro/client/$clientId")
    }
}
