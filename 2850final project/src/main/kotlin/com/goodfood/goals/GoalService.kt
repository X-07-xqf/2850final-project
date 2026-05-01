package com.goodfood.goals

import com.goodfood.diary.NutritionalGoals
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Read/write nutritional goal targets per user.
 *
 * The schema allows multiple historical rows per user (`set_at` timestamp), but
 * the application UX is "one current goal", so [saveGoals] does an upsert on
 * `user_id` rather than appending a new row.
 */
object GoalService {

    /**
     * Most-recent goals for [userId]; `null` when the user has never set goals.
     * Returned map keys: `calories`, `protein`, `carbs`, `fat`, `fiber`. Any
     * individual macro can itself be `null` if the user only filled in some.
     */
    fun getGoals(userId: Int): Map<String, BigDecimal?>? = transaction {
        NutritionalGoals.selectAll().where { NutritionalGoals.userId eq userId }
            .orderBy(NutritionalGoals.setAt, SortOrder.DESC).firstOrNull()?.let { row ->
                mapOf("calories" to row[NutritionalGoals.dailyCalories], "protein" to row[NutritionalGoals.dailyProtein],
                    "carbs" to row[NutritionalGoals.dailyCarbs], "fat" to row[NutritionalGoals.dailyFat], "fiber" to row[NutritionalGoals.dailyFiber])
            }
    }

    /**
     * Upsert nutritional goals for [userId]. If the user already has a row, every
     * macro is overwritten (passing `null` zeros that macro). If not, a new row
     * is inserted with the supplied values.
     *
     * @param cal daily calorie target in kcal
     * @param prot daily protein target in grams
     * @param carbs daily carbohydrate target in grams
     * @param fat daily fat target in grams
     * @param fiber daily fibre target in grams (`null` is acceptable — fibre is
     *  not surfaced on every page).
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
