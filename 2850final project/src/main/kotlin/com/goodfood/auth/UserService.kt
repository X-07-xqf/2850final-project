package com.goodfood.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Authentication and account-management for the Sage app.
 *
 * Owns the password lifecycle (BCrypt hash on write, verify on login) and the
 * uniqueness rule on `email`. Routes interact with this object only — they
 * never touch the [Users] table directly.
 */
object UserService {

    /**
     * Verify the supplied [password] against the stored BCrypt hash for [email].
     *
     * @return a map shaped like the session payload (`id`, `fullName`, `email`, `role`)
     *  on a successful match, or `null` when the email is unknown OR the password is wrong.
     *  The same `null` is returned in both failure cases on purpose — the caller cannot
     *  distinguish "user does not exist" from "wrong password" and so cannot enumerate
     *  registered emails.
     */
    fun authenticate(email: String, password: String): Map<String, Any>? = transaction {
        val row = Users.selectAll().where { Users.email eq email }.singleOrNull() ?: return@transaction null
        val result = BCrypt.verifyer().verify(password.toCharArray(), row[Users.passwordHash])
        if (!result.verified) return@transaction null
        mapOf("id" to row[Users.id], "fullName" to row[Users.fullName], "email" to row[Users.email], "role" to row[Users.role])
    }

    /**
     * Create a new user row.
     *
     * @param fullName display name shown in the sidebar and on professional dashboards.
     * @param email unique login identifier; must not already exist.
     * @param password plaintext, hashed with BCrypt cost-12 before persisting.
     * @param role one of `"subscriber"` or `"professional"` — controls which sidebar
     *  and which routes the user can reach.
     * @return the new user's auto-generated `id`, or `null` when the email already exists.
     */
    /**
     * v0.6.36 — password-complexity gate for new registrations. Returns the
     * specific reason a password is rejected, or `null` when it passes. Order
     * matters: surface the most-actionable failure first (length before
     * character classes).
     *
     * Existing accounts whose hashes pre-date this rule are unaffected —
     * [authenticate] only verifies the BCrypt hash, not strength, so old
     * passwords still log in.
     */
    fun validatePassword(password: String): String? = when {
        password.length < 6 -> "Password must be at least 6 characters."
        !password.any { it.isUpperCase() } -> "Password must include an uppercase letter (A–Z)."
        !password.any { it.isLowerCase() } -> "Password must include a lowercase letter (a–z)."
        !password.any { it.isDigit() } -> "Password must include a number (0–9)."
        else -> null
    }

    fun register(fullName: String, email: String, password: String, role: String): Int? = transaction {
        if (Users.selectAll().where { Users.email eq email }.count() > 0) return@transaction null
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        Users.insert {
            it[Users.fullName] = fullName; it[Users.email] = email; it[Users.passwordHash] = hash
            it[Users.role] = role; it[Users.createdAt] = LocalDateTime.now()
        } get Users.id
    }

    /**
     * Fetch a user row by primary key. Returns `null` when no row exists for [id].
     * Used by the profile and professional-dashboard rendering paths.
     */
    fun getById(id: Int): ResultRow? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()
    }

    /**
     * Update an existing user's display name, email, and (optionally) password.
     *
     * @param password set to a non-blank string to rotate the BCrypt hash; pass `null`
     *  or blank to leave the existing hash intact (the typical "I just want to update
     *  my name" path).
     */
    fun updateProfile(id: Int, fullName: String, email: String, password: String?) = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.fullName] = fullName; it[Users.email] = email; it[Users.updatedAt] = LocalDateTime.now()
            if (!password.isNullOrBlank()) it[Users.passwordHash] = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        }
    }
}
