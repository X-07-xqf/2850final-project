package com.goodfood.recipes

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Recipes : Table("recipes") {
    val id = integer("id").autoIncrement()
    val createdBy = integer("created_by").references(Users.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val prepTimeMinutes = integer("prep_time_minutes")
    val cookTimeMinutes = integer("cook_time_minutes")
    val servings = integer("servings")
    val difficulty = varchar("difficulty", 50)
    val imageUrl = varchar("image_url", 500).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
