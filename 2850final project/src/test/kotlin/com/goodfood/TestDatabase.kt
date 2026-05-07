package com.goodfood

import com.goodfood.auth.Users
import com.goodfood.diary.FoodDiaryEntries
import com.goodfood.diary.FoodItems
import com.goodfood.diary.NutritionalGoals
import com.goodfood.messages.AdviceMessages
import com.goodfood.professional.ClientRelationships
import com.goodfood.professional.ProfessionalProfiles
import com.goodfood.recipes.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

object TestDatabase {
    fun setup() {
        Database.connect(
            "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )

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
    }

    fun insertUser(
        name: String = "Test User",
        email: String = "test${UUID.randomUUID()}@example.com",
        role: String = "subscriber"
    ): Int = transaction {
        Users.insert {
            it[fullName] = name
            it[Users.email] = email
            it[passwordHash] = "not-used"
            it[Users.role] = role
            it[createdAt] = LocalDateTime.now()
        } get Users.id
    }

    fun insertFood(name: String = "Apple", calories: BigDecimal = BigDecimal("100.00")): Int = transaction {
        FoodItems.insert {
            it[FoodItems.name] = name
            it[category] = "Fruit"
            it[caloriesPer100g] = calories
            it[proteinPer100g] = BigDecimal("1.00")
            it[carbsPer100g] = BigDecimal("20.00")
            it[fatPer100g] = BigDecimal("0.50")
            it[fiberPer100g] = BigDecimal("2.00")
            it[sugarPer100g] = BigDecimal("10.00")
            it[createdAt] = LocalDateTime.now()
        } get FoodItems.id
    }

    fun insertRecipe(createdBy: Int, title: String = "Healthy Pasta", difficulty: String = "Easy"): Int = transaction {
        Recipes.insert {
            it[Recipes.createdBy] = createdBy
            it[Recipes.title] = title
            it[description] = "Simple healthy meal"
            it[prepTimeMinutes] = 10
            it[cookTimeMinutes] = 20
            it[servings] = 2
            it[Recipes.difficulty] = difficulty
            it[createdAt] = LocalDateTime.now()
        } get Recipes.id
    }
}