package com.goodfood.recipes

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [RecipeService].
 *
 * Job stories covered (see Wiki: Job-Stories — Subscriber Features):
 *  - "Recipe Search" — when I want to cook, I want to search by title /
 *    difficulty so I can find a quick suitable meal.
 *  - "Favourite Recipes" — when I like a recipe, I want to favourite it
 *    so it shows up on my profile next time.
 *
 * Acceptance criteria exercised:
 *  - AC-RECIPE-1  Recipe search by title returns matching recipes.            [searchRecipesFindsRecipeByTitle]
 *  - AC-RECIPE-2  Toggling favourite adds the recipe; toggling again removes. [toggleFavouriteAddsAndRemovesFavourite]
 */
class RecipeServiceTest {

    @Test
    fun searchRecipesFindsRecipeByTitle() {
        TestDatabase.setup()

        val userId = TestDatabase.insertUser()
        TestDatabase.insertRecipe(userId, title = "Healthy Pasta")

        val results = RecipeService.searchRecipes("pasta", "all")

        assertTrue(results.isNotEmpty())
        assertEquals("Healthy Pasta", results.first()["title"])
    }

    @Test
    fun toggleFavouriteAddsAndRemovesFavourite() {
        TestDatabase.setup()

        val userId = TestDatabase.insertUser()
        val recipeId = TestDatabase.insertRecipe(userId)

        assertFalse(RecipeService.isFavourite(userId, recipeId))

        val added = RecipeService.toggleFavourite(userId, recipeId)
        assertTrue(added)
        assertTrue(RecipeService.isFavourite(userId, recipeId))

        val removed = RecipeService.toggleFavourite(userId, recipeId)
        assertFalse(removed)
        assertFalse(RecipeService.isFavourite(userId, recipeId))
    }
}