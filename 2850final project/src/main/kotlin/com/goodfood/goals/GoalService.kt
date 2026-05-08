@file:Suppress("MaxLineLength")

package com.goodfood.goals

import com.goodfood.diary.NutritionalGoals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Read/write goal targets — daily calorie + macro targets per user.
 */
object GoalService {
    /**
     * Latest goals row for [userId], or null if the user has never set goals.
     * Macro values may be null when the user left a target blank.
     */
    fun getGoals(userId: Int): Map<String, BigDecimal?>? = transaction {
        NutritionalGoals.selectAll().where { NutritionalGoals.userId eq userId }
            .orderBy(NutritionalGoals.setAt, SortOrder.DESC).firstOrNull()?.let { row ->
                mapOf("calories" to row[NutritionalGoals.dailyCalories], "protein" to row[NutritionalGoals.dailyProtein],
                    "carbs" to row[NutritionalGoals.dailyCarbs], "fat" to row[NutritionalGoals.dailyFat], "fiber" to row[NutritionalGoals.dailyFiber])
            }
    }

    /**
     * Upsert: overwrite the existing goals row for [userId] when one exists,
     * otherwise insert a fresh row with the supplied values.
     */
    fun saveGoals(userId: Int, cal: BigDecimal?, prot: BigDecimal?, carbs: BigDecimal?, fat: BigDecimal?, fiber: BigDecimal?) = transaction {
        val existing = NutritionalGoals.selectAll().where { NutritionalGoals.userId eq userId }.firstOrNull()
        if (existing != null) {
            NutritionalGoals.update({ NutritionalGoals.userId eq userId }) {
                it[NutritionalGoals.dailyCalories] = cal; it[NutritionalGoals.dailyProtein] = prot; it[NutritionalGoals.dailyCarbs] = carbs; it[NutritionalGoals.dailyFat] = fat; it[NutritionalGoals.dailyFiber] = fiber
                it[NutritionalGoals.updatedAt] = LocalDateTime.now()
            }
        } else {
            NutritionalGoals.insert {
                it[NutritionalGoals.userId] = userId; it[NutritionalGoals.dailyCalories] = cal; it[NutritionalGoals.dailyProtein] = prot; it[NutritionalGoals.dailyCarbs] = carbs
                it[NutritionalGoals.dailyFat] = fat; it[NutritionalGoals.dailyFiber] = fiber; it[NutritionalGoals.setAt] = LocalDateTime.now()
            }
        }
    }
}
