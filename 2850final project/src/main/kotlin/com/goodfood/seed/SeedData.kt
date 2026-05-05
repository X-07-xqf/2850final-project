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

    /**
     * Specs for the six recipes added in v0.6.8 — six titles covering
     * breakfast / lunch / dinner that re-use the existing `food_items`
     * library so per-serving nutrition computes automatically. Single
     * source of truth used by both [insertIfEmpty] (fresh DB) and
     * [backfillExtraRecipes] (existing live DB on Render).
     */
    private data class IngredientSpec(val foodName: String, val quantity: String, val unit: String = "g")
    private data class ExtraRecipe(
        val title: String, val description: String,
        val prepMin: Int, val cookMin: Int, val servings: Int, val difficulty: String,
        val imageUrl: String,
        val ingredients: List<IngredientSpec>, val steps: List<String>
    )

    private val extraRecipes: List<ExtraRecipe> = listOf(
        ExtraRecipe(
            "Avocado Toast",
            "Smashed avocado on whole wheat with a soft-boiled egg, cherry tomatoes, and a drizzle of olive oil.",
            5, 5, 1, "easy",
            "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Whole Wheat Bread", "60"),
                IngredientSpec("Avocado", "100"),
                IngredientSpec("Egg (boiled)", "50"),
                IngredientSpec("Cherry Tomatoes", "60"),
                IngredientSpec("Olive Oil", "5", "tsp")
            ),
            listOf(
                "Toast the bread until golden.",
                "Mash the avocado with a drizzle of olive oil and a pinch of salt; spread over the toast.",
                "Slice the boiled egg in half and arrange on top.",
                "Halve the cherry tomatoes and scatter over the toast."
            )
        ),
        ExtraRecipe(
            "Greek Yogurt Parfait",
            "Layered breakfast bowl with creamy yogurt, sweet banana, crunchy almonds, and chia.",
            5, 0, 1, "easy",
            "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Greek Yogurt", "200"),
                IngredientSpec("Banana", "100"),
                IngredientSpec("Almonds", "15"),
                IngredientSpec("Chia Seeds", "10")
            ),
            listOf(
                "Spoon half the yogurt into a glass or jar.",
                "Slice the banana and add a layer over the yogurt.",
                "Top with the rest of the yogurt and a sprinkle of chia seeds.",
                "Finish with the almonds for crunch."
            )
        ),
        ExtraRecipe(
            "Tofu & Brown Rice Bowl",
            "Seared tofu, fluffy brown rice, steamed broccoli, and fresh greens — a balanced lunch bowl.",
            10, 15, 2, "easy",
            "https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Tofu", "200"),
                IngredientSpec("Brown Rice", "250"),
                IngredientSpec("Broccoli", "150"),
                IngredientSpec("Mixed Salad Greens", "80"),
                IngredientSpec("Olive Oil", "10", "tsp")
            ),
            listOf(
                "Cook the brown rice according to package instructions.",
                "Cube the tofu and pan-sear in olive oil until golden on all sides.",
                "Steam the broccoli for 4-5 minutes until tender-crisp.",
                "Divide rice between two bowls; top with tofu, broccoli, and greens."
            )
        ),
        ExtraRecipe(
            "Avocado Egg Wrap",
            "Boiled egg and avocado folded into a soft wholewheat wrap with crunchy greens and tomato.",
            10, 5, 1, "easy",
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Whole Wheat Bread", "80"),
                IngredientSpec("Egg (boiled)", "100"),
                IngredientSpec("Avocado", "80"),
                IngredientSpec("Mixed Salad Greens", "50"),
                IngredientSpec("Cherry Tomatoes", "60")
            ),
            listOf(
                "Warm the bread briefly to soften it.",
                "Mash the avocado and spread across the bread.",
                "Slice the boiled egg and layer on top with the greens and halved tomatoes.",
                "Roll up tightly and slice in half."
            )
        ),
        ExtraRecipe(
            "Tofu Broccoli Stir-Fry",
            "Crisp tofu and bright broccoli over brown rice — fast, vegan, satisfying weeknight dinner.",
            15, 15, 2, "medium",
            "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Tofu", "250"),
                IngredientSpec("Broccoli", "250"),
                IngredientSpec("Brown Rice", "300"),
                IngredientSpec("Olive Oil", "15", "tsp")
            ),
            listOf(
                "Cook the brown rice while you prep.",
                "Press and cube the tofu; sear in a hot pan with half the olive oil until golden on each side.",
                "Add the broccoli florets and the rest of the oil; stir-fry for 4-5 minutes until tender-crisp.",
                "Serve over the rice with a splash of soy sauce if you like."
            )
        ),
        ExtraRecipe(
            "Lentil & Sweet Potato Curry",
            "Warming fibre-rich curry — sweet potato and broccoli in a lightly spiced lentil base.",
            15, 30, 4, "medium",
            "https://images.unsplash.com/photo-1580013759032-c96505e24c1f?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Lentils (cooked)", "400"),
                IngredientSpec("Sweet Potato", "400"),
                IngredientSpec("Broccoli", "200"),
                IngredientSpec("Olive Oil", "20", "tsp")
            ),
            listOf(
                "Cube the sweet potato and sauté in olive oil for 5 minutes.",
                "Add curry powder, ground cumin, and a pinch of chilli; toast for 30 seconds.",
                "Stir in the lentils and 200ml water; simmer for 15 minutes until the sweet potato is tender.",
                "Add the broccoli florets and cook for a final 5-7 minutes.",
                "Season generously and serve with rice or flatbread."
            )
        ),

        // v0.6.41 — 14 more recipes to bring the total to 23. All ingredients
        // map to rows in the existing 57-food library; all imageUrls are
        // Unsplash photo URLs verified 200 before adding.

        ExtraRecipe(
            "Chicken & Quinoa Power Bowl",
            "High-protein lunch bowl — grilled chicken on fluffy quinoa with greens, peppers, and lemon.",
            10, 15, 2, "easy",
            "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Chicken Breast (grilled)", "240"),
                IngredientSpec("Quinoa", "200"),
                IngredientSpec("Spinach", "80"),
                IngredientSpec("Bell Pepper", "100"),
                IngredientSpec("Olive Oil", "10", "tsp"),
                IngredientSpec("Lemon", "20")
            ),
            listOf(
                "Cook the quinoa in salted water for 12 minutes; fluff with a fork.",
                "Slice the bell pepper and toss with a teaspoon of olive oil.",
                "Divide quinoa between two bowls, add spinach and peppers.",
                "Slice the grilled chicken on top and finish with a squeeze of lemon."
            )
        ),
        ExtraRecipe(
            "Berry Smoothie Bowl",
            "Frozen berries and banana blitzed thick with yogurt — topped with chia and almonds.",
            5, 0, 1, "easy",
            "https://images.unsplash.com/photo-1490474418585-ba9bad8fd0ea?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Greek Yogurt", "200"),
                IngredientSpec("Blueberries", "80"),
                IngredientSpec("Strawberries", "80"),
                IngredientSpec("Banana", "100"),
                IngredientSpec("Almonds", "15"),
                IngredientSpec("Chia Seeds", "10")
            ),
            listOf(
                "Blend the yogurt, half the banana, and the frozen berries until thick.",
                "Pour into a wide bowl.",
                "Slice the rest of the banana and arrange on top.",
                "Scatter almonds and chia over the surface."
            )
        ),
        ExtraRecipe(
            "Shrimp & Avocado Salad",
            "Quick sautéed shrimp on a bed of greens with avocado, tomatoes, and a lemon-olive dressing.",
            10, 5, 1, "easy",
            "https://images.unsplash.com/photo-1505253758473-96b7015fcd40?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Shrimp", "150"),
                IngredientSpec("Avocado", "100"),
                IngredientSpec("Mixed Salad Greens", "80"),
                IngredientSpec("Cherry Tomatoes", "80"),
                IngredientSpec("Lemon", "15"),
                IngredientSpec("Olive Oil", "8", "tsp")
            ),
            listOf(
                "Sauté the shrimp in half the olive oil for 2-3 minutes until pink.",
                "Whisk the rest of the olive oil with lemon juice and a pinch of salt.",
                "Pile greens on a plate, add halved tomatoes and sliced avocado.",
                "Top with the warm shrimp and drizzle with the dressing."
            )
        ),
        ExtraRecipe(
            "Beef & Pepper Stir-Fry",
            "Lean ground beef with peppers, onion, and mushroom over white rice — fast weeknight dinner.",
            10, 15, 2, "medium",
            "https://images.unsplash.com/photo-1574484284002-952d92456975?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Beef (lean ground)", "300"),
                IngredientSpec("Bell Pepper", "150"),
                IngredientSpec("Onion", "80"),
                IngredientSpec("Mushroom", "100"),
                IngredientSpec("White Rice", "300"),
                IngredientSpec("Olive Oil", "10", "tsp")
            ),
            listOf(
                "Cook the white rice while you prep the rest.",
                "Heat the olive oil and sear the ground beef until browned.",
                "Add the sliced onion, peppers, and mushroom; stir-fry for 5-6 minutes.",
                "Season with soy sauce and serve over the rice."
            )
        ),
        ExtraRecipe(
            "Spinach Mushroom Omelette",
            "Three-egg omelette folded over wilted spinach, mushrooms, and a sprinkle of cheddar.",
            5, 8, 1, "easy",
            "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Egg (boiled)", "150"),
                IngredientSpec("Spinach", "60"),
                IngredientSpec("Mushroom", "60"),
                IngredientSpec("Cheddar Cheese", "20"),
                IngredientSpec("Olive Oil", "5", "tsp")
            ),
            listOf(
                "Sauté the sliced mushrooms in olive oil for 3 minutes; add spinach and let it wilt.",
                "Beat the eggs, season, pour into the pan over the vegetables.",
                "Cook on medium-low until the surface just sets.",
                "Sprinkle cheddar across one half, fold the other half over, and slide onto a plate."
            )
        ),
        ExtraRecipe(
            "Pasta Primavera",
            "Spring vegetable pasta — broccoli, cherry tomatoes, and peppers tossed with olive oil.",
            10, 12, 2, "easy",
            "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Pasta", "200"),
                IngredientSpec("Broccoli", "150"),
                IngredientSpec("Cherry Tomatoes", "120"),
                IngredientSpec("Bell Pepper", "100"),
                IngredientSpec("Olive Oil", "15", "tsp")
            ),
            listOf(
                "Cook the pasta in salted water until al dente; reserve a cup of cooking water.",
                "In the last 4 minutes of cooking, drop the broccoli florets in with the pasta.",
                "Sauté peppers and tomatoes in olive oil until the tomatoes start to burst.",
                "Toss the drained pasta and broccoli through the pan with a splash of pasta water."
            )
        ),
        ExtraRecipe(
            "Tuna & Bean Salad",
            "Pantry-friendly cold salad — canned tuna, black beans, cucumber, and greens with a lemon-olive dressing.",
            10, 0, 1, "easy",
            "https://images.unsplash.com/photo-1502741224143-90386d7f8c82?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Tuna (canned)", "120"),
                IngredientSpec("Black Beans", "150"),
                IngredientSpec("Mixed Salad Greens", "80"),
                IngredientSpec("Cucumber", "100"),
                IngredientSpec("Lemon", "15"),
                IngredientSpec("Olive Oil", "8", "tsp")
            ),
            listOf(
                "Drain the tuna and the black beans; rinse the beans.",
                "Whisk lemon juice with olive oil and a pinch of salt.",
                "Toss greens, beans, and diced cucumber with most of the dressing.",
                "Top with the tuna; spoon over the rest of the dressing."
            )
        ),
        ExtraRecipe(
            "Sweet Potato & Black Bean Bowl",
            "Roasted sweet potato over rice with black beans, avocado, and spinach — vegetarian filling lunch.",
            15, 25, 2, "medium",
            "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Sweet Potato", "300"),
                IngredientSpec("Black Beans", "200"),
                IngredientSpec("Avocado", "100"),
                IngredientSpec("Spinach", "80"),
                IngredientSpec("Brown Rice", "250"),
                IngredientSpec("Olive Oil", "15", "tsp")
            ),
            listOf(
                "Heat the oven to 200°C. Cube the sweet potato, toss with olive oil and a pinch of cumin and chilli.",
                "Roast for 22-25 minutes until the edges caramelise.",
                "Cook the brown rice while the potatoes are in the oven; warm the black beans through.",
                "Build bowls: rice, then beans, sweet potato, sliced avocado, and a handful of fresh spinach."
            )
        ),
        ExtraRecipe(
            "Pineapple Shrimp Skewers",
            "Sweet-and-savoury shrimp and pineapple skewers with peppers and onion — grill or oven.",
            15, 8, 2, "medium",
            "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Shrimp", "300"),
                IngredientSpec("Pineapple", "200"),
                IngredientSpec("Bell Pepper", "150"),
                IngredientSpec("Onion", "80"),
                IngredientSpec("Olive Oil", "10", "tsp")
            ),
            listOf(
                "Cube the pineapple, peppers, and onion to roughly the size of the shrimp.",
                "Thread the skewers, alternating shrimp and pieces; brush with olive oil and a pinch of salt.",
                "Grill or roast at 220°C for 6-8 minutes, turning once.",
                "Finish with a squeeze of lime if you have one."
            )
        ),
        ExtraRecipe(
            "Apple Cinnamon Oatmeal",
            "Warm bowl of oats with apple, almonds, chia, and a splash of milk — autumn breakfast.",
            3, 5, 1, "easy",
            "https://images.unsplash.com/photo-1485963631004-f2f00b1d6606?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Oatmeal", "60"),
                IngredientSpec("Apple", "120"),
                IngredientSpec("Almonds", "10"),
                IngredientSpec("Milk (2%)", "200"),
                IngredientSpec("Chia Seeds", "5")
            ),
            listOf(
                "Bring the milk and a pinch of salt to a gentle simmer.",
                "Stir in the oats and grated apple; cook for 4-5 minutes until thick.",
                "Sprinkle ground cinnamon and let it sit for a minute off the heat.",
                "Top with the almonds and chia seeds."
            )
        ),
        ExtraRecipe(
            "Chickpea Curry",
            "Lightly spiced chickpea and sweet potato curry over brown rice — fibre-rich vegetarian dinner.",
            10, 25, 4, "medium",
            "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Chickpeas", "400"),
                IngredientSpec("Sweet Potato", "350"),
                IngredientSpec("Spinach", "150"),
                IngredientSpec("Brown Rice", "300"),
                IngredientSpec("Olive Oil", "15", "tsp")
            ),
            listOf(
                "Cube the sweet potato; sauté with olive oil for 5 minutes until it starts to soften.",
                "Add curry powder, garam masala, and a pinch of chilli; stir for 30 seconds.",
                "Pour in the drained chickpeas plus 250ml water; simmer 15 minutes.",
                "Stir spinach through at the end and serve over the brown rice."
            )
        ),
        ExtraRecipe(
            "Turkey & Hummus Wrap",
            "Sliced turkey breast and hummus rolled with cucumber and peppers in a wholewheat wrap.",
            8, 0, 1, "easy",
            "https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Turkey Breast", "120"),
                IngredientSpec("Hummus", "60"),
                IngredientSpec("Cucumber", "80"),
                IngredientSpec("Mixed Salad Greens", "40"),
                IngredientSpec("Bell Pepper", "60"),
                IngredientSpec("Whole Wheat Bread", "80")
            ),
            listOf(
                "Warm the bread briefly so it rolls without cracking.",
                "Spread hummus across the surface, leaving a 2cm border.",
                "Layer turkey, sliced cucumber, peppers, and greens.",
                "Roll up tightly and slice in half on the diagonal."
            )
        ),
        ExtraRecipe(
            "Cottage Cheese & Berry Bowl",
            "Cottage cheese topped with fresh berries, almonds, and chia — high-protein quick breakfast.",
            5, 0, 1, "easy",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Cottage Cheese", "200"),
                IngredientSpec("Blueberries", "60"),
                IngredientSpec("Strawberries", "60"),
                IngredientSpec("Almonds", "15"),
                IngredientSpec("Chia Seeds", "8")
            ),
            listOf(
                "Spoon the cottage cheese into a wide bowl.",
                "Slice the strawberries and pile both berries on top.",
                "Scatter almonds and chia across the surface.",
                "A drizzle of honey is optional but lovely."
            )
        ),
        ExtraRecipe(
            "Salmon Quinoa Bowl",
            "Flaked grilled salmon over quinoa with spinach, avocado, and lemon — omega-3 rich dinner.",
            10, 15, 2, "medium",
            "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&auto=format&fit=crop&q=80",
            listOf(
                IngredientSpec("Salmon (grilled)", "240"),
                IngredientSpec("Quinoa", "200"),
                IngredientSpec("Spinach", "100"),
                IngredientSpec("Avocado", "100"),
                IngredientSpec("Lemon", "20"),
                IngredientSpec("Olive Oil", "10", "tsp")
            ),
            listOf(
                "Cook the quinoa in salted water for 12 minutes; fluff with a fork.",
                "Wilt the spinach in a hot pan with half the olive oil for a minute.",
                "Build bowls with quinoa, then spinach, sliced avocado.",
                "Flake the salmon over the top and finish with a generous squeeze of lemon."
            )
        )
    )

    /**
     * Insert the recipes from [extraRecipes] that aren't already present, by title.
     * Idempotent — safe to call on every boot. Sarah Williams (the professional in
     * the seed data) is set as the author; if for some reason she isn't in the DB,
     * we silently no-op rather than crash startup.
     */
    /**
     * Demo-account convenience: ensure the seeded `alice@email.com` user has
     * diary entries for **today** so the marker doesn't open `/dashboard` and
     * see "Nothing here yet" rows. Idempotent — only inserts when Alice has
     * zero entries for `LocalDate.now()`. Real users (who registered through
     * the UI) are untouched. Real entries Alice added on previous days are
     * untouched too; this only fills today's gap.
     */
    fun ensureAliceHasTodayEntries() {
        transaction {
            val alice = Users.selectAll().where { Users.email eq "alice@email.com" }.singleOrNull() ?: return@transaction
            val aliceId = alice[Users.id]
            val today = LocalDate.now()
            val hasToday = FoodDiaryEntries.selectAll().where {
                (FoodDiaryEntries.userId eq aliceId) and (FoodDiaryEntries.entryDate eq today)
            }.count() > 0L
            if (hasToday) return@transaction

            val foodMap = FoodItems.selectAll().associate { it[FoodItems.name] to it[FoodItems.id] }
            val now = LocalDateTime.now()
            fun diary(food: String, meal: String, grams: String) {
                val foodId = foodMap[food] ?: return
                FoodDiaryEntries.insert {
                    it[userId] = aliceId; it[foodItemId] = foodId; it[mealType] = meal
                    it[quantityGrams] = BigDecimal(grams); it[entryDate] = today; it[createdAt] = now
                }
            }
            diary("Oatmeal", "breakfast", "250"); diary("Banana", "breakfast", "120")
            diary("Green Tea", "breakfast", "250"); diary("Chicken Breast (grilled)", "lunch", "200")
            diary("Mixed Salad Greens", "lunch", "150"); diary("Brown Rice", "lunch", "200")
            diary("Greek Yogurt", "snack", "150"); diary("Almonds", "snack", "30")
        }
    }

    /**
     * v0.6.33 — extra commonly-eaten foods so the diary "Add food" picker has
     * meaningful variety (apple, orange, beef, milk, peanut butter, ...). Same
     * shape as the original [FoodSeed] inserts but applied as a backfill: only
     * inserts foods whose `name` doesn't already exist, so repeated boots are
     * idempotent.
     */
    private data class ExtraFood(val name: String, val cat: String, val cal: String, val prot: String, val carb: String, val fat: String, val fiber: String? = null, val sugar: String? = null)
    private val extraFoods: List<ExtraFood> = listOf(
        // Fruits
        ExtraFood("Apple", "Fruits", "52", "0.3", "14", "0.2", "2.4", "10.4"),
        ExtraFood("Orange", "Fruits", "47", "0.9", "12", "0.1", "2.4", "9.4"),
        ExtraFood("Strawberries", "Fruits", "32", "0.7", "8", "0.3", "2", "4.9"),
        ExtraFood("Blueberries", "Fruits", "57", "0.7", "14", "0.3", "2.4", "10"),
        ExtraFood("Mango", "Fruits", "60", "0.8", "15", "0.4", "1.6", "13.7"),
        ExtraFood("Pineapple", "Fruits", "50", "0.5", "13", "0.1", "1.4", "9.9"),
        ExtraFood("Grapes", "Fruits", "67", "0.6", "17", "0.4", "0.9", "16.3"),
        ExtraFood("Watermelon", "Fruits", "30", "0.6", "8", "0.2", "0.4", "6.2"),
        ExtraFood("Lemon", "Fruits", "29", "1.1", "9", "0.3", "2.8", "2.5"),
        // Vegetables
        ExtraFood("Carrot", "Vegetables", "41", "0.9", "10", "0.2", "2.8", "4.7"),
        ExtraFood("Spinach", "Vegetables", "23", "2.9", "3.6", "0.4", "2.2", "0.4"),
        ExtraFood("Bell Pepper", "Vegetables", "31", "1", "6", "0.3", "2.1", "4.2"),
        ExtraFood("Onion", "Vegetables", "40", "1.1", "9", "0.1", "1.7", "4.2"),
        ExtraFood("Mushroom", "Vegetables", "22", "3.1", "3.3", "0.3", "1", "2"),
        ExtraFood("Cauliflower", "Vegetables", "25", "1.9", "5", "0.3", "2", "1.9"),
        ExtraFood("Corn", "Vegetables", "86", "3.2", "19", "1.4", "2.7", "6.3"),
        // Grains
        ExtraFood("White Rice", "Grains", "130", "2.7", "28", "0.3", "0.4", "0.1"),
        ExtraFood("Quinoa", "Grains", "120", "4.4", "21", "1.9", "2.8", "0.9"),
        ExtraFood("Pasta", "Grains", "131", "5", "25", "1.1", "1.8", "0.6"),
        ExtraFood("Bagel", "Grains", "245", "10", "48", "1.5", "2", "5"),
        // Meat / fish
        ExtraFood("Beef (lean ground)", "Meat", "250", "26", "0", "15", "0", "0"),
        ExtraFood("Pork (lean)", "Meat", "143", "21", "0", "6", "0", "0"),
        ExtraFood("Turkey Breast", "Meat", "135", "30", "0", "1", "0", "0"),
        ExtraFood("Tuna (canned)", "Fish", "132", "28", "0", "1", "0", "0"),
        ExtraFood("Shrimp", "Fish", "99", "24", "0.2", "0.3", "0", "0"),
        // Legumes
        ExtraFood("Black Beans", "Legumes", "132", "8.9", "23.7", "0.5", "8.7", "0.3"),
        ExtraFood("Chickpeas", "Legumes", "164", "8.9", "27", "2.6", "7.6", "4.8"),
        // Dairy
        ExtraFood("Milk (2%)", "Dairy", "50", "3.3", "5", "2", "0", "5"),
        ExtraFood("Cheddar Cheese", "Dairy", "402", "25", "1.3", "33", "0", "0.5"),
        ExtraFood("Cottage Cheese", "Dairy", "98", "11", "3.4", "4.3", "0", "2.7"),
        // Snacks
        ExtraFood("Peanut Butter", "Snacks", "588", "25", "20", "50", "6", "9"),
        ExtraFood("Hummus", "Snacks", "166", "8", "14", "10", "6", "0.3"),
        ExtraFood("Dark Chocolate", "Snacks", "598", "7.8", "46", "43", "11", "24"),
        ExtraFood("Popcorn", "Snacks", "387", "12", "78", "4.5", "14.5", "0.9"),
        // Beverages
        ExtraFood("Coffee (black)", "Beverages", "1", "0.1", "0", "0", "0", "0"),
        ExtraFood("Orange Juice", "Beverages", "45", "0.7", "10", "0.2", "0.2", "8.4"),
        ExtraFood("Smoothie (fruit)", "Beverages", "82", "1.5", "19", "0.5", "1.8", "14")
    )

    fun backfillExtraFoods() {
        transaction {
            val now = LocalDateTime.now()
            for (f in extraFoods) {
                val exists = FoodItems.selectAll().where { FoodItems.name eq f.name }.count() > 0L
                if (exists) continue
                FoodItems.insert {
                    it[name] = f.name; it[category] = f.cat
                    it[caloriesPer100g] = BigDecimal(f.cal)
                    it[proteinPer100g] = BigDecimal(f.prot)
                    it[carbsPer100g] = BigDecimal(f.carb)
                    it[fatPer100g] = BigDecimal(f.fat)
                    it[fiberPer100g] = f.fiber?.let { v -> BigDecimal(v) }
                    it[sugarPer100g] = f.sugar?.let { v -> BigDecimal(v) }
                    it[createdAt] = now
                }
            }
        }
    }

    fun backfillExtraRecipes() {
        transaction {
            val sarah = Users.selectAll().where { Users.email eq "sarah@clinic.com" }.singleOrNull() ?: return@transaction
            val sarahId = sarah[Users.id]
            val foodMap = FoodItems.selectAll().associate { it[FoodItems.name] to it[FoodItems.id] }
            val now = LocalDateTime.now()

            for (extra in extraRecipes) {
                val exists = Recipes.selectAll().where { Recipes.title eq extra.title }.count() > 0L
                if (exists) continue

                val rid = Recipes.insert {
                    it[createdBy] = sarahId
                    it[title] = extra.title
                    it[description] = extra.description
                    it[prepTimeMinutes] = extra.prepMin
                    it[cookTimeMinutes] = extra.cookMin
                    it[servings] = extra.servings
                    it[difficulty] = extra.difficulty
                    it[imageUrl] = extra.imageUrl
                    it[createdAt] = now
                } get Recipes.id

                for (ing in extra.ingredients) {
                    RecipeIngredients.insert {
                        it[recipeId] = rid
                        it[foodItemId] = foodMap[ing.foodName]
                        it[ingredientName] = ing.foodName
                        it[quantity] = ing.quantity
                        it[unit] = ing.unit
                    }
                }

                for ((idx, step) in extra.steps.withIndex()) {
                    RecipeSteps.insert {
                        it[recipeId] = rid
                        it[stepNumber] = idx + 1
                        it[instruction] = step
                    }
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
