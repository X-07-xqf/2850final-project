package com.goodfood.recipes

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object RecipeRatings : Table("recipe_ratings") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val recipeId = integer("recipe_id").references(Recipes.id)
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
