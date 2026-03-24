package com.goodfood.messages

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object MessageService {

    fun getConversationPartners(userId: Int): List<Map<String, Any>> = transaction {
        val partners = mutableMapOf<Int, MutableMap<String, Any>>()
        AdviceMessages.selectAll().where {
            (AdviceMessages.senderId eq userId) or (AdviceMessages.receiverId eq userId)
        }.orderBy(AdviceMessages.sentAt, SortOrder.DESC).forEach { row ->
            val partnerId = if (row[AdviceMessages.senderId] == userId) row[AdviceMessages.receiverId] else row[AdviceMessages.senderId]
            if (partnerId !in partners) {
                val partner = Users.selectAll().where { Users.id eq partnerId }.single()
                val unread = AdviceMessages.selectAll().where {
                    (AdviceMessages.senderId eq partnerId) and (AdviceMessages.receiverId eq userId) and (AdviceMessages.isRead eq false)
                }.count()
                val name = partner[Users.fullName]
                val initials = name.split(" ").filter { it.isNotEmpty() }.map { it.first().uppercase() }.joinToString("")
                partners[partnerId] = mutableMapOf("partnerId" to partnerId, "partnerName" to name, "partnerInitials" to initials,
                    "partnerRole" to partner[Users.role], "lastMessage" to row[AdviceMessages.message].take(50), "unreadCount" to unread)
            }
        }
        partners.values.toList()
    }

    fun getConversation(userId: Int, partnerId: Int): List<Map<String, Any>> = transaction {
        AdviceMessages.update({
            (AdviceMessages.senderId eq partnerId) and (AdviceMessages.receiverId eq userId) and (AdviceMessages.isRead eq false)
        }) { it[isRead] = true }
        AdviceMessages.selectAll().where {
            ((AdviceMessages.senderId eq userId) and (AdviceMessages.receiverId eq partnerId)) or
            ((AdviceMessages.senderId eq partnerId) and (AdviceMessages.receiverId eq userId))
        }.orderBy(AdviceMessages.sentAt).map { row ->
            mapOf("id" to row[AdviceMessages.id], "senderId" to row[AdviceMessages.senderId],
                "message" to row[AdviceMessages.message], "sentAt" to row[AdviceMessages.sentAt], "isMine" to (row[AdviceMessages.senderId] == userId))
        }
    }

    fun sendMessage(senderId: Int, receiverId: Int, message: String) = transaction {
        AdviceMessages.insert { it[AdviceMessages.senderId] = senderId; it[AdviceMessages.receiverId] = receiverId
            it[AdviceMessages.message] = message; it[isRead] = false; it[sentAt] = LocalDateTime.now() }
    }

    fun getUnreadCount(userId: Int): Long = transaction {
        AdviceMessages.selectAll().where { (AdviceMessages.receiverId eq userId) and (AdviceMessages.isRead eq false) }.count()
    }
}
