package com.goodfood.config

import com.goodfood.auth.Users
import com.goodfood.diary.*
import com.goodfood.goals.*
import com.goodfood.messages.AdviceMessages
import com.goodfood.professional.*
import com.goodfood.recipes.*
import com.goodfood.seed.SeedData
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

private data class DbSettings(val driver: String, val url: String, val user: String, val password: String)

/**
 * When `DATABASE_URL` is set (Render injects this when a PostgreSQL service is
 * linked) we connect to PostgreSQL so user accounts survive container rebuilds.
 * Otherwise we fall back to the H2 file from `application.conf` — convenient
 * for Codespaces / local dev where data living in `./data/` is good enough.
 *
 * Render hands us the libpq form `postgres://user:pass@host:port/db`, but JDBC
 * needs `jdbc:postgresql://host:port/db` with credentials passed separately,
 * so the URL is parsed before being handed to `Database.connect`.
 */
private fun Application.resolveDbSettings(): DbSettings {
    val raw = System.getenv("DATABASE_URL")?.trim().orEmpty()
    if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
        val uri = URI(raw)
        val (pgUser, pgPassword) = uri.userInfo.orEmpty().split(":", limit = 2).let {
            (it.getOrNull(0).orEmpty()) to (it.getOrNull(1).orEmpty())
        }
        val port = if (uri.port == -1) 5432 else uri.port
        val query = uri.rawQuery?.let { "?$it" } ?: ""
        val jdbcUrl = "jdbc:postgresql://${uri.host}:$port${uri.path}$query"
        return DbSettings("org.postgresql.Driver", jdbcUrl, pgUser, pgPassword)
    }
    val cfg = environment.config
    return DbSettings(
        cfg.property("database.driver").getString(),
        cfg.property("database.url").getString(),
        cfg.property("database.user").getString(),
        cfg.property("database.password").getString()
    )
}

fun Application.configureDatabase() {
    val (driver, url, user, password) = resolveDbSettings()

    Database.connect(url, driver, user, password)

    transaction {
        SchemaUtils.create(
            Users,
            ProfessionalProfiles,
            ClientRelationships,
            FoodItems,
            FoodDiaryEntries,
            NutritionalGoals,
            AdviceMessages,
            Recipes,
            RecipeIngredients,
            RecipeSteps,
            RecipeFavourites,
            RecipeRatings
        )
    }

    SeedData.insertIfEmpty()
    // Idempotent — fills in image_url on the seed recipes for any DB that
    // came up before that column was being populated. Safe on every boot.
    SeedData.backfillImageUrls()
    // Idempotent — inserts the v0.6.8 recipe pack (titles checked first), so
    // the live Render PostgreSQL gets six new recipes without a fresh seed.
    SeedData.backfillExtraRecipes()
}
