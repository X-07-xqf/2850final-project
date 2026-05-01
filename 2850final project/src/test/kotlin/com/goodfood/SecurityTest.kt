package com.goodfood

import com.goodfood.diary.DiaryService
import com.goodfood.recipes.RecipeService
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the four critical security fixes shipped in v0.4.4
 * (issues #16 / #17 / #18 / #19).
 *
 * Acceptance criteria exercised:
 *  - AC-SEC-1  Food search treats `%` as a literal character, not a SQL wildcard.    [searchFoodTreatsPercentAsLiteral]
 *  - AC-SEC-2  Food search treats `_` as a literal character.                        [searchFoodTreatsUnderscoreAsLiteral]
 *  - AC-SEC-3  Recipe search treats `%` as a literal character.                      [searchRecipesTreatsPercentAsLiteral]
 *  - AC-SEC-4  A normal search query still finds the matching food.                  [searchFoodHappyPathStillWorks]
 *  - AC-SEC-5  deleteEntry() refuses to delete another user's diary entry.           [deleteEntryDoesNotAllowCrossUserDeletion]
 *
 * IDOR / authorisation tests for the professional routes are at the route layer
 * (requires testApplication) — see [IntegrationTest].
 */
class SecurityTest {

    @Test
    fun searchFoodTreatsPercentAsLiteral() {
        TestDatabase.setup()
        // Seed a few food items, NONE containing a literal '%' character.
        TestDatabase.insertFood(name = "Banana")
        TestDatabase.insertFood(name = "Apple")
        TestDatabase.insertFood(name = "Chicken Breast")
        TestDatabase.insertFood(name = "Olive Oil")
        TestDatabase.insertFood(name = "Salmon")

        // Pre-fix behaviour: '%' would dump every row.
        // Post-fix behaviour: '%' is escaped and matches only foods that contain a literal '%'.
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

        // Sanity check that wildcard escaping does NOT break ordinary substring search.
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
