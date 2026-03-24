package com.goodfood.recipes

import com.goodfood.diary.FoodItems
import org.jetbrains.exposed.sql.Table

object RecipeIngredients : Table("recipe_ingredients") {
    val id = integer("id").autoIncrement()
    val recipeId = integer("recipe_id").references(Recipes.id)
    val foodItemId = integer("food_item_id").references(FoodItems.id).nullable()
    val ingredientName = varchar("ingredient_name", 255)
    val quantity = varchar("quantity", 100)
    val unit = varchar("unit", 50)
    override val primaryKey = PrimaryKey(id)
}
