# Sage — Healthy Eating

COMP2850 Software Engineering — Group Project

A full-stack web application that supports diet monitoring, nutritional advice, and home cooking.

## Tech Stack

- **Backend**: Ktor 2.3.7 (Kotlin) + Netty
- **Database**: H2 (embedded, MySQL-compatible) + Exposed ORM
- **Templating**: Thymeleaf (server-side rendering)
- **Auth**: Ktor Sessions + BCrypt
- **Build**: Gradle 8.5 (Kotlin DSL)
- **Java**: JDK 17+

## Project Structure

```
├── build.gradle.kts               # Gradle build config
├── settings.gradle.kts
├── src/main/
│   ├── kotlin/com/goodfood/
│   │   ├── Application.kt         # Ktor entry point
│   │   ├── plugins/               # Database, Security, Routing, Templating
│   │   ├── models/                # 12 Exposed table definitions + SeedData
│   │   ├── routes/                # Auth, Dashboard, Diary, Recipe, Goal, Message, Profile, Professional
│   │   └── services/              # UserService, DiaryService, RecipeService, GoalService, MessageService
│   └── resources/
│       ├── application.conf       # Ktor config (port, database)
│       ├── templates/             # Thymeleaf HTML templates
│       └── static/                # CSS + JS
├── ER_diagram.md                  # Database ER diagram (Mermaid)
├── UI_wireframes.md               # UI wireframe descriptions
└── ui-prototype/                  # Static HTML/CSS prototype
```

## Quick Start

### Prerequisites
- JDK 17 or higher

### Test Accounts
| Role | Email | Password |
|------|-------|----------|
| Subscriber | alice@email.com | password |
| Subscriber | bob@email.com | password |
| Professional | sarah@clinic.com | password |

