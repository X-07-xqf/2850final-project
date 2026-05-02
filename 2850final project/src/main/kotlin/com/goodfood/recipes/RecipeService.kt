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

    /**
     * Title-substring + difficulty filter. Both filters are optional.
     *
     * @param query case-insensitive substring of the recipe title; wildcard-escaped.
     * @param difficulty `"easy"`, `"medium"`, `"hard"`, or `"all"` / `null` to skip.
     * @return one map per recipe with id, title, description, prep/cook/total time,
     *  servings, difficulty, average rating, review count, cover image / fallback
     *  emoji + tone, and per-serving calories / protein.
     */
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
        filtered.map { summariseRow(it) }
    }

    /**
     * Top [limit] recipes by average rating (ties broken by review count, then
     * by recipe id for stability). Powers the `Featured this week` strip on
     * `/recipes`. Recipes with zero ratings sort last.
     */
    fun getFeatured(limit: Int = 3): List<Map<String, Any?>> = transaction {
        Recipes.selectAll().map { summariseRow(it) }
            .sortedWith(compareByDescending<Map<String, Any?>> { (it["avgRating"] as BigDecimal) }
                .thenByDescending { it["reviewCount"] as Int }
                .thenBy { it["id"] as Int })
            .take(limit)
    }

    /**
     * Full recipe page payload — the recipe row, every ingredient (joined to its
     * `food_items` row when one exists, for nutrition computation), every step
     * ordered by `step_number`, every rating with the rater's name, and the
     * macros-per-serving derived by summing each ingredient's nutrition and
     * dividing by the recipe's servings count.
     *
     * @return `null` when [recipeId] does not exist.
     */
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

    /** True when [userId] has favourited [recipeId]. Used to render the heart icon state. */
    fun isFavourite(userId: Int, recipeId: Int): Boolean = transaction {
        RecipeFavourites.selectAll().where { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }.count() > 0
    }

    /**
     * Flip the favourite state for ([userId], [recipeId]).
     *
     * @return `true` when the recipe is now favourited (was previously not),
     *  `false` when it was previously favourited and has just been removed.
     */
    fun toggleFavourite(userId: Int, recipeId: Int): Boolean = transaction {
        val existing = RecipeFavourites.selectAll().where { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }.singleOrNull()
        if (existing != null) { RecipeFavourites.deleteWhere { (RecipeFavourites.userId eq userId) and (RecipeFavourites.recipeId eq recipeId) }; false }
        else { RecipeFavourites.insert { it[RecipeFavourites.userId] = userId; it[RecipeFavourites.recipeId] = recipeId; it[RecipeFavourites.createdAt] = LocalDateTime.now() }; true }
    }

    /**
     * Add or update a rating row for ([userId], [recipeId]). One user can rate
     * a recipe at most once — re-rating overwrites the previous row's `rating`
     * and `comment` rather than appending a duplicate.
     *
     * @param rating expected to already be coerced into 1..5 by the route handler.
     */
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

    /**
     * Every recipe [userId] has favourited, decorated with the recipe's average
     * rating. Powers the "Favourites" section on the user's profile.
     */
    fun getUserFavourites(userId: Int): List<Map<String, Any?>> = transaction {
        (RecipeFavourites innerJoin Recipes).selectAll().where { RecipeFavourites.userId eq userId }.map { row ->
            val rid = row[Recipes.id]; val ratings = RecipeRatings.selectAll().where { RecipeRatings.recipeId eq rid }.toList()
            val avg = if (ratings.isNotEmpty()) ratings.map { it[RecipeRatings.rating] }.average() else 0.0
            mapOf("id" to rid, "title" to row[Recipes.title], "difficulty" to row[Recipes.difficulty], "avgRating" to BigDecimal(avg).setScale(1, RoundingMode.HALF_UP))
        }
    }
}
