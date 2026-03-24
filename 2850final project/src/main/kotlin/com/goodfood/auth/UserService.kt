package com.goodfood.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object UserService {

    fun authenticate(email: String, password: String): Map<String, Any>? = transaction {
        val row = Users.selectAll().where { Users.email eq email }.singleOrNull() ?: return@transaction null
        val result = BCrypt.verifyer().verify(password.toCharArray(), row[Users.passwordHash])
        if (!result.verified) return@transaction null
        mapOf("id" to row[Users.id], "fullName" to row[Users.fullName], "email" to row[Users.email], "role" to row[Users.role])
    }

    fun register(fullName: String, email: String, password: String, role: String): Int? = transaction {
        if (Users.selectAll().where { Users.email eq email }.count() > 0) return@transaction null
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        Users.insert {
            it[Users.fullName] = fullName; it[Users.email] = email; it[Users.passwordHash] = hash
            it[Users.role] = role; it[Users.createdAt] = LocalDateTime.now()
        } get Users.id
    }

    fun getById(id: Int): ResultRow? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()
    }

    fun updateProfile(id: Int, fullName: String, email: String, password: String?) = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.fullName] = fullName; it[Users.email] = email; it[Users.updatedAt] = LocalDateTime.now()
            if (!password.isNullOrBlank()) it[Users.passwordHash] = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        }
    }
}
