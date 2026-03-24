package com.goodfood.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val fullName = varchar("full_name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 50)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
