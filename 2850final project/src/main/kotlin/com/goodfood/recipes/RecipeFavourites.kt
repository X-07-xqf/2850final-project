package com.goodfood.recipes

import com.goodfood.auth.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object RecipeFavourites : Table("recipe_favourites") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val recipeId = integer("recipe_id").references(Recipes.id)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
