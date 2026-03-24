package com.goodfood.messages

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object AdviceMessages : Table("advice_messages") {
    val id = integer("id").autoIncrement()
    val senderId = integer("sender_id").references(Users.id)
    val receiverId = integer("receiver_id").references(Users.id)
    val message = text("message")
    val isRead = bool("is_read").default(false)
    val sentAt = datetime("sent_at")
    override val primaryKey = PrimaryKey(id)
}
