package com.goodfood.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionalGoalsTest {

    @Test
    fun tableNameIsCorrect() {
        assertEquals("nutritional_goals", NutritionalGoals.tableName)
    }

    @Test
    fun primaryKeyUsesIdColumn() {
        assertTrue(NutritionalGoals.primaryKey.columns.contains(NutritionalGoals.id))
    }
}