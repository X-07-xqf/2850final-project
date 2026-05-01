package com.goodfood.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Schema-level tests for the [NutritionalGoals] Exposed table definition.
 *
 * These guard the contract the rest of the system relies on (table name and
 * primary key wiring) so that an accidental rename in the table object
 * triggers an immediate test failure rather than a silent runtime crash.
 *
 * Acceptance criteria exercised:
 *  - AC-DB-1  Database table is named `nutritional_goals` (matches schema docs in Wiki: Database-Diagram). [tableNameIsCorrect]
 *  - AC-DB-2  Primary key is the `id` column.                                                              [primaryKeyUsesIdColumn]
 */
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