# Changelog - Good Food & Healthy Eating

All notable changes to this project will be documented in this file.

---

## [v0.3.0] - 2026-03-24 — Refactor: Restructure Kotlin by Feature Module

### Changed
- Project structure refactored from layer-based (models/, plugins/, routes/, services/) to feature-based modules
- Each module folder now contains its own table definitions, routes, and service logic together
- Updated all package declarations and import paths

### New Directory Layout
```
src/main/kotlin/com/goodfood/
├── Application.kt           # Entry point
├── config/                  # Database, Routing, Security, Templating
├── auth/                    # Users, AuthRoutes, UserService
├── diary/                   # FoodItems, DiaryEntries, NutritionalGoals, DiaryService, DashboardRoutes
├── goals/                   # GoalRoutes, GoalService
├── recipes/                 # Recipes, Ingredients, Steps, Ratings, Favourites, RecipeService
├── messages/                # AdviceMessages, MessageRoutes, MessageService
├── professional/            # Profiles, ClientRelationships, ProfessionalRoutes
├── profile/                 # ProfileRoutes
└── seed/                    # SeedData
```

---

## [v0.2.0] - 2026-03-24 — Add Ktor Full-Stack Web Application

### Added
- Complete Kotlin/Ktor backend with all web functionalities
- H2 embedded database with Exposed ORM (12 tables)
- Thymeleaf server-side rendering with 11 HTML templates
- User authentication with BCrypt password hashing
- Subscriber features: Dashboard, Food Diary, Recipes, Goals, Messages, Profile
- Professional features: Client management, messaging
- Seed data with test accounts (alice@email.com, bob@email.com, sarah@clinic.com / password: password)

---

## [v0.1.0] - 2026-03-24 — Initial Commit: Design Phase

### Added
- ER diagram design
- UI wireframes
- Interactive HTML/CSS prototype
