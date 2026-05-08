@file:Suppress("MaxLineLength")

package com.goodfood.recipes

import com.goodfood.auth.Users
import com.goodfood.diary.FoodItems
import com.goodfood.util.recipeCoverEmoji
import com.goodfood.util.recipeCoverTone
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * All recipe read paths plus the social actions on top of them (favourite, rate).
 *
 * Recipe data is stored across five tables (`recipes`, `recipe_ingredients`,
 * `recipe_steps`, `recipe_ratings`, `recipe_favourites`); this service hides
 * that fan-out behind ergonomic call sites for routes and templates.
 */
object RecipeService {

    /**
     * Escape SQL `LIKE` wildcards (`%`, `_`) and the escape character itself so a
     * user-supplied search term is matched as a literal substring. Closes issue #19
     * for the recipe-title search call site below.
     */
    private fun escapeLikePattern(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /**
     * Compute the per-serving macros for a recipe by summing each ingredient's
     * scaled nutrition (factor = `quantity / 100g`) and dividing by `servings`.
     * Returns a four-element pair-of-pairs: cal, protein, carbs, fat. Used by
     * both [searchRecipes] (card meta line) and [getRecipeDetail].
     */
    private fun perServingMacros(recipeId: Int, servings: Int): Map<String, BigDecimal> {
        val ingredients = RecipeIngredients.selectAll().where { RecipeIngredients.recipeId eq recipeId }.toList()
        var cal = BigDecimal.ZERO; var prot = BigDecimal.ZERO; var carb = BigDecimal.ZERO; var fat = BigDecimal.ZERO
        for (row in ingredients) {
            val fid = row[RecipeIngredients.foodItemId] ?: continue
            val food = FoodItems.selectAll().where { FoodItems.id eq fid }.singleOrNull() ?: continue
            val qty = try { BigDecimal(row[RecipeIngredients.quantity]) } catch (_: Exception) { continue }
            val factor = qty.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            cal += food[FoodItems.caloriesPer100g] * factor; prot += food[FoodItems.proteinPer100g] * factor
            carb += food[FoodItems.carbsPer100g] * factor; fat += food[FoodItems.fatPer100g] * factor
        }
        val s = servings.toBigDecimal()
        return mapOf(
            "cal" to cal.divide(s, 0, RoundingMode.HALF_UP),
            "prot" to prot.divide(s, 1, RoundingMode.HALF_UP),
            "carb" to carb.divide(s, 1, RoundingMode.HALF_UP),
            "fat" to fat.divide(s, 1, RoundingMode.HALF_UP)
        )
    }

    /**
     * Build the card-shaped summary map for a single recipe row — covers the
     * `/recipes` listing and the `Featured this week` strip. Inside `transaction`.
     */
    private fun summariseRow(row: org.jetbrains.exposed.sql.ResultRow): Map<String, Any?> {
        val rid = row[Recipes.id]
        val title = row[Recipes.title]
        val servings = row[Recipes.servings]
        val ratings = RecipeRatings.selectAll().where { RecipeRatings.recipeId eq rid }.toList()
        val avgRating = if (ratings.isNotEmpty()) ratings.map { it[RecipeRatings.rating] }.average() else 0.0
        val macros = perServingMacros(rid, servings)
        return mapOf("id" to rid, "title" to title, "description" to row[Recipes.description],
            "prepTime" to row[Recipes.prepTimeMinutes], "cookTime" to row[Recipes.cookTimeMinutes],
            "totalTime" to (row[Recipes.prepTimeMinutes] + row[Recipes.cookTimeMinutes]),
            "servings" to servings, "difficulty" to row[Recipes.difficulty],
            "avgRating" to BigDecimal(avgRating).setScale(1, RoundingMode.HALF_UP), "reviewCount" to ratings.size,
            "imageUrl" to row[Recipes.imageUrl],
            "coverEmoji" to recipeCoverEmoji(title), "coverTone" to recipeCoverTone(title),
            "calPerServing" to macros["cal"], "protPerServing" to macros["prot"])
    }


    fun searchRecipes(
        query: String?,
        difficulty: String?,
        calories: String? = null,
        protein: String? = null,
        time: String? = null
    ): List<Map<String, Any?>> = transaction {
        val baseQuery = Recipes.selectAll()
        val filtered = baseQuery.let { q ->
            if (!query.isNullOrBlank()) {
                val safe = escapeLikePattern(query.lowercase())
                q.andWhere { Recipes.title.lowerCase() like "%$safe%" }
            }
            if (!difficulty.isNullOrBlank() && difficulty != "all") q.andWhere { Recipes.difficulty eq difficulty }
            q
        }
        // macros and totalTime are calculated in
        // summariseRow and not stored as columns, so the calories/protein/time
        // buckets are evaluated in Kotlin after the row pass.
        filtered.map { summariseRow(it) }
            .filter { row -> matchesCalorieBucket(row, calories) }
            .filter { row -> matchesProteinBucket(row, protein) }
            .filter { row -> matchesTimeBucket(row, time) }
    }

   //calorie totals per recipe
    private fun matchesCalorieBucket(row: Map<String, Any?>, bucket: String?): Boolean {
        if (bucket.isNullOrBlank() || bucket == "all") return true
        val cal = (row["calPerServing"] as? BigDecimal)?.toInt() ?: return true
        return when (bucket) {
            "light"    -> cal <= 400
            "standard" -> cal in 401..650
            "hearty"   -> cal > 650
            else       -> true
        }
    }

    // protein amounts per recipe
    private fun matchesProteinBucket(row: Map<String, Any?>, bucket: String?): Boolean {
        if (bucket.isNullOrBlank() || bucket == "all") return true
        val prot = (row["protPerServing"] as? BigDecimal)?.toDouble() ?: return true
        return when (bucket) {
            "low"      -> prot < 15.0
            "moderate" -> prot in 15.0..25.0
            "high"     -> prot > 25.0
            else       -> true
        }
    }


    // 20, 21-45, >45 min prep+cook time
    private fun matchesTimeBucket(row: Map<String, Any?>, bucket: String?): Boolean {
        if (bucket.isNullOrBlank() || bucket == "all") return true
        val total = (row["totalTime"] as? Int) ?: return true
        return when (bucket) {
            "quick"    -> total <= 20
            "standard" -> total in 21..45
            "slow"     -> total > 45
            else       -> true
        }
    }

    
    // featured recipes - sorted by average rating
    fun getFeatured(limit: Int = 3): List<Map<String, Any?>> = transaction {
        Recipes.selectAll().map { summariseRow(it) }
            .sortedWith(compareByDescending<Map<String, Any?>> { (it["avgRating"] as BigDecimal) }
                .thenByDescending { it["reviewCount"] as Int }
                .thenBy { it["id"] as Int })
            .take(limit)
    }

    
    fun getRecipeDetail(recipeId: Int): Map<String, Any?>? = transaction {
        val recipe = Recipes.selectAll().where { Recipes.id eq recipeId }.singleOrNull() ?: return@transaction null
        val authorRow = Users.selectAll().where { Users.id eq recipe[Recipes.createdBy] }.singleOrNull()
        val authorId = recipe[Recipes.createdBy]
        val authorName = authorRow?.get(Users.fullName) ?: "the author"
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
            "authorId" to authorId, "authorName" to authorName,
            "ingredients" to ingredients, "steps" to steps, "ratings" to ratings,
            "avgRating" to BigDecimal(avgRating).setScale(1, RoundingMode.HALF_UP), "reviewCount" to ratings.size,
            "calPerServing" to totalCal.divide(servings, 0, RoundingMode.HALF_UP),
            "protPerServing" to totalProt.divide(servings, 1, RoundingMode.HALF_UP),
            "carbPerServing" to totalCarb.divide(servings, 1, RoundingMode.HALF_UP),
            "fatPerServing" to totalFat.divide(servings, 1, RoundingMode.HALF_UP))
    }

    // shows if user favourited recipe
    fun isFavourite(userId: Int, recipeId: Int): Boolean = transaction {
        RecipeFavourites.selectAll().where { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }.count() > 0
    }

    
    fun toggleFavourite(userId: Int, recipeId: Int): Boolean = transaction {
        val existing = RecipeFavourites.selectAll().where { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }.singleOrNull()
        if (existing != null) { RecipeFavourites.deleteWhere { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }; false }
        else { RecipeFavourites.insert { it[RecipeFavourites.userId] = userId; it[RecipeFavourites.recipeId] = recipeId; it[RecipeFavourites.createdAt] = LocalDateTime.now() }; true }
    }

    
    // add rating row, a user can only rate a recipe once. rerating overwrites previous rating
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
            val rid = row[Recipes.id]
            val title = row[Recipes.title]
            val ratings = RecipeRatings.selectAll().where { RecipeRatings.recipeId eq rid }.toList()
            val avg = if (ratings.isNotEmpty()) ratings.map { it[RecipeRatings.rating] }.average() else 0.0
            mapOf("id" to rid, "title" to title, "difficulty" to row[Recipes.difficulty],
                "avgRating" to BigDecimal(avg).setScale(1, RoundingMode.HALF_UP),
                "reviewCount" to ratings.size,
                "imageUrl" to row[Recipes.imageUrl],
                "coverEmoji" to recipeCoverEmoji(title), "coverTone" to recipeCoverTone(title))
        }
    }
}
