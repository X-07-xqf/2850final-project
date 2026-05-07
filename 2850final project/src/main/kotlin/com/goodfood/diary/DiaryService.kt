@file:Suppress("MaxLineLength")

package com.goodfood.diary

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Food diary r/w operations and per-day macro aggregation.
 * calorie/protein/carbs/fat scaling logic (per-100g values × grams ÷ 100)
 */
object DiaryService {
    fun getEntriesForDate(userId: Int, date: LocalDate): List<Map<String, Any?>> = transaction {
        (FoodDiaryEntries innerJoin FoodItems).selectAll().where {
            (FoodDiaryEntries.userId eq userId) and (FoodDiaryEntries.entryDate eq date)
        }.orderBy(FoodDiaryEntries.mealType).map { row ->
            val qty = row[FoodDiaryEntries.quantityGrams]
            val factor = qty.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            mapOf(
                "id" to row[FoodDiaryEntries.id], "foodName" to row[FoodItems.name],
                "mealType" to row[FoodDiaryEntries.mealType], "quantity" to qty,
                "calories" to (row[FoodItems.caloriesPer100g] * factor).setScale(0, RoundingMode.HALF_UP),
                "protein" to (row[FoodItems.proteinPer100g] * factor).setScale(1, RoundingMode.HALF_UP),
                "carbs" to (row[FoodItems.carbsPer100g] * factor).setScale(1, RoundingMode.HALF_UP),
                "fat" to (row[FoodItems.fatPer100g] * factor).setScale(1, RoundingMode.HALF_UP),
                "notes" to row[FoodDiaryEntries.notes]
            )
        }
    }

    /**
     * Sum the macro totals across [getEntriesForDate], returns map keys
     * `calories`, `protein`, `carbs`, `fat`. All values are `BigDecimal.ZERO`
     * when no entries are logged for [date].
     */
    fun getDailySummary(userId: Int, date: LocalDate): Map<String, BigDecimal> {
        val entries = getEntriesForDate(userId, date)
        return mapOf("calories" to entries.sumOf { it["calories"] as BigDecimal }, "protein" to entries.sumOf { it["protein"] as BigDecimal },
            "carbs" to entries.sumOf { it["carbs"] as BigDecimal }, "fat" to entries.sumOf { it["fat"] as BigDecimal })
    }

    
    fun getWeeklySummary(userId: Int): List<Map<String, Any>> { //calculates current 'monday'
        val today = LocalDate.now(); val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        return getWeeklySummary(userId, monday)
    }

    fun getWeeklySummary(userId: Int, monday: LocalDate): List<Map<String, Any>> { //generates actual weekly summary
        return (0..6).map { offset ->
            val d = monday.plusDays(offset.toLong()); val summary = getDailySummary(userId, d)
            mapOf("date" to d, "dayName" to d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }, "calories" to summary["calories"]!!)
        }
    }

    
    fun addEntry(userId: Int, foodItemId: Int, mealType: String, grams: BigDecimal, date: LocalDate, notes: String?) = transaction {
        FoodDiaryEntries.insert {
            it[FoodDiaryEntries.userId] = userId; it[FoodDiaryEntries.foodItemId] = foodItemId; it[FoodDiaryEntries.mealType] = mealType
            it[FoodDiaryEntries.quantityGrams] = grams; it[FoodDiaryEntries.entryDate] = date; it[FoodDiaryEntries.notes] = notes
            it[FoodDiaryEntries.createdAt] = LocalDateTime.now()
        }
    }

    fun deleteEntry(entryId: Int, userId: Int) = transaction {
        FoodDiaryEntries.deleteWhere { (FoodDiaryEntries.id eq entryId) and (FoodDiaryEntries.userId eq userId) }
    }


    private fun escapeLikePattern(input: String): String = //escape wildcard chars
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


    //search food function - shows up to 60 items if query is blank. 
    fun searchFood(query: String): List<Map<String, Any>> = transaction {
        val rows = if (query.isBlank()) {
            FoodItems.selectAll().orderBy(FoodItems.name).limit(60)
        } else {
            val safe = escapeLikePattern(query.lowercase())
            FoodItems.selectAll().where { FoodItems.name.lowerCase() like "%$safe%" }
                .orderBy(FoodItems.name).limit(20)
        }
        rows.map { row ->
            val name = row[FoodItems.name]
            val category = row[FoodItems.category]
            mapOf(
                "id" to row[FoodItems.id],
                "name" to name,
                "category" to category,
                "calories" to row[FoodItems.caloriesPer100g],
                "emoji" to com.goodfood.util.foodEmoji(name),
                "tone" to com.goodfood.util.foodTone(category)
            )
        }
    }
}
