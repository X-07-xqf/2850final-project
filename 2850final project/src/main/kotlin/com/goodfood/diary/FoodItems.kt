package com.goodfood.diary

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object FoodItems : Table("food_items") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val category = varchar("category", 100)
    val caloriesPer100g = decimal("calories_per_100g", 10, 2)
    val proteinPer100g = decimal("protein_per_100g", 10, 2)
    val carbsPer100g = decimal("carbs_per_100g", 10, 2)
    val fatPer100g = decimal("fat_per_100g", 10, 2)
    val fiberPer100g = decimal("fiber_per_100g", 10, 2).nullable()
    val sugarPer100g = decimal("sugar_per_100g", 10, 2).nullable()
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
