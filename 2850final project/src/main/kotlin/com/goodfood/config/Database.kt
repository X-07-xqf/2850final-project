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

fun Application.configureDatabase() {
    val config = environment.config
    val driver = config.property("database.driver").getString()
    val url = config.property("database.url").getString()
    val user = config.property("database.user").getString()
    val password = config.property("database.password").getString()

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
}
