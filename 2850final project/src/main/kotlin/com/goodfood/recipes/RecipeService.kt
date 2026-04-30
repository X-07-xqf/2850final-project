package com.goodfood.recipes

import com.goodfood.auth.Users
import com.goodfood.diary.FoodItems
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

object RecipeService {

    /**
     * Escape SQL `LIKE` wildcards (`%`, `_`) and the escape character itself so a
     * user-supplied search term is matched as a literal substring. Closes issue #19
     * for the recipe-title search call site below.
     */
    private fun escapeLikePattern(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    fun searchRecipes(query: String?, difficulty: String?): List<Map<String, Any?>> = transaction {
        val baseQuery = Recipes.selectAll()
        val filtered = baseQuery.let { q ->
            if (!query.isNullOrBlank()) {
                val safe = escapeLikePattern(query.lowercase())
                q.andWhere { Recipes.title.lowerCase() like "%$safe%" }
            }
            if (!difficulty.isNullOrBlank() && difficulty != "all") q.andWhere { Recipes.difficulty eq difficulty }
            q
        }
        filtered.map { row ->
            val rid = row[Recipes.id]
            val ratings = RecipeRatings.selectAll().where { RecipeRatings.recipeId eq rid }.toList()
            val avgRating = if (ratings.isNotEmpty()) ratings.map { it[RecipeRatings.rating] }.average() else 0.0
            mapOf("id" to rid, "title" to row[Recipes.title], "description" to row[Recipes.description],
                "prepTime" to row[Recipes.prepTimeMinutes], "cookTime" to row[Recipes.cookTimeMinutes],
                "totalTime" to (row[Recipes.prepTimeMinutes] + row[Recipes.cookTimeMinutes]),
                "servings" to row[Recipes.servings], "difficulty" to row[Recipes.difficulty],
                "avgRating" to BigDecimal(avgRating).setScale(1, RoundingMode.HALF_UP), "reviewCount" to ratings.size)
        }
    }

    fun getRecipeDetail(recipeId: Int): Map<String, Any?>? = transaction {
        val recipe = Recipes.selectAll().where { Recipes.id eq recipeId }.singleOrNull() ?: return@transaction null
        val ingredients = RecipeIngredients.selectAll().where { RecipeIngredients.recipeId eq recipeId }.map { row ->
            mapOf("name" to row[RecipeIngredients.ingredientName], "quantity" to row[RecipeIngredients.quantity],
                "unit" to row[RecipeIngredients.unit], "foodItemId" to row[RecipeIngredients.foodItemId])
        }
        val steps = RecipeSteps.selectAll().where { RecipeSteps.recipeId eq recipeId }
            .orderBy(RecipeSteps.stepNumber).map { it[RecipeSteps.instruction] }
        val ratings = RecipeRatings.selectAll().where { RecipeRatings.recipeId eq recipeId }.map { row ->
            val user = Users.selectAll().where { Users.id eq row[RecipeRatings.userId] }.single()
            mapOf("userName" to user[Users.fullName], "rating" to row[RecipeRatings.rating],
                "comment" to row[RecipeRatings.comment], "date" to row[RecipeRatings.createdAt].toLocalDate())
        }
        val avgRating = if (ratings.isNotEmpty()) ratings.map { (it["rating"] as Int) }.average() else 0.0

        var totalCal = BigDecimal.ZERO; var totalProt = BigDecimal.ZERO; var totalCarb = BigDecimal.ZERO; var totalFat = BigDecimal.ZERO
        for (ing in ingredients) {
            val fid = ing["foodItemId"] as? Int ?: continue
            val food = FoodItems.selectAll().where { FoodItems.id eq fid }.singleOrNull() ?: continue
            val qty = try { BigDecimal(ing["quantity"] as String) } catch (_: Exception) { continue }
            val factor = qty.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            totalCal += food[FoodItems.caloriesPer100g] * factor; totalProt += food[FoodItems.proteinPer100g] * factor
            totalCarb += food[FoodItems.carbsPer100g] * factor; totalFat += food[FoodItems.fatPer100g] * factor
        }
        val servings = recipe[Recipes.servings].toBigDecimal()
        mapOf("id" to recipe[Recipes.id], "title" to recipe[Recipes.title], "description" to recipe[Recipes.description],
            "prepTime" to recipe[Recipes.prepTimeMinutes], "cookTime" to recipe[Recipes.cookTimeMinutes],
            "totalTime" to (recipe[Recipes.prepTimeMinutes] + recipe[Recipes.cookTimeMinutes]),
            "servings" to recipe[Recipes.servings], "difficulty" to recipe[Recipes.difficulty],
            "ingredients" to ingredients, "steps" to steps, "ratings" to ratings,
            "avgRating" to BigDecimal(avgRating).setScale(1, RoundingMode.HALF_UP), "reviewCount" to ratings.size,
            "calPerServing" to totalCal.divide(servings, 0, RoundingMode.HALF_UP),
            "protPerServing" to totalProt.divide(servings, 1, RoundingMode.HALF_UP),
            "carbPerServing" to totalCarb.divide(servings, 1, RoundingMode.HALF_UP),
            "fatPerServing" to totalFat.divide(servings, 1, RoundingMode.HALF_UP))
    }

    fun isFavourite(userId: Int, recipeId: Int): Boolean = transaction {
        RecipeFavourites.selectAll().where { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }.count() > 0
    }

    fun toggleFavourite(userId: Int, recipeId: Int): Boolean = transaction {
        val existing = RecipeFavourites.selectAll().where { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }.singleOrNull()
        if (existing != null) { RecipeFavourites.deleteWhere { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }; false }
        else { RecipeFavourites.insert { it[RecipeFavourites.userId] = userId; it[RecipeFavourites.recipeId] = recipeId; it[RecipeFavourites.createdAt] = LocalDateTime.now() }; true }
    }

    fun addRating(userId: Int, recipeId: Int, rating: Int, comment: String?) = transaction {
        val existing = RecipeRatings.selectAll().where { (RecipeRatings.userId eq userId) and (RecipeRatings.recipeId eq recipeId) }.singleOrNull()
        if (existing != null) {
            RecipeRatings.update({ (RecipeRatings.userId eq userId) and (RecipeRatings.recipeId eq recipeId) }) {
                it[RecipeRatings.rating] = rating; it[RecipeRatings.comment] = comment; it[RecipeRatings.updatedAt] = LocalDateTime.now()
            }
        } else {
            RecipeRatings.insert { it[RecipeRatings.userId] = userId; it[RecipeRatings.recipeId] = recipeId; it[RecipeRatings.rating] = rating; it[RecipeRatings.comment] = comment; it[RecipeRatings.createdAt] = LocalDateTime.now() }
        }
    }

    fun getUserFavourites(userId: Int): List<Map<String, Any?>> = transaction {
        (RecipeFavourites innerJoin Recipes).selectAll().where { RecipeFavourites.userId eq userId }.map { row ->
            val rid = row[Recipes.id]; val ratings = RecipeRatings.selectAll().where { RecipeRatings.recipeId eq rid }.toList()
            val avg = if (ratings.isNotEmpty()) ratings.map { it[RecipeRatings.rating] }.average() else 0.0
            mapOf("id" to rid, "title" to row[Recipes.title], "difficulty" to row[Recipes.difficulty], "avgRating" to BigDecimal(avg).setScale(1, RoundingMode.HALF_UP))
        }
    }
}
