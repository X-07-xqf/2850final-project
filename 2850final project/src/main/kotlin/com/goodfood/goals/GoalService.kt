package com.goodfood.goals

import com.goodfood.diary.NutritionalGoals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

object GoalService {

    fun getGoals(userId: Int): Map<String, BigDecimal?>? = transaction {
        NutritionalGoals.selectAll().where { NutritionalGoals.userId eq userId }
            .orderBy(NutritionalGoals.setAt, SortOrder.DESC).firstOrNull()?.let { row ->
                mapOf("calories" to row[NutritionalGoals.dailyCalories], "protein" to row[NutritionalGoals.dailyProtein],
                    "carbs" to row[NutritionalGoals.dailyCarbs], "fat" to row[NutritionalGoals.dailyFat], "fiber" to row[NutritionalGoals.dailyFiber])
            }
    }

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
