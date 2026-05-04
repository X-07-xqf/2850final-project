package com.goodfood.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Render a [BigDecimal] the way a UI should: integers stay integers
 * (`2000`, not `2000.00`), and fractions are capped at [maxDecimals]
 * with trailing zeros stripped (`99.9`, not `99.90`; `100`, not `100.0`).
 *
 * The DB columns are declared as `NUMERIC(_, 2)` and round-trip with that
 * scale baked in, so without this `Thymeleaf` ends up calling `toString()`
 * on the raw BigDecimal and the user sees the storage scale, not the
 * intended display precision.
 */
fun BigDecimal.fmt(maxDecimals: Int = 1): String {
    val rounded = this.setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros()
    // stripTrailingZeros on values like "100.00" gives a negative scale, which
    // toString() renders as scientific notation ("1E+2") — toPlainString avoids that.
    return rounded.toPlainString()
}

private val timeOfDay = DateTimeFormatter.ofPattern("h:mm a")
private val monthDayTime = DateTimeFormatter.ofPattern("MMM d, h:mm a")
private val fullDateTime = DateTimeFormatter.ofPattern("MMM d yyyy, h:mm a")

/**
 * Humanize a chat-message timestamp:
 *   - same day  → `Today 4:24 PM`
 *   - yesterday → `Yesterday 4:24 PM`
 *   - this year → `Apr 30, 4:24 PM`
 *   - older     → `Apr 30 2025, 4:24 PM`
 *
 * Replaces the raw `LocalDateTime.toString()` form (`2026-04-30T16:24:08.749909`)
 * we used to leak straight into chat bubbles.
 */
fun LocalDateTime.fmtChatTime(now: LocalDateTime = LocalDateTime.now()): String {
    val today = now.toLocalDate()
    val msgDate = this.toLocalDate()
    return when {
        msgDate == today -> "Today ${this.format(timeOfDay)}"
        msgDate == today.minusDays(1) -> "Yesterday ${this.format(timeOfDay)}"
        msgDate.year == today.year -> this.format(monthDayTime)
        else -> this.format(fullDateTime)
    }
}

/**
 * Pick a cover emoji for a recipe card from a keyword scan over its title.
 * Falls through to a generic plate when nothing matches. Cheap stand-in for
 * a real image pipeline — keeps the recipe grid from looking text-only.
 */
fun recipeCoverEmoji(title: String): String {
    val t = title.lowercase()
    return when {
        "salad" in t -> "🥗"
        "salmon" in t || "fish" in t || "tuna" in t -> "🐟"
        "chicken" in t || "turkey" in t -> "🍗"
        "beef" in t || "steak" in t || "burger" in t -> "🥩"
        "egg" in t || "omelette" in t -> "🥚"
        "rice" in t || "risotto" in t -> "🍚"
        "noodle" in t || "pasta" in t || "spaghetti" in t -> "🍝"
        "soup" in t || "stew" in t -> "🥣"
        "bowl" in t || "oats" in t || "oatmeal" in t || "porridge" in t -> "🥣"
        "smoothie" in t || "juice" in t || "tea" in t -> "🥤"
        "fruit" in t || "berry" in t || "apple" in t || "banana" in t -> "🍎"
        "bread" in t || "toast" in t || "sandwich" in t || "wrap" in t -> "🥪"
        "pizza" in t -> "🍕"
        "veggie" in t || "vegetable" in t || "broccoli" in t -> "🥦"
        else -> "🍽️"
    }
}

/**
 * Pick a stable cover tone (`sage` / `oat` / `clay` / `berry`) for a recipe
 * card so the grid has visual variety without random reshuffles between
 * page loads. The hash is masked to a 16-bit window so it stays positive
 * across JVMs.
 */
fun recipeCoverTone(title: String): String {
    val tones = listOf("sage", "oat", "clay", "berry")
    val idx = ((title.hashCode().toLong() and 0xFFFF) % tones.size).toInt()
    return tones[idx]
}

/**
 * Pick a food emoji for a food-item card from a keyword scan over its name.
 * Used by the diary "Add food" picker so each card has a recognisable
 * visual without needing real photographs. Falls through to a generic
 * plate when nothing matches.
 */
fun foodEmoji(name: String): String {
    val n = name.lowercase()
    return when {
        "apple" in n -> "🍎"
        "banana" in n -> "🍌"
        "orange" in n -> "🍊"
        "grape" in n -> "🍇"
        "strawberr" in n -> "🍓"
        "blueberr" in n -> "🫐"
        "watermelon" in n -> "🍉"
        "pineapple" in n -> "🍍"
        "mango" in n -> "🥭"
        "lemon" in n -> "🍋"
        "avocado" in n -> "🥑"
        "tomato" in n -> "🍅"
        "carrot" in n -> "🥕"
        "broccoli" in n -> "🥦"
        "cauliflower" in n -> "🥦"
        "cucumber" in n -> "🥒"
        "spinach" in n || "salad green" in n || "lettuce" in n -> "🥬"
        "sweet potato" in n -> "🍠"
        "potato" in n -> "🥔"
        "onion" in n -> "🧅"
        "garlic" in n -> "🧄"
        "pepper" in n -> "🫑"
        "corn" in n -> "🌽"
        "mushroom" in n -> "🍄"
        "rice" in n -> "🍚"
        "bagel" in n -> "🥯"
        "bread" in n || "toast" in n -> "🍞"
        "pasta" in n || "noodle" in n || "spaghetti" in n -> "🍝"
        "oat" in n || "oatmeal" in n -> "🥣"
        "quinoa" in n -> "🌾"
        "egg" in n -> "🥚"
        "milk" in n -> "🥛"
        "cheese" in n || "cheddar" in n || "cottage" in n -> "🧀"
        "butter" in n && "peanut" !in n -> "🧈"
        "yogurt" in n -> "🍦"
        "chicken" in n || "turkey" in n -> "🍗"
        "beef" in n || "steak" in n || "burger" in n -> "🥩"
        "pork" in n || "bacon" in n || "ham" in n -> "🥓"
        "salmon" in n || "tuna" in n || "fish" in n -> "🐟"
        "shrimp" in n || "prawn" in n -> "🦐"
        "tofu" in n || "tempeh" in n -> "🍢"
        "lentil" in n || "chickpea" in n || "bean" in n -> "🫘"
        "almond" in n || "nut" in n || "peanut" in n -> "🥜"
        "chia" in n || "seed" in n -> "🌱"
        "olive" in n || "oil" in n -> "🫒"
        "honey" in n -> "🍯"
        "chocolate" in n -> "🍫"
        "popcorn" in n -> "🍿"
        "hummus" in n -> "🥣"
        "tea" in n -> "🍵"
        "coffee" in n -> "☕"
        "smoothie" in n || "juice" in n -> "🧃"
        "water" in n -> "💧"
        else -> "🍽️"
    }
}

/**
 * Map a food category to one of the four brand cover tones used by recipe
 * cards (sage / oat / clay / berry). Tones live in CSS as
 * `.recipe-card__cover--<tone>` with light/dark gradients via existing
 * brand tokens, so the food picker reuses them for free.
 */
fun foodTone(category: String): String = when (category.lowercase()) {
    "fruits" -> "berry"
    "vegetables" -> "sage"
    "grains" -> "oat"
    "meat", "fish" -> "clay"
    "legumes" -> "sage"
    "dairy" -> "oat"
    "nuts", "seeds" -> "clay"
    "oils" -> "sage"
    "snacks" -> "berry"
    "beverages" -> "sage"
    else -> "sage"
}
