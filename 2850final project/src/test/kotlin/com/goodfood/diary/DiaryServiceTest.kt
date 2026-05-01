package com.goodfood.diary

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Unit tests for [DiaryService].
 *
 * Job stories covered (see Wiki: Job-Stories — Subscriber Features):
 *  - "Quick Meal Logging" — when I log a meal, I want to do it quickly using
 *    recent or saved meals so that I don't spend too much time on the app.
 *  - "Recipe Search" — when I want to cook, I want to search for recipes /
 *    foods using ingredients I have on hand.
 *
 * Acceptance criteria exercised:
 *  - AC-DIARY-1  A diary entry created today appears in the daily summary
 *                with calories scaled by quantity.                            [addEntryAndGetDailySummary]
 *  - AC-DIARY-2  Free-text food search returns the matching food item.       [searchFoodFindsMatchingFood]
 */
class DiaryServiceTest {

    @Test
    fun addEntryAndGetDailySummary() {
        TestDatabase.setup()

        val userId = TestDatabase.insertUser()
        val foodId = TestDatabase.insertFood(calories = BigDecimal("100.00"))

        DiaryService.addEntry(userId, foodId, "Breakfast", BigDecimal("200.00"), LocalDate.now(), "Test note")

        val summary = DiaryService.getDailySummary(userId, LocalDate.now())

        assertEquals(BigDecimal("200"), summary["calories"])
        assertEquals(BigDecimal("2.0"), summary["protein"])
    }

    @Test
    fun searchFoodFindsMatchingFood() {
        TestDatabase.setup()

        TestDatabase.insertFood(name = "Banana")

        val results = DiaryService.searchFood("banana")

        assertTrue(results.isNotEmpty())
        assertEquals("Banana", results.first()["name"])
    }
}