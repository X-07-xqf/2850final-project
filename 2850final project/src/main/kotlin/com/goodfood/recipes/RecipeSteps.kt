package com.goodfood.recipes

import org.jetbrains.exposed.sql.Table

object RecipeSteps : Table("recipe_steps") {
    val id = integer("id").autoIncrement()
    val recipeId = integer("recipe_id").references(Recipes.id)
    val stepNumber = integer("step_number")
    val instruction = text("instruction")
    override val primaryKey = PrimaryKey(id)
}
