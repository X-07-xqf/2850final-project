# Changelog - Good Food & Healthy Eating

All notable changes to this project will be documented in this file.

---

## [v0.4.0] - 2026-04-29 — UI Upgrade: Visual refresh, dark mode, component polish

### Added
- **Dark mode** with three modes — auto (follows `prefers-color-scheme`), manual light, manual dark
  - Theme toggle button in every sidebar (and floating button on the login page)
  - User selection persisted via `localStorage` (`gf-theme` key)
  - Inline pre-`DOMContentLoaded` script applies saved theme synchronously to prevent flash-of-wrong-theme
  - Full color-token reorganization (~30 semantic tokens: surface, surface-alt, text, text-strong, muted, border, primary, accent, danger, sidebar layers, chat layers, table layers, etc.)
- **Mobile drawer sidebar** below 840 px — fixed-position slide-in drawer with backdrop, replacing the previous squashed top bar
  - Hamburger toggle button (top-left), backdrop click, Escape key, and nav-link click all close the drawer
- **Component polish**
  - Card hover lift (recipe cards, conversations, table rows)
  - Animated progress bars (smooth width transition + subtle shimmer)
  - Modal scale-in + backdrop blur
  - Toast region (`.toast-region` / `.toast` / `.toast--warn` / `.toast--danger`)
  - Skeleton loader (`.skeleton`)
  - Chat bubble entrance animation
  - Star rating hover scale
- **Accessibility**
  - `:focus-visible` ring on all interactive elements (replaces the previous 2px outline)
  - `prefers-reduced-motion` reduces all animation durations to ~0
  - Print stylesheet hides chrome (sidebar, toggles, toasts) and flattens shadows

### Changed
- Color contrast bumped — `--color-text-strong` introduced for headings, primary button gains gradient + soft shadow, focus ring uses accent color
- Typography polish — added `letter-spacing` micro-adjustments to titles, font-smoothing on body, slightly larger page-title (1.65 → 1.75 rem)
- Auth background switched to dual radial-gradient
- Modal panel scales in instead of fading; backdrop has 4 px backdrop blur
- Sidebar active state uses inset accent border (3 px) in addition to bg color
- Tablet breakpoint added (1024 px) — recipe grid retightens, main padding compacts
- All transitions use a shared `cubic-bezier(0.4, 0, 0.2, 1)` ease and three duration tokens (`--dur-fast` / `--dur` / `--dur-slow`)

### Notes
- Backend, routes, services, models, and seed data are **untouched** — this release is CSS + JS + template chrome only.
- No new dependencies; uses native CSS features (`color-mix`, custom properties, `prefers-color-scheme`).

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
