package com.goodfood.messages

import com.goodfood.auth.Users
import com.goodfood.util.fmtChatTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 1-on-1 messaging between subscribers and their professionals.
 *
 * Persistence is a single `advice_messages` table; conversations are derived
 * by querying every row where the user is sender *or* receiver and grouping
 * by the partner's user id. The "unread badge" feature relies on the
 * `is_read` boolean flipping the moment a conversation is opened.
 */
object MessageService {

    /**
     * List of every distinct conversation partner of [userId], ordered by the
     * most recent message exchanged. Each entry carries:
     *  - `partnerId` / `partnerName` / `partnerInitials` / `partnerRole`
     *  - `lastMessage` — the partner's most recent message body, truncated to 50 chars
     *  - `unreadCount` — number of messages from this partner not yet read
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

    /**
     * Full chronological transcript of the conversation between [userId] and
     * [partnerId]. As a side-effect, marks every message **from** [partnerId]
     * **to** [userId] as read — the unread badge in the sidebar will tick
     * down on the next render.
     *
     * @return one map per message with `id`, `senderId`, `message`, `sentAt`,
     *  and `isMine` (true when the row was sent by [userId]).
     */
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

    /**
     * Insert a new message row. Authorisation (e.g. "may this professional
     * actually message this subscriber?") is enforced upstream by the route —
     * see [com.goodfood.professional.professionalRoutes] / `hasActiveRelationship`.
     */
    fun sendMessage(senderId: Int, receiverId: Int, message: String) = transaction {
        AdviceMessages.insert { it[AdviceMessages.senderId] = senderId; it[AdviceMessages.receiverId] = receiverId
            it[AdviceMessages.message] = message; it[isRead] = false; it[sentAt] = LocalDateTime.now() }
    }

    /**
     * Number of messages addressed to [userId] that are still flagged unread.
     * Used by every page's sidebar to render the red badge next to "Messages".
     */
    fun getUnreadCount(userId: Int): Long = transaction {
        AdviceMessages.selectAll().where { (AdviceMessages.receiverId eq userId) and (AdviceMessages.isRead eq false) }.count()
    }

    /**
     * "Directory" — every user of the opposite role that [userId] has *not*
     * yet exchanged messages with. Powers the new-conversation list at the
     * bottom of `/messages`. Subscribers see professionals, professionals see
     * subscribers; never returns the current user themselves.
     */
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
