# Class Diagram — Good Food & Healthy Eating

This diagram complements [`ER_diagram.md`](ER_diagram.md). The ER diagram covers the database layer; this one covers the application layer — Ktor route extension functions, the service layer that owns business logic, and the Exposed `Table` objects that map onto database tables.

The codebase is organised by **feature module** (each module folder contains its own table objects, routes, and service), so the diagram is grouped that way.

## Application-layer class diagram

```mermaid
classDiagram
    direction LR

    %% --- core platform / config ---
    class Application {
        +main()
        +configureSecurity()
        +configureDatabase()
        +configureRouting()
        +configureTemplating()
    }
    class UserSession {
        +Int userId
        +String fullName
        +String email
        +String role
        +String initials
    }

    %% --- auth feature ---
    class Users {
        <<Exposed Table>>
        +id: int
        +email: varchar
        +passwordHash: varchar
        +fullName: varchar
        +role: varchar
    }
    class UserService {
        <<object>>
        +register(name, email, password, role) Int?
        +authenticate(email, password) Map?
        +getById(id) Map?
    }
    class AuthRoutes {
        <<route extension>>
        +authRoutes()
    }

    %% --- diary feature ---
    class FoodItems {
        <<Exposed Table>>
    }
    class FoodDiaryEntries {
        <<Exposed Table>>
    }
    class NutritionalGoals {
        <<Exposed Table>>
    }
    class DiaryService {
        <<object>>
        +getEntriesForDate(userId, date) List
        +getDailySummary(userId, date) Map
        +getWeeklySummary(userId) List
        +addEntry(...)
        +deleteEntry(id, userId)
        +searchFood(query) List
    }
    class DiaryRoutes {
        <<route extension>>
        +diaryRoutes()
    }
    class DashboardRoutes {
        <<route extension>>
        +dashboardRoutes()
    }

    %% --- goals feature ---
    class GoalService {
        <<object>>
        +saveGoals(userId, cal, prot, carb, fat, fiber)
        +getGoals(userId) Map?
    }
    class GoalRoutes {
        <<route extension>>
        +goalRoutes()
    }

    %% --- recipes feature ---
    class Recipes {
        <<Exposed Table>>
    }
    class RecipeIngredients {
        <<Exposed Table>>
    }
    class RecipeSteps {
        <<Exposed Table>>
    }
    class RecipeRatings {
        <<Exposed Table>>
    }
    class RecipeFavourites {
        <<Exposed Table>>
    }
    class RecipeService {
        <<object>>
        +searchRecipes(query, difficulty) List
        +getRecipeDetail(id) Map?
        +isFavourite(userId, recipeId) Bool
        +toggleFavourite(userId, recipeId) Bool
        +addRating(userId, recipeId, rating, comment)
        +getUserFavourites(userId) List
    }
    class RecipeRoutes {
        <<route extension>>
        +recipeRoutes()
    }

    %% --- messages feature ---
    class AdviceMessages {
        <<Exposed Table>>
    }
    class MessageService {
        <<object>>
        +sendMessage(senderId, receiverId, body)
        +getConversation(userA, userB) List
        +getUnreadCount(userId) Int
        +listConversations(userId) List
    }
    class MessageRoutes {
        <<route extension>>
        +messageRoutes()
    }

    %% --- professional feature ---
    class ProfessionalProfiles {
        <<Exposed Table>>
    }
    class ClientRelationships {
        <<Exposed Table>>
    }
    class ProfessionalRoutes {
        <<route extension>>
        +professionalRoutes()
        -hasActiveRelationship(proId, subId) Bool
    }

    %% --- profile feature ---
    class ProfileRoutes {
        <<route extension>>
        +profileRoutes()
    }

    %% --- seed ---
    class SeedData {
        <<object>>
        +seedIfEmpty()
    }

    %% --- relationships ---
    Application --> UserSession : reads from cookie
    Application --> AuthRoutes
    Application --> DiaryRoutes
    Application --> DashboardRoutes
    Application --> GoalRoutes
    Application --> RecipeRoutes
    Application --> MessageRoutes
    Application --> ProfessionalRoutes
    Application --> ProfileRoutes
    Application --> SeedData

    AuthRoutes --> UserService
    UserService --> Users

    DiaryRoutes --> DiaryService
    DashboardRoutes --> DiaryService
    DashboardRoutes --> GoalService
    DiaryService --> FoodDiaryEntries
    DiaryService --> FoodItems

    GoalRoutes --> GoalService
    GoalService --> NutritionalGoals

    RecipeRoutes --> RecipeService
    RecipeService --> Recipes
    RecipeService --> RecipeIngredients
    RecipeService --> RecipeSteps
    RecipeService --> RecipeRatings
    RecipeService --> RecipeFavourites
    RecipeService --> FoodItems

    MessageRoutes --> MessageService
    MessageService --> AdviceMessages

    ProfessionalRoutes --> DiaryService
    ProfessionalRoutes --> GoalService
    ProfessionalRoutes --> MessageService
    ProfessionalRoutes --> ClientRelationships
    ProfessionalRoutes --> Users

    ProfileRoutes --> RecipeService
    ProfileRoutes --> Users

    SeedData --> Users
    SeedData --> FoodItems
    SeedData --> Recipes
    SeedData --> NutritionalGoals
    SeedData --> ClientRelationships
    SeedData --> AdviceMessages
```

## Layering rules

1. **Routes** depend on **services**; never on table objects directly.
2. **Services** depend on **table objects** (Exposed DSL); never on routes.
3. **Table objects** are pure schema definitions and depend on nothing in the codebase except other table objects (for foreign keys).
4. `Application` wires everything together but contains no business logic.
5. `SeedData` is allowed to write through table objects directly because it bootstraps the DB before services run.

## Cross-cutting

- All routes read `UserSession` from the cookie (`config/Security.kt`) for authentication and role-based authorisation.
- `ProfessionalRoutes` additionally calls `hasActiveRelationship()` on `ClientRelationships` to enforce the IDOR-safe authorisation pattern (added in v0.4.4).

## How to view this file

GitHub renders Mermaid diagrams natively — open this file directly in the browser.
