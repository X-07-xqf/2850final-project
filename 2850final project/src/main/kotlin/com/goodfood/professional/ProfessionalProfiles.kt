package com.goodfood.professional

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ProfessionalProfiles : Table("professional_profiles") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id).uniqueIndex()
    val specialty = varchar("specialty", 255)
    val qualification = varchar("qualification", 255)
    val bio = text("bio").nullable()
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
