package com.goodfood.diary

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object NutritionalGoals : Table("nutritional_goals") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val dailyCalories = decimal("daily_calories", 10, 2).nullable()
    val dailyProtein = decimal("daily_protein", 10, 2).nullable()
    val dailyCarbs = decimal("daily_carbs", 10, 2).nullable()
    val dailyFat = decimal("daily_fat", 10, 2).nullable()
    val dailyFiber = decimal("daily_fiber", 10, 2).nullable()
    val setAt = datetime("set_at")
    val updatedAt = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
