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

fun Route.messageRoutes() {
    get("/messages") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val partners = MessageService.getConversationPartners(session.userId)
        val unread = MessageService.getUnreadCount(session.userId)
        val firstPartner = partners.firstOrNull(); val firstPartnerId = firstPartner?.get("partnerId") as? Int
        val conversation = if (firstPartnerId != null) MessageService.getConversation(session.userId, firstPartnerId) else emptyList()
        val template = if (session.role == "professional") "professional/messages" else "subscriber/messages"
        call.respond(ThymeleafContent(template, model(
            "session" to session, "partners" to partners, "conversation" to conversation,
            "activePartnerId" to firstPartnerId, "activePartnerName" to (firstPartner?.get("partnerName") ?: ""),
            "activePartnerInitials" to (firstPartner?.get("partnerInitials") ?: ""),
            "unreadMessages" to unread, "activePage" to "messages")))
    }

    get("/messages/{partnerId}") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
        val partnerId = call.parameters["partnerId"]?.toIntOrNull() ?: return@get call.respondRedirect("/messages")
        val partners = MessageService.getConversationPartners(session.userId)
        val conversation = MessageService.getConversation(session.userId, partnerId)
        val partner = UserService.getById(partnerId)
        val unread = MessageService.getUnreadCount(session.userId)
        val pName = partner?.get(Users.fullName) ?: ""
        val pInitials = pName.split(" ").filter { it.isNotEmpty() }.map { it.first().uppercase() }.joinToString("")
        val template = if (session.role == "professional") "professional/messages" else "subscriber/messages"
        call.respond(ThymeleafContent(template, model(
            "session" to session, "partners" to partners, "conversation" to conversation,
            "activePartnerId" to partnerId, "activePartnerName" to pName, "activePartnerInitials" to pInitials,
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
