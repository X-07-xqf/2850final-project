package com.goodfood.messages

import com.goodfood.auth.Users
import com.goodfood.util.fmtChatTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// direct messaging between users and their assigned professionals.
// All messages are in a single advice_messages table. A "conversation" is
// all the rows where the two users appear as sender and receiver in
// either direction, grouped by the other person's ID. 
// checks is_read flag, which becomes 'true' as soon as user opens conversation

object MessageService {

    /**
     * List of every distinct conversation partner of [userId], ordered by the
     * most recent message exchanged. 
     */
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


    // gets conversation between userID and partnerID
    // returns 1 map per message, with id, senderId, message, sentAt, isMine (checks if the row was sent by userId)
    fun getConversation(userId: Int, partnerId: Int): List<Map<String, Any>> = transaction {
        AdviceMessages.update({
            (AdviceMessages.senderId eq partnerId) and (AdviceMessages.receiverId eq userId) and (AdviceMessages.isRead eq false)
        }) { it[isRead] = true }
        AdviceMessages.selectAll().where {
            ((AdviceMessages.senderId eq userId) and (AdviceMessages.receiverId eq partnerId)) or
            ((AdviceMessages.senderId eq partnerId) and (AdviceMessages.receiverId eq userId))
        }.orderBy(AdviceMessages.sentAt).map { row ->
            val ts = row[AdviceMessages.sentAt]
            mapOf("id" to row[AdviceMessages.id], "senderId" to row[AdviceMessages.senderId],
                "message" to row[AdviceMessages.message],
                "sentAt" to ts.fmtChatTime(),
                "sentDate" to ts.toLocalDate(),
                "sentTime" to ts.format(DateTimeFormatter.ofPattern("HH:mm")),
                "isMine" to (row[AdviceMessages.senderId] == userId))
        }
    }

    // inserts a new message row
    // authorisation for whether the sender and reciever have an active relationship
    // is done in professionalRoutes hasActiveRelationship()
    fun sendMessage(senderId: Int, receiverId: Int, message: String) = transaction {
        AdviceMessages.insert { it[AdviceMessages.senderId] = senderId; it[AdviceMessages.receiverId] = receiverId
            it[AdviceMessages.message] = message; it[isRead] = false; it[sentAt] = LocalDateTime.now() }
    }


    // gets number of messages to userId that are flagged unread
    // used to render red badge next to Messages
    fun getUnreadCount(userId: Int): Long = transaction {
        AdviceMessages.selectAll().where { (AdviceMessages.receiverId eq userId) and (AdviceMessages.isRead eq false) }.count()
    }

    /**
     * Polling endpoint backbone (v0.6.37). Returns every message in the
     * [userId]↔[partnerId] thread whose `id` is greater than [lastId], in
     * chronological order. Caller (JS) tracks the highest id it has rendered
     * and asks for newer ones every few seconds.
     *
     * Marks any newly-arrived message FROM the partner as read, same as
     * [getConversation] does on full page load.
     */
    
    // Returns all messages in a conversation that arrived after lastId, oldest first.
    // Frontend keeps track of the highest message ID already shown
    // and passes it in every few seconds to pick up anything new.
    // Any unread messages from partnerId are marked as read
    
    fun getConversationSince(userId: Int, partnerId: Int, lastId: Int): List<Map<String, Any>> = transaction {
        AdviceMessages.update({
            (AdviceMessages.senderId eq partnerId) and (AdviceMessages.receiverId eq userId) and (AdviceMessages.isRead eq false)
        }) { it[isRead] = true }
        AdviceMessages.selectAll().where {
            (
                ((AdviceMessages.senderId eq userId) and (AdviceMessages.receiverId eq partnerId)) or
                ((AdviceMessages.senderId eq partnerId) and (AdviceMessages.receiverId eq userId))
            ) and (AdviceMessages.id greater lastId)
        }.orderBy(AdviceMessages.sentAt).map { row ->
            val ts = row[AdviceMessages.sentAt]
            mapOf(
                "id" to row[AdviceMessages.id],
                "senderId" to row[AdviceMessages.senderId],
                "message" to row[AdviceMessages.message],
                "sentAt" to ts.fmtChatTime(),
                "sentTime" to ts.format(DateTimeFormatter.ofPattern("HH:mm")),
                "isMine" to (row[AdviceMessages.senderId] == userId)
            )
        }
    }


    // directory of every user of the opposite role that userId hasn't messaged yet
    // 'new conversation' list in Messages
    // does not return the actual current user
    fun getEligibleNewPartners(userId: Int, currentUserRole: String): List<Map<String, Any>> = transaction {
        val targetRole = if (currentUserRole == "professional") "subscriber" else "professional"
        val existing: Set<Int> = AdviceMessages.selectAll().where {
            (AdviceMessages.senderId eq userId) or (AdviceMessages.receiverId eq userId)
        }.map { row ->
            if (row[AdviceMessages.senderId] == userId) row[AdviceMessages.receiverId] else row[AdviceMessages.senderId]
        }.toSet()
        Users.selectAll().where {
            (Users.role eq targetRole) and (Users.id neq userId)
        }.orderBy(Users.fullName)
        .map { user ->
            val name = user[Users.fullName]
            val initials = name.split(" ").filter { it.isNotEmpty() }
                .map { it.first().uppercase() }.joinToString("")
            mapOf(
                "partnerId" to user[Users.id],
                "partnerName" to name,
                "partnerInitials" to initials,
                "partnerRole" to user[Users.role]
            )
        }
        .filter { (it["partnerId"] as Int) !in existing }
    }
}
