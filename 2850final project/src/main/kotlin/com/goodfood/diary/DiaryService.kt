package com.goodfood.diary

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Food-diary read/write operations and per-day macro aggregation.
 *
 * Owns the calorie/protein/carbs/fat scaling logic (per-100g values × grams ÷ 100)
 * that powers the dashboard progress bars and the professional client-detail page.
 * All public functions are pure with respect to time — the caller passes the
 * `LocalDate` so test code can pin "today".
 */
object DiaryService {

    /**
     * Return every diary entry for [userId] on [date], joined to its `food_items`
     * row, with calorie / macro values already scaled by the entry's `quantityGrams`.
     *
     * @return one map per entry, keyed by `id`, `foodName`, `mealType`, `quantity`,
     *  `calories`, `protein`, `carbs`, `fat`, `notes`. Empty list when nothing is logged.
     */
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
     * Sum the macro totals across [getEntriesForDate]. Returned map keys are
     * `calories`, `protein`, `carbs`, `fat`. All values are `BigDecimal.ZERO`
     * when no entries are logged for [date].
     */
    fun getDailySummary(userId: Int, date: LocalDate): Map<String, BigDecimal> {
        val entries = getEntriesForDate(userId, date)
        return mapOf("calories" to entries.sumOf { it["calories"] as BigDecimal }, "protein" to entries.sumOf { it["protein"] as BigDecimal },
            "carbs" to entries.sumOf { it["carbs"] as BigDecimal }, "fat" to entries.sumOf { it["fat"] as BigDecimal })
    }

    /**
     * Mon–Sun calorie roll-up for the current week (anchor = today's `LocalDate.now()`).
     * Powers the weekly bar chart on the goals page. Always returns 7 rows; days
     * with no entries appear as 0.
     */
    fun getWeeklySummary(userId: Int): List<Map<String, Any>> {
        val today = LocalDate.now(); val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        return getWeeklySummary(userId, monday)
    }

    /**
     * Mon–Sun calorie roll-up for the week starting at [monday]. Always returns
     * 7 rows; days with no entries appear as 0. Used by the goals page when
     * the marker scrolls back/forwards to inspect a different week.
     */
    fun getWeeklySummary(userId: Int, monday: LocalDate): List<Map<String, Any>> {
        return (0..6).map { offset ->
            val d = monday.plusDays(offset.toLong()); val summary = getDailySummary(userId, d)
            mapOf("date" to d, "dayName" to d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }, "calories" to summary["calories"]!!)
        }
    }

    /**
     * Insert a new diary entry.
     *
     * @param mealType one of `"breakfast"`, `"lunch"`, `"dinner"`, `"snack"` —
     *  used to group entries on the dashboard. Not validated here; routes
     *  should pass an enumerated value.
     * @param grams how much was eaten, in grams; combined with `food_items`'
     *  per-100g macros at read time.
     */
    fun addEntry(userId: Int, foodItemId: Int, mealType: String, grams: BigDecimal, date: LocalDate, notes: String?) = transaction {
        FoodDiaryEntries.insert {
            it[FoodDiaryEntries.userId] = userId; it[FoodDiaryEntries.foodItemId] = foodItemId; it[FoodDiaryEntries.mealType] = mealType
            it[FoodDiaryEntries.quantityGrams] = grams; it[FoodDiaryEntries.entryDate] = date; it[FoodDiaryEntries.notes] = notes
            it[FoodDiaryEntries.createdAt] = LocalDateTime.now()
        }
    }

    /**
     * Delete a single diary entry. Critically the WHERE clause includes both
     * [entryId] *and* [userId] — a user trying to delete another user's entry
     * by guessing the id will match zero rows and silently no-op.
     */
    fun deleteEntry(entryId: Int, userId: Int) = transaction {
        FoodDiaryEntries.deleteWhere { (FoodDiaryEntries.id eq entryId) and (FoodDiaryEntries.userId eq userId) }
    }

    /**
     * Escape the SQL `LIKE` wildcard characters (`%`, `_`) and the escape char itself
     * so a user-supplied search term is treated as a literal substring rather than a
     * pattern. Closes issue #19 for the food-search call site below. The escape
     * character is the default `\\`, which Exposed maps to the underlying H2 / MySQL
     * `LIKE ... ESCAPE '\\'` semantics.
     */
    private fun escapeLikePattern(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /**
     * Case-insensitive substring search over `food_items.name`. When [query] is
     * blank, returns the full library (capped at 60) — powers the visual food
     * picker grid that opens with everything visible instead of waiting for
     * the user to type. Returns `id` / `name` / `category` / `calories` plus
     * `emoji` + `tone` so the front-end can render brand-tinted cards without
     * a second round-trip.
     */
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
