package com.goodfood.diary

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object FoodDiaryEntries : Table("food_diary_entries") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val foodItemId = integer("food_item_id").references(FoodItems.id)
    val mealType = varchar("meal_type", 50)
    val quantityGrams = decimal("quantity_grams", 10, 2)
    val entryDate = date("entry_date")
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
