package com.goodfood.diary

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.LocalDate

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