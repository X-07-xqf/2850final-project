package com.goodfood

import com.goodfood.diary.DiaryService
import com.goodfood.recipes.RecipeService
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Acceptance criteria:
 *  - Food search treats `%` as a literal character, not a SQL wildcard.    [searchFoodTreatsPercentAsLiteral]
 *  - Food search treats `_` as a literal character.                        [searchFoodTreatsUnderscoreAsLiteral]
 *  - Recipe search treats `%` as a literal character.                      [searchRecipesTreatsPercentAsLiteral]
 *  - A normal search query still finds the matching food.                  [searchFoodHappyPathStillWorks]
 *  - deleteEntry() refuses to delete another user's diary entry.           [deleteEntryDoesNotAllowCrossUserDeletion]
 */
class SecurityTest {

    @Test
    fun searchFoodTreatsPercentAsLiteral() {
        TestDatabase.setup()
        // Seed a few food items, none containing a literal '%' character.
        TestDatabase.insertFood(name = "Banana")
        TestDatabase.insertFood(name = "Apple")
        TestDatabase.insertFood(name = "Chicken Breast")
        TestDatabase.insertFood(name = "Olive Oil")
        TestDatabase.insertFood(name = "Salmon")

        // pre fix, '%' would dump every row.
        // post fix, '%' is escaped and matches only foods that contain a literal '%'.
        val results = DiaryService.searchFood("%")

        assertEquals(0, results.size, "raw % wildcard must not dump the entire food_items table")
    }

    @Test
    fun searchFoodTreatsUnderscoreAsLiteral() {
        TestDatabase.setup()
        TestDatabase.insertFood(name = "Banana")
        TestDatabase.insertFood(name = "Apple")
        TestDatabase.insertFood(name = "Carrot")

        // '_' matches any single character in raw SQL LIKE; escaped, matches only literal underscore.
        val results = DiaryService.searchFood("_")

        assertEquals(0, results.size, "raw _ wildcard must not match every single-character substring")
    }

    @Test
    fun searchRecipesTreatsPercentAsLiteral() {
        TestDatabase.setup()
        val userId = TestDatabase.insertUser()
        TestDatabase.insertRecipe(userId, title = "Pasta")
        TestDatabase.insertRecipe(userId, title = "Salad")
        TestDatabase.insertRecipe(userId, title = "Soup")

        val results = RecipeService.searchRecipes("%", "all")

        assertEquals(0, results.size, "raw % wildcard must not dump the entire recipes table")
    }

    @Test
    fun searchFoodHappyPathStillWorks() {
        TestDatabase.setup()
        TestDatabase.insertFood(name = "Banana")
        TestDatabase.insertFood(name = "Apple")

        // check that wildcard escaping does not break ordinary substring search.
        val results = DiaryService.searchFood("ban")

        assertEquals(1, results.size)
        assertEquals("Banana", results.first()["name"])
    }

    @Test
    fun deleteEntryDoesNotAllowCrossUserDeletion() {
        TestDatabase.setup()
        val alice = TestDatabase.insertUser(name = "Alice")
        val bob = TestDatabase.insertUser(name = "Bob")
        val foodId = TestDatabase.insertFood(name = "Toast")

        // Alice logs a diary entry.
        DiaryService.addEntry(alice, foodId, "breakfast", BigDecimal("100"), LocalDate.now(), null)
        val aliceEntry = DiaryService.getEntriesForDate(alice, LocalDate.now()).first()
        val aliceEntryId = aliceEntry["id"] as Int

        // Bob tries to delete Alice's entry by guessing the id.
        DiaryService.deleteEntry(aliceEntryId, bob)

        // Alice's entry must still exist.
        val aliceAfter = DiaryService.getEntriesForDate(alice, LocalDate.now())
        assertEquals(1, aliceAfter.size, "Bob must not be able to delete Alice's diary row")
        assertTrue(aliceAfter.any { it["id"] == aliceEntryId })
    }
}
