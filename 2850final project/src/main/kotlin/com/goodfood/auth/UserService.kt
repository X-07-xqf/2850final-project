package com.goodfood.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Auth and account management — login, registration policy, profile updates.
 */
object UserService {
    /**
     * Look up [email], verify [password] against the bcrypt hash, and return
     * the public user fields. Returns null when the email is unknown or the
     * password doesn't match.
     */
    fun authenticate(email: String, password: String): Map<String, Any>? = transaction {
        val row = Users.selectAll().where { Users.email eq email }.singleOrNull() ?: return@transaction null
        val result = BCrypt.verifyer().verify(password.toCharArray(), row[Users.passwordHash])
        if (!result.verified) return@transaction null
        mapOf("id" to row[Users.id], "fullName" to row[Users.fullName], "email" to row[Users.email], "role" to row[Users.role])
    }

    /**
     * Registration password policy: min 6 chars, at least one upper, one
     * lower, one digit. Returns null when valid; otherwise a user-facing
     * reason.
     */
    fun validatePassword(password: String): String? = when {
        password.length < 6 -> "Password must be at least 6 characters."
        !password.any { it.isUpperCase() } -> "Password must include an uppercase letter (A–Z)."
        !password.any { it.isLowerCase() } -> "Password must include a lowercase letter (a–z)."
        !password.any { it.isDigit() } -> "Password must include a number (0–9)."
        else -> null
    }

    /**
     * Insert a new user with a freshly bcrypt-hashed password. Returns the
     * new id, or null if the email is already taken.
     */
    fun register(fullName: String, email: String, password: String, role: String): Int? = transaction {
        if (Users.selectAll().where { Users.email eq email }.count() > 0) return@transaction null
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        Users.insert {
            it[Users.fullName] = fullName; it[Users.email] = email; it[Users.passwordHash] = hash
            it[Users.role] = role; it[Users.createdAt] = LocalDateTime.now()
        } get Users.id
    }

    /**
     * Fetch a user row by primary key. Returns null when no such user.
     */
    fun getById(id: Int): ResultRow? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()
    }

    /**
     * Update profile fields. If [password] is non-blank, re-hash and write it
     * too; otherwise the password column is left untouched.
     */
    fun updateProfile(id: Int, fullName: String, email: String, password: String?) = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.fullName] = fullName; it[Users.email] = email; it[Users.updatedAt] = LocalDateTime.now()
            if (!password.isNullOrBlank()) it[Users.passwordHash] = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        }
    }
}
