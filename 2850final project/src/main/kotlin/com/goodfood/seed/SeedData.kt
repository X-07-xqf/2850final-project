package com.goodfood.seed

import at.favre.lib.crypto.bcrypt.BCrypt
import com.goodfood.auth.Users
import com.goodfood.diary.*
import com.goodfood.messages.AdviceMessages
import com.goodfood.professional.*
import com.goodfood.recipes.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object SeedData {
    private fun hash(pw: String): String = BCrypt.withDefaults().hashToString(12, pw.toCharArray())

    /**
     * Image-URL backfill for the three seed recipes. Idempotent — only sets
     * `image_url` when the row exists *and* the column is currently null, so
     * it's safe to run on every boot regardless of whether the deploy started
     * from a fresh DB or one that's been live since before this column was
     * being populated.
     */
    private val seedRecipeImages = mapOf(
        "Grilled Chicken Salad" to "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&auto=format&fit=crop&q=80",
        "Overnight Oats Bowl" to "https://images.unsplash.com/photo-1517673400267-0251440c45dc?w=800&auto=format&fit=crop&q=80",
        "Grilled Salmon with Veggies" to "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=800&auto=format&fit=crop&q=80"
    )

    fun backfillImageUrls() {
        transaction {
            for ((title, url) in seedRecipeImages) {
                Recipes.update({ (Recipes.title eq title) and Recipes.imageUrl.isNull() }) {
                    it[imageUrl] = url
                }
            }
        }
    }

    fun insertIfEmpty() {
        transaction {
            if (Users.selectAll().count() > 0L) return@transaction

            val now = LocalDateTime.now()

            val alice = Users.insert {
                it[fullName] = "Alice Johnson"; it[email] = "alice@email.com"; it[passwordHash] = hash("password")
                it[role] = "subscriber"; it[createdAt] = now
            } get Users.id

            val bob = Users.insert {
                it[fullName] = "Bob Williams"; it[email] = "bob@email.com"; it[passwordHash] = hash("password")
                it[role] = "subscriber"; it[createdAt] = now
            } get Users.id

            val drSarah = Users.insert {
                it[fullName] = "Dr. Sarah Williams"; it[email] = "sarah@clinic.com"; it[passwordHash] = hash("password")
                it[role] = "professional"; it[createdAt] = now
            } get Users.id

            ProfessionalProfiles.insert {
                it[userId] = drSarah; it[specialty] = "Nutrition & Dietetics"; it[qualification] = "PhD in Nutritional Science"
                it[bio] = "Experienced nutritionist helping clients achieve their health goals."; it[createdAt] = now
            }

            ClientRelationships.insert { it[professionalId] = drSarah; it[subscriberId] = alice; it[status] = "active"; it[startedAt] = now }
            ClientRelationships.insert { it[professionalId] = drSarah; it[subscriberId] = bob; it[status] = "active"; it[startedAt] = now }

            data class FoodSeed(val name: String, val cat: String, val cal: String, val prot: String, val carb: String, val fat: String, val fiber: String? = null, val sugar: String? = null)
            val foods = listOf(
                FoodSeed("Oatmeal", "Grains", "68", "2.4", "12", "1.4", "1.7", "0.3"),
                FoodSeed("Banana", "Fruits", "89", "1.1", "23", "0.3", "2.6", "12.2"),
                FoodSeed("Chicken Breast (grilled)", "Meat", "165", "31", "0", "3.6", "0", "0"),
                FoodSeed("Brown Rice", "Grains", "123", "2.7", "26", "1", "1.8", "0.4"),
                FoodSeed("Mixed Salad Greens", "Vegetables", "20", "1.5", "3.5", "0.2", "2", "1.3"),
                FoodSeed("Greek Yogurt", "Dairy", "59", "10", "3.6", "0.4", "0", "3.2"),
                FoodSeed("Almonds", "Nuts", "579", "21", "22", "50", "12.5", "4.4"),
                FoodSeed("Salmon (grilled)", "Fish", "208", "20", "0", "13", "0", "0"),
                FoodSeed("Broccoli", "Vegetables", "34", "2.8", "7", "0.4", "2.6", "1.7"),
                FoodSeed("Egg (boiled)", "Dairy", "155", "13", "1.1", "11", "0", "1.1"),
                FoodSeed("Whole Wheat Bread", "Grains", "247", "13", "41", "3.4", "7", "6"),
                FoodSeed("Avocado", "Fruits", "160", "2", "9", "15", "6.7", "0.7"),
                FoodSeed("Sweet Potato", "Vegetables", "86", "1.6", "20", "0.1", "3", "4.2"),
                FoodSeed("Lentils (cooked)", "Legumes", "116", "9", "20", "0.4", "7.9", "1.8"),
                FoodSeed("Olive Oil", "Oils", "884", "0", "0", "100", "0", "0"),
                FoodSeed("Cherry Tomatoes", "Vegetables", "18", "0.9", "3.9", "0.2", "1.2", "2.6"),
                FoodSeed("Cucumber", "Vegetables", "15", "0.7", "3.6", "0.1", "0.5", "1.7"),
                FoodSeed("Green Tea", "Beverages", "1", "0.2", "0", "0", "0", "0"),
                FoodSeed("Chia Seeds", "Seeds", "486", "17", "42", "31", "34", "0"),
                FoodSeed("Tofu", "Legumes", "76", "8", "1.9", "4.8", "0.3", "0.6")
            )

            val foodIds = mutableMapOf<String, Int>()
            for (f in foods) {
                val fid = FoodItems.insert {
                    it[name] = f.name; it[category] = f.cat; it[caloriesPer100g] = BigDecimal(f.cal)
                    it[proteinPer100g] = BigDecimal(f.prot); it[carbsPer100g] = BigDecimal(f.carb); it[fatPer100g] = BigDecimal(f.fat)
                    it[fiberPer100g] = f.fiber?.let { v -> BigDecimal(v) }; it[sugarPer100g] = f.sugar?.let { v -> BigDecimal(v) }
                    it[createdAt] = now
                } get FoodItems.id
                foodIds[f.name] = fid
            }

            val today = LocalDate.now()
            fun diary(uid: Int, food: String, meal: String, grams: String) {
                FoodDiaryEntries.insert {
                    it[userId] = uid; it[foodItemId] = foodIds[food]!!; it[mealType] = meal
                    it[quantityGrams] = BigDecimal(grams); it[entryDate] = today; it[createdAt] = now
                }
            }
            diary(alice, "Oatmeal", "breakfast", "250"); diary(alice, "Banana", "breakfast", "120")
            diary(alice, "Green Tea", "breakfast", "250"); diary(alice, "Chicken Breast (grilled)", "lunch", "200")
            diary(alice, "Mixed Salad Greens", "lunch", "150"); diary(alice, "Brown Rice", "lunch", "200")
            diary(alice, "Greek Yogurt", "snack", "150"); diary(alice, "Almonds", "snack", "30")

            NutritionalGoals.insert {
                it[userId] = alice; it[dailyCalories] = BigDecimal("2000"); it[dailyProtein] = BigDecimal("80")
                it[dailyCarbs] = BigDecimal("250"); it[dailyFat] = BigDecimal("65"); it[dailyFiber] = BigDecimal("30"); it[setAt] = now
            }

            val r1 = Recipes.insert {
                it[createdBy] = drSarah; it[title] = "Grilled Chicken Salad"
                it[description] = "A fresh and healthy salad with grilled chicken breast, mixed greens, and a light olive oil dressing."
                it[prepTimeMinutes] = 10; it[cookTimeMinutes] = 15; it[servings] = 2; it[difficulty] = "easy"
                it[imageUrl] = seedRecipeImages["Grilled Chicken Salad"]; it[createdAt] = now
            } get Recipes.id
            for ((ing, qty, u) in listOf(Triple("Chicken Breast (grilled)", "200", "g"), Triple("Mixed Salad Greens", "150", "g"),
                Triple("Cherry Tomatoes", "100", "g"), Triple("Cucumber", "50", "g"), Triple("Olive Oil", "2", "tbsp"))) {
                RecipeIngredients.insert { it[recipeId] = r1; it[foodItemId] = foodIds[ing]; it[ingredientName] = ing; it[quantity] = qty; it[unit] = u }
            }
            for ((n, inst) in listOf(1 to "Season chicken breast with salt and pepper, grill for 6-7 minutes per side.",
                2 to "Wash and chop salad greens, tomatoes, and cucumber.", 3 to "Slice grilled chicken into strips.",
                4 to "Combine all vegetables, top with chicken, drizzle with olive oil.")) {
                RecipeSteps.insert { it[recipeId] = r1; it[stepNumber] = n; it[instruction] = inst }
            }

            val r2 = Recipes.insert {
                it[createdBy] = drSarah; it[title] = "Overnight Oats Bowl"
                it[description] = "A quick and nutritious breakfast bowl prepared the night before."
                it[prepTimeMinutes] = 10; it[cookTimeMinutes] = 0; it[servings] = 1; it[difficulty] = "easy"
                it[imageUrl] = seedRecipeImages["Overnight Oats Bowl"]; it[createdAt] = now
            } get Recipes.id
            for ((ing, qty, u) in listOf(Triple("Oatmeal", "80", "g"), Triple("Greek Yogurt", "100", "g"),
                Triple("Banana", "1", "medium"), Triple("Chia Seeds", "15", "g"), Triple("Almonds", "20", "g"))) {
                RecipeIngredients.insert { it[recipeId] = r2; it[foodItemId] = foodIds[ing]; it[ingredientName] = ing; it[quantity] = qty; it[unit] = u }
            }
            for ((n, inst) in listOf(1 to "Mix oats, yogurt, and chia seeds in a jar.",
                2 to "Refrigerate overnight.", 3 to "Top with sliced banana and almonds before serving.")) {
                RecipeSteps.insert { it[recipeId] = r2; it[stepNumber] = n; it[instruction] = inst }
            }

            val r3 = Recipes.insert {
                it[createdBy] = drSarah; it[title] = "Grilled Salmon with Veggies"
                it[description] = "Omega-3 rich salmon fillet with roasted broccoli and sweet potato."
                it[prepTimeMinutes] = 15; it[cookTimeMinutes] = 25; it[servings] = 2; it[difficulty] = "medium"
                it[imageUrl] = seedRecipeImages["Grilled Salmon with Veggies"]; it[createdAt] = now
            } get Recipes.id
            for ((ing, qty, u) in listOf(Triple("Salmon (grilled)", "300", "g"), Triple("Broccoli", "200", "g"),
                Triple("Sweet Potato", "200", "g"), Triple("Olive Oil", "1", "tbsp"))) {
                RecipeIngredients.insert { it[recipeId] = r3; it[foodItemId] = foodIds[ing]; it[ingredientName] = ing; it[quantity] = qty; it[unit] = u }
            }
            for ((n, inst) in listOf(1 to "Preheat oven to 200C. Dice sweet potato, toss with olive oil, roast for 15 min.",
                2 to "Add broccoli florets to the tray, roast another 10 min.", 3 to "Season salmon, grill 4-5 min per side.",
                4 to "Serve salmon over roasted vegetables.")) {
                RecipeSteps.insert { it[recipeId] = r3; it[stepNumber] = n; it[instruction] = inst }
            }

            RecipeRatings.insert { it[userId] = alice; it[recipeId] = r1; it[rating] = 5; it[comment] = "Delicious and so easy to make!"; it[createdAt] = now }
            RecipeRatings.insert { it[userId] = bob; it[recipeId] = r1; it[rating] = 4; it[comment] = "Great recipe, I added some feta cheese."; it[createdAt] = now }
            RecipeRatings.insert { it[userId] = alice; it[recipeId] = r3; it[rating] = 5; it[comment] = "Perfect dinner option!"; it[createdAt] = now }

            RecipeFavourites.insert { it[userId] = alice; it[recipeId] = r1; it[createdAt] = now }
            RecipeFavourites.insert { it[userId] = alice; it[recipeId] = r3; it[createdAt] = now }

            AdviceMessages.insert { it[senderId] = drSarah; it[receiverId] = alice; it[message] = "Hi Alice! Great job on hitting your protein goals this week!"; it[isRead] = true; it[sentAt] = now.minusDays(1) }
            AdviceMessages.insert { it[senderId] = drSarah; it[receiverId] = alice; it[message] = "I noticed your fiber intake has been low. Try adding more vegetables or whole grains."; it[isRead] = true; it[sentAt] = now.minusDays(1).plusMinutes(2) }
            AdviceMessages.insert { it[senderId] = alice; it[receiverId] = drSarah; it[message] = "Thank you Dr. Williams! Any specific foods you'd recommend for fiber?"; it[isRead] = true; it[sentAt] = now.minusDays(1).plusHours(1) }
            AdviceMessages.insert { it[senderId] = drSarah; it[receiverId] = alice; it[message] = "Try broccoli (5.1g per cup), lentils (15.6g per cup), or chia seeds (10g per oz)!"; it[isRead] = false; it[sentAt] = now }
        }
    }
}
