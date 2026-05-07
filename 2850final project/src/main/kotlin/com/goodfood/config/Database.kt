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
    SeedData.backfillImageUrls()
    SeedData.backfillExtraRecipes()
    SeedData.backfillExtraFoods()
    SeedData.ensureAliceHasTodayEntries() //fill test account with data for testing and demo
}
