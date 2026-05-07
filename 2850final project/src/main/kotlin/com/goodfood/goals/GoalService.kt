@file:Suppress("MaxLineLength")

package com.goodfood.goals

import com.goodfood.diary.NutritionalGoals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

//read/write goal targets
object GoalService {


    //get most recent goals for a user, null if no goals ever set. returns calories, protein, carbs, fat,fibre
    //macros can also be null if left blank by user
    fun getGoals(userId: Int): Map<String, BigDecimal?>? = transaction {
        NutritionalGoals.selectAll().where { NutritionalGoals.userId eq userId }
            .orderBy(NutritionalGoals.setAt, SortOrder.DESC).firstOrNull()?.let { row ->
                mapOf("calories" to row[NutritionalGoals.dailyCalories], "protein" to row[NutritionalGoals.dailyProtein],
                    "carbs" to row[NutritionalGoals.dailyCarbs], "fat" to row[NutritionalGoals.dailyFat], "fiber" to row[NutritionalGoals.dailyFiber])
            }
    }

    
    //upsert goals for a user. if a row exists, it is overwritten. if not, a new row is inserted with the given values
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
