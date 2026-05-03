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

### Run the application
```bash
./gradlew run
```
Then open http://localhost:8080 in your browser.

### Test Accounts
| Role | Email | Password |
|------|-------|----------|
| Subscriber | alice@email.com | password |
| Subscriber | bob@email.com | password |
| Professional | sarah@clinic.com | password |

## Features

| Module | Description |
|--------|-------------|
| **Authentication** | Login / Register / Logout, BCrypt passwords, Session management, Role-based routing |
| **Dashboard** | Daily nutrition summary, calorie/protein/carbs/fat progress bars, meal overview |
| **Food Diary** | Date navigation, add/delete food entries, AJAX food search, daily nutrition summary |
| **Nutritional Goals** | Set/update daily targets, weekly calorie progress chart |
| **Recipes** | Search & filter by difficulty, recipe detail (ingredients/steps/nutrition), favourite, rate & review |
| **Messages** | Conversation list, send/receive messages, unread count badge |
| **Professional Panel** | Client list with compliance stats, view client diary (read-only), send advice |

## Database & class design

- [ER_diagram.md](ER_diagram.md) — entity-relationship diagram for the data layer.
- [CLASS_diagram.md](CLASS_diagram.md) — application-layer class diagram (services, routes, table objects).

**12 Tables:** users, professional_profiles, client_relationships, food_items, food_diary_entries, nutritional_goals, advice_messages, recipes, recipe_ingredients, recipe_steps, recipe_favourites, recipe_ratings

The database is auto-created on startup with seed data (sample users, foods, recipes, diary entries, messages).

## UI design

The original static design pass — annotated wireframes and screen-by-screen rationale — lives in [`UI_wireframes.md`](UI_wireframes.md). The design tokens, dark-mode theme and component states landed iteratively (see CHANGELOG `v0.4.0` onward).

## Accessibility

Per-page WCAG 2.1 AA self-audit, structural a11y patterns, and known gaps are documented in [`ACCESSIBILITY.md`](ACCESSIBILITY.md).

## Requirements

The prioritised user-story backlog (with MoSCoW priority and acceptance-criteria IDs that map into the test suite) is in [`USER_STORIES.md`](USER_STORIES.md).

## Usability testing

The Round-1 moderated walkthrough findings, severity ratings and which findings will be addressed before the demo are in [`UX_TESTING.md`](UX_TESTING.md).

## Design decisions

A short rationale for every significant design or architecture choice (with alternatives considered) is in [`DESIGN_DECISIONS.md`](DESIGN_DECISIONS.md).

## Beyond the basic spec

The brief asks for a working web app that meets the system specification. We delivered that and a few things on top:

- **Dark mode** — auto-follows `prefers-color-scheme` plus a manual toggle persisted to `localStorage`. ~30 semantic colour tokens so light and dark share component CSS. (See `DESIGN_DECISIONS.md` D-4.)
- **Mobile drawer** — below 840 px the sidebar slides in from the left with a backdrop, hamburger toggle, Escape-to-close. (D-5.)
- **IDOR-safe authorisation pattern** — the professional-only routes share a `hasActiveRelationship(...)` gate so a professional cannot read or message a subscriber they do not supervise. (D-6.)
- **Cookie hardening** — `HttpOnly` + `SameSite=Lax` on the session cookie for defence-in-depth against XSS theft and form-based CSRF. (D-7.)
- **Continuous integration** — every push and every PR runs `./gradlew build test` plus Detekt static analysis, with HTML reports uploaded as artifacts.
- **Devcontainer** — `.devcontainer/devcontainer.json` pins JDK 17 and forwards port 8080 so a marker can clone, open in Codespaces, and run with no manual setup.
- **AI usage transparency** — `AI_USAGE.md` is the canonical log of every AI-assisted contribution. The amber-rating brief permits AI use *with* acknowledgment; we treat AI as a tool, not a contributor. (D-9.)
- **Documented design evolution** — `DESIGN_DECISIONS.md`, `USER_STORIES.md`, `UX_TESTING.md`, `ACCESSIBILITY.md` and `CLASS_diagram.md` extend the standard `ER_diagram.md` so a marker can audit *why* the system looks the way it does, not only *how*.

## Generative AI usage

Per the COMP2850 brief (amber rating for generative AI), AI-assisted contributions are logged in [`AI_USAGE.md`](AI_USAGE.md). All AI-assisted code was reviewed and tested by a human team member before merging into `main`.

## Team

COMP2850 Group Project — University of Leeds, 2026
