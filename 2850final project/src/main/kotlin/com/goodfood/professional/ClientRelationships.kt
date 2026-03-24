package com.goodfood.professional

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ClientRelationships : Table("client_relationships") {
    val id = integer("id").autoIncrement()
    val professionalId = integer("professional_id").references(Users.id)
    val subscriberId = integer("subscriber_id").references(Users.id)
    val status = varchar("status", 50)
    val startedAt = datetime("started_at")
    val endedAt = datetime("ended_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
