package com.goodfood.goals

import com.goodfood.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.math.BigDecimal

class GoalServiceTest {

    @Test
    fun saveGoalsCreatesGoalsForUser() {
        TestDatabase.setup()

        val userId = TestDatabase.insertUser()

        GoalService.saveGoals(
            userId,
            BigDecimal("2000.00"),
            BigDecimal("100.00"),
            BigDecimal("250.00"),
            BigDecimal("70.00"),
            BigDecimal("30.00")
        )

        val goals = GoalService.getGoals(userId)

        assertNotNull(goals)
        assertEquals(BigDecimal("2000.00"), goals["calories"])
    }

    @Test
    fun saveGoalsUpdatesExistingGoals() {
        TestDatabase.setup()

        val userId = TestDatabase.insertUser()

        GoalService.saveGoals(userId, BigDecimal("2000.00"), null, null, null, null)
        GoalService.saveGoals(userId, BigDecimal("1800.00"), null, null, null, null)

        val goals = GoalService.getGoals(userId)

        assertEquals(BigDecimal("1800.00"), goals?.get("calories"))
    }
}