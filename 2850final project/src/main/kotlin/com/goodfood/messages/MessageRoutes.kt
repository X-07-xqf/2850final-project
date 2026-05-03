package com.goodfood.messages

import com.goodfood.auth.UserService
import com.goodfood.auth.Users
import com.goodfood.config.UserSession
import com.goodfood.config.model
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Group a flat conversation list by `sentDate`, labelling each group as
 * "Today" / "Yesterday" / day-of-week (within the last 6 days) / "MMM d, yyyy".
 * Returns a list of `{label, messages}` maps so the template can render
 * sticky date pills between message clusters (Telegram-style).
 */
private fun groupByDate(conversation: List<Map<String, Any>>, today: LocalDate): List<Map<String, Any>> {
    if (conversation.isEmpty()) return emptyList()
    val dayOfWeekFmt = DateTimeFormatter.ofPattern("EEEE")
    val absoluteFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return conversation.groupBy { it["sentDate"] as LocalDate }
        .toSortedMap()
        .map { (date, msgs) ->
            val label = when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                date.isAfter(today.minusDays(7)) -> date.format(dayOfWeekFmt)
                else -> date.format(absoluteFmt)
            }
            mapOf("label" to label, "messages" to msgs)
        }
}

fun Route.messageRoutes() {
    get("/messages") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val partners = MessageService.getConversationPartners(session.userId)
        val directory = MessageService.getEligibleNewPartners(session.userId, session.role)
        val unread = MessageService.getUnreadCount(session.userId)
        val firstPartner = partners.firstOrNull(); val firstPartnerId = firstPartner?.get("partnerId") as? Int
        val conversation = if (firstPartnerId != null) MessageService.getConversation(session.userId, firstPartnerId) else emptyList()
        val groups = groupByDate(conversation, LocalDate.now())
        val template = if (session.role == "professional") "professional/messages" else "subscriber/messages"
        call.respond(ThymeleafContent(template, model(
            "session" to session, "partners" to partners, "directory" to directory,
            "conversation" to conversation, "conversationGroups" to groups,
            "activePartnerId" to firstPartnerId, "activePartnerName" to (firstPartner?.get("partnerName") ?: ""),
            "activePartnerInitials" to (firstPartner?.get("partnerInitials") ?: ""),
            "activePartnerRole" to (firstPartner?.get("partnerRole") ?: ""),
            "unreadMessages" to unread, "activePage" to "messages")))
    }

    get("/messages/{partnerId}") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val partnerId = call.parameters["partnerId"]?.toIntOrNull() ?: return@get call.respondRedirect("/messages")
        val partners = MessageService.getConversationPartners(session.userId)
        val directory = MessageService.getEligibleNewPartners(session.userId, session.role)
        val conversation = MessageService.getConversation(session.userId, partnerId)
        val groups = groupByDate(conversation, LocalDate.now())
        val partner = UserService.getById(partnerId)
        val unread = MessageService.getUnreadCount(session.userId)
        val pName = partner?.get(Users.fullName) ?: ""
        val pInitials = pName.split(" ").filter { it.isNotEmpty() }.map { it.first().uppercase() }.joinToString("")
        val pRole = partner?.get(Users.role) ?: ""
        val template = if (session.role == "professional") "professional/messages" else "subscriber/messages"
        call.respond(ThymeleafContent(template, model(
            "session" to session, "partners" to partners, "directory" to directory,
            "conversation" to conversation, "conversationGroups" to groups,
            "activePartnerId" to partnerId, "activePartnerName" to pName,
            "activePartnerInitials" to pInitials, "activePartnerRole" to pRole,
            "unreadMessages" to unread, "activePage" to "messages")))
    }

    post("/messages/{partnerId}") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
        val partnerId = call.parameters["partnerId"]?.toIntOrNull() ?: return@post call.respondRedirect("/messages")
        val message = call.receiveParameters()["message"] ?: ""
        if (message.isNotBlank()) MessageService.sendMessage(session.userId, partnerId, message)
        call.respondRedirect("/messages/$partnerId")
    }
}
