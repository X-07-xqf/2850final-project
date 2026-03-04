# Good Food & Healthy Eating — ER Diagram

## Entity Relationship Diagram (Mermaid)

```mermaid
erDiagram
    users {
        int id PK
        varchar full_name "NN"
        varchar email "UK, NN"
        varchar password_hash "NN"
        varchar role "NN (subscriber / professional)"
        varchar avatar_url
        datetime created_at "NN"
        datetime updated_at
    }

    professional_profiles {
        int id PK
        int user_id FK "NN, UK"
        varchar specialty "NN"
        varchar qualification "NN"
        text bio
        datetime created_at "NN"
    }

    client_relationships {
        int id PK
        int professional_id FK "NN"
        int subscriber_id FK "NN"
        varchar status "NN (active / ended)"
        datetime started_at "NN"
        datetime ended_at
    }

    food_items {
        int id PK
        varchar name "NN"
        varchar category "NN"
        decimal calories_per_100g "NN"
        decimal protein_per_100g "NN"
        decimal carbs_per_100g "NN"
        decimal fat_per_100g "NN"
        decimal fiber_per_100g
        decimal sugar_per_100g
        datetime created_at "NN"
    }

    food_diary_entries {
        int id PK
        int user_id FK "NN"
        int food_item_id FK "NN"
        varchar meal_type "NN (breakfast / lunch / dinner / snack)"
        decimal quantity_grams "NN"
        date entry_date "NN"
        time entry_time
        text notes
        datetime created_at "NN"
    }

    nutritional_goals {
        int id PK
        int user_id FK "NN"
        decimal daily_calories
        decimal daily_protein
        decimal daily_carbs
        decimal daily_fat
        decimal daily_fiber
        datetime set_at "NN"
        datetime updated_at
    }

    advice_messages {
        int id PK
        int professional_id FK "NN"
        int subscriber_id FK "NN"
        text message "NN"
        boolean is_read "NN, default false"
        datetime sent_at "NN"
    }

    recipes {
        int id PK
        int created_by FK "NN"
        varchar title "NN"
        text description
        int prep_time_minutes "NN"
        int cook_time_minutes "NN"
        int servings "NN"
        varchar difficulty "NN (easy / medium / hard)"
        varchar image_url
        datetime created_at "NN"
        datetime updated_at
    }

    recipe_ingredients {
        int id PK
        int recipe_id FK "NN"
        int food_item_id FK
        varchar ingredient_name "NN"
        varchar quantity "NN"
        varchar unit "NN"
    }

    recipe_steps {
        int id PK
        int recipe_id FK "NN"
        int step_number "NN"
        text instruction "NN"
    }

    recipe_favourites {
        int id PK
        int user_id FK "NN"
        int recipe_id FK "NN"
        datetime created_at "NN"
    }

    recipe_ratings {
        int id PK
        int user_id FK "NN"
        int recipe_id FK "NN"
        int rating "NN (1-5)"
        text comment
        datetime created_at "NN"
        datetime updated_at
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
