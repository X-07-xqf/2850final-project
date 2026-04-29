package com.goodfood.recipes

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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