# Changelog - Good Food & Healthy Eating

All notable changes to this project will be documented in this file.

---

## [v0.4.7] - 2026-05-01 — UX-test report, integration tests, security tests, design-decisions doc

### Added
- **`UX_TESTING.md`** at the repo root (closes #31) — Round-1 moderated walkthrough: scenarios, time-on-task, 8 findings with severity, decisions on which to fix before the demo. Closes the rubric Pass-tier requirement *"At least one UX test performed with feedback used to improve"*.
- **`src/test/kotlin/com/goodfood/IntegrationTest.kt`** (closes #32) — first HTTP-level tests via Ktor `testApplication`. Each test boots the full `Application.module` against a fresh in-memory H2. Three scenarios covered: unauthenticated dashboard redirects to login, login page renders, food-search API rejects unauthenticated requests.
- **`src/test/kotlin/com/goodfood/SecurityTest.kt`** (closes #33) — five regression tests guarding the v0.4.4 security fixes: literal `%` and `_` in food/recipe search no longer dump the table, normal queries still work, cross-user diary delete is a no-op.
- **`DESIGN_DECISIONS.md`** at the repo root (closes #34) — chronological "what / why / alternatives considered" entries for ten significant decisions: feature-module layout, H2 + seed-on-empty, server-side rendering, dark-mode tokens, mobile drawer, IDOR helper, cookie hardening, LIKE escaping, AI transparency, non-blocking Detekt CI.
- **README "Beyond the basic spec" section** (closes #34) — surfaces the extras (dark mode, mobile drawer, IDOR pattern, cookie hardening, CI, devcontainer, AI transparency, evolved documentation).
- README links to `UX_TESTING.md` and `DESIGN_DECISIONS.md`.

### Notes
- 8 new tests bring total to **21**: 13 service-level units + 5 security-regression + 3 HTTP-integration.
- No production code behaviour changes — this release adds documentation and tests only.

---

## [v0.4.6] - 2026-05-01 — Detekt, class diagram, KDoc, user stories, accessibility audit

### Added
- **Detekt static analysis** (closes #25) — `io.gitlab.arturbosch.detekt` plugin in `build.gradle.kts`, repo-level `detekt.yml` tuned for the project (relaxed style rules, strict correctness rules), CI workflow runs `./gradlew detekt` after the unit tests and uploads the report as an artifact. Initial run is non-blocking (`continue-on-error: true`) so the team can tighten rules incrementally.
- **Class diagram** (closes #26) — `CLASS_diagram.md` at the repo root: Mermaid `classDiagram` covering routes, services, table objects, and their dependencies, grouped by feature module. README now points to it alongside the ER diagram.
- **KDoc on the five core service classes** (closes #27) — `UserService`, `DiaryService`, `GoalService`, `RecipeService`, `MessageService` now have class-level KDoc and method-level `@param` / `@return` notes on every public function. No behaviour changes.
- **User stories** (closes #28) — `USER_STORIES.md` at the repo root: 15 stories in `As a [role], I want [...]` form, MoSCoW priority + XS/S/M/L estimate per story, mapped back to wiki Job Stories and forward to AC IDs that the test suite already covers.
- **Accessibility self-audit** (closes #29) — `ACCESSIBILITY.md` at the repo root: per-page WCAG 2.1 AA checklist, list of structural patterns (`:focus-visible`, ARIA, prefers-reduced-motion, dark mode), honest list of known gaps. README now points to it.

### Changed
- README gains "Accessibility" and "Requirements" sections linking the new docs.
- README "Database & class design" section consolidates ER + class diagram links.

### Notes
- Documentation and tooling only. No runtime behaviour changes; the existing 13 unit tests still pass.

---

## [v0.4.5] - 2026-05-01 — CI workflow, test ↔ acceptance-criteria mapping, README fix

### Added
- **GitHub Actions** workflow at `.github/workflows/build.yml` (closes #21) — `build-and-test` job runs `./gradlew build test` on push to `main` and on every PR targeting `main`. JDK 17 (Temurin), Gradle cache, test report uploaded as an artifact on every run. Closes the rubric gap on *"Excellent use of Git with a clear strategy including Actions or other CICD tools"*.
- **Acceptance-criteria headers** on every `*ServiceTest.kt` (closes #22) — each file now opens with a KDoc block naming the wiki Job Story it covers and the explicit ACs (`AC-AUTH-1`, `AC-DIARY-1`, `AC-GOAL-1`, `AC-RECIPE-1`, `AC-MSG-1`, `AC-DB-1` …) that each test method exercises. No test logic changed.

### Changed
- **README** (closes #23) — the `## UI Prototype` section pointed to a `ui-prototype/` folder that was removed long ago; replaced with a `## UI design` section pointing to `UI_wireframes.md`.

### Notes
- Documentation / process only. No runtime behaviour change.
- The first run of the new workflow will be the merge commit of this PR.

---

## [v0.4.4] - 2026-04-30 — Critical security fixes

### Fixed
- **IDOR on professional client view** (#16) — `GET /pro/client/{id}` previously returned any subscriber's diary, goals, and nutrition summary as long as the requester's session role was `professional`. Now also requires an active row in `client_relationships` between the professional and the requested subscriber; otherwise the request is redirected to `/pro/dashboard`.
- **IDOR on professional advice endpoint** (#17) — `POST /pro/client/{id}/advice` previously created a message addressed to any user id. Same authorisation gate now applies; an extra role check was added on this route for parity with the GET. Submissions to non-client ids now redirect and create no message.
- **Session cookie missing `HttpOnly` / `SameSite`** (#18) — `config/Security.kt` now sets `cookie.httpOnly = true` and `cookie.extensions["SameSite"] = "Lax"`. JavaScript can no longer read the session cookie, and browsers will not send it on cross-site POST submissions, blocking the standard form-based CSRF path. `Secure` is intentionally left off so `./gradlew run` over plain HTTP in dev / Codespaces keeps working; flip it on for production HTTPS.
- **`LIKE` wildcard injection in food / recipe search** (#19) — `searchFood()` (DiaryService) and `searchRecipes()` (RecipeService) now escape the SQL wildcards `%` and `_` and the escape character `\` before substituting user input into the `LIKE` pattern. Submitting `%` no longer matches the entire table.

### Notes
- All four fixes are scoped to authorisation / cookie / search-input handling; no schema changes, no UI changes, no new dependencies.
- Existing unit tests still pass; manual regression covered the happy paths (Sarah → Alice diary visible, food search for `apple`, recipe search for `salmon`).

---

## [v0.4.3] - 2026-04-29 — Backfill Cursor acknowledgment in AI_USAGE.md

### Added
- `AI_USAGE.md` now lists the 5 pre-v0.4.0 commits assisted by Cursor (build-config / Dockerfile fixes by `chlwu0777`). The `Made-with: Cursor` git trailers on those commits remain as the contemporaneous acknowledgment; this update brings the project-level log to full coverage.

### Notes
- Documentation only. No code changes. The Cursor trailers were already valid amber-rating acknowledgments — this commit just consolidates them into the central log.

---

## [v0.4.2] - 2026-04-29 — AI acknowledgment & code comments

### Added
- `AI_USAGE.md` at the repo root — canonical log of every AI-assisted contribution (model, scope, what the human verified) for COMP2850 amber-rated AI use compliance.
- Header comments in `static/css/styles.css` and `static/js/app.js` naming the AI model (Claude Opus 4.6) and the lines/sections it drafted, per the assessment brief example format.
- README "Generative AI usage" section pointing to the log.

### Changed
- Going forward, no `Co-Authored-By: <AI>` git trailers and no `🤖 Generated with Claude Code` bot footers in PR descriptions — AI acknowledgment lives in code comments and `AI_USAGE.md` instead, treating AI as a supportive tool (not a contributor) in line with the amber rating.

### Notes
- No code behavior changes — comments and documentation only.
- Retroactively documents AI assistance for v0.4.0 (UI refresh) and v0.4.1 (devcontainer config).

---

## [v0.4.1] - 2026-04-29 — Codespace / dev container config

### Added
- `.devcontainer/devcontainer.json` — pins JDK 17 (matches `build.gradle.kts` `jvmToolchain(17)`) so GitHub Codespaces and VS Code Dev Containers boot ready to run `./gradlew run` with no manual `apt-get install` or JDK switching.
  - Base image: `mcr.microsoft.com/devcontainers/java:1-17-bookworm`
  - Port `8080` auto-forwarded with `openBrowser` on first start
  - `postCreateCommand` warms the Gradle daemon so the first build is faster
  - Pre-installs Kotlin, Gradle, and Thymeleaf VS Code extensions

### Notes
- No code changes — config-only.
- Existing local dev workflow (`./gradlew run` with a system JDK 17) is unaffected.

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
