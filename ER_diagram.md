# Good Food & Healthy Eating — ER Diagram

## Entity Relationship Diagram (Mermaid)

```mermaid
erDiagram
    subscriber {
        firstName varchar
        lastName varchar
        userID int
        email varchar 
        password_hash varchar
        dob datetime
        favRecipes recipe[]
        created_at datetime
        updated_at datetime
    }

    professional {
        firstName varchar
        lastName varchar
        userID int
        email varchar 
        password_hash varchar
        dob datetime
        created_at datetime
        updated_at datetime
        specialty varchar
        bio string
    }


    foodItem {
        name varchar
        itemID int
        category varchar
        calories_per_100g float
        protein_per_100g float
        carbs_per_100g float
        fat_per_100g float
        fiber_per_100g float
        sugar_per_100g float
        sodium_per_100g float
        cholesterol_per_100g float
        created_at datetime 
    }

    diaryEntry {
        item foodItem
        mealType varchar 
        quantityGrams int
        entryDate datetime
        notes varchar
        created_at datetime 
    }

    nutritionalGoals {
        userID userID
        dailyCals int
        dailyProtein int
        dailyCarbs varchar
        dailyFat varchar
        dailyFiber varchar
        set_at datetime
    }

    weightGoal {
        userID userID
        goalWeight varchar
        startWeight varchar
        currentWeight varchar

    }



    recipe {
        recipeID int
        ingredients foodItem[]
        createdBy varchar
        title varchar
        description varchar
        prepTime varchar
        cookTime varchar
        totalTime varchar
        servings int
        rating varchar
        difficulty varchar
        image_url varchar 
        created_at datetime 
        updated_at datetime 
    }



    users ||--o| professional_profiles : "has profile"
    users ||--o{ food_diary_entries : "records"
    users ||--o| nutritional_goals : "sets"
    users ||--o{ recipe_favourites : "favourites"
    users ||--o{ recipe_ratings : "rates"
    users ||--o{ recipes : "creates"

    professional_profiles ||--o{ client_relationships : "manages"
    users ||--o{ client_relationships : "subscribed to"

    users ||--o{ advice_messages : "sends (professional)"
    users ||--o{ advice_messages : "receives (subscriber)"

    food_items ||--o{ food_diary_entries : "logged in"
    food_items ||--o{ recipe_ingredients : "used in"

    recipes ||--o{ recipe_ingredients : "contains"
    recipes ||--o{ recipe_steps : "has steps"
    recipes ||--o{ recipe_favourites : "favourited by"
    recipes ||--o{ recipe_ratings : "rated by"
```

## Table Descriptions

| Table | Description |
|-------|-------------|
| **users** | All users (subscribers and health professionals). `role` distinguishes user type. |
| **professional_profiles** | Extended profile for health professionals (specialty, qualifications). |
| **client_relationships** | Links professionals to their subscriber clients. |
| **food_items** | Food database with nutritional information per 100g. |
| **food_diary_entries** | Daily food intake log — the "food diary" feature. |
| **nutritional_goals** | Personalised nutritional targets set by/for a subscriber. |
| **advice_messages** | Messages from professionals to their clients. |
| **recipes** | Home cooking recipes with metadata. |
| **recipe_ingredients** | Ingredients for each recipe, optionally linked to food_items. |
| **recipe_steps** | Step-by-step cooking instructions. |
| **recipe_favourites** | Subscribers' saved/favourite recipes. |
| **recipe_ratings** | Ratings and comments on recipes. |

## Key Relationships

- A **user** can be a subscriber or a professional (role-based).
- A **professional** manages multiple subscribers via `client_relationships`.
- Subscribers record daily food intake in `food_diary_entries`, referencing `food_items`.
- Professionals send `advice_messages` to their subscribers.
- Anyone can create **recipes**; subscribers can **favourite** and **rate** them.
- `recipe_ingredients` optionally links to `food_items` for nutritional calculations.
