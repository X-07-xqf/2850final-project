# Changelog - Good Food & Healthy Eating

All notable changes to this project will be documented in this file.

---

## [v0.6.5] - 2026-05-02 — Final polish: macro typography, date prominence, filter row, sidebar tone (closes #52)

### Fixed
- Macro rows now split current value (body-size, strong) from goal (`/ 80 g` muted) instead of one grey lump.
- Page-header date bumped from small / soft to base size with a sage-soft left accent bar so the date reads as page context rather than fine print.
- Recipes filter card collapsed from a 3-row stacked form into a single inline strip (search input + difficulty select + Apply); roughly 60px less vertical air on `/recipes`.
- Sidebar bg softened from `#1f2a23` to `#2d3d34` (warmer, less editorial); active-link tint lifted to `rgba(184,209,168,0.26)` so the highlight reads on the new bg.

---

## [v0.6.4] - 2026-05-02 — Dashboard polish: ring, thicker bars, warmer empty states (closes #50)

### Fixed
- Calories progress was a thin 6px line buried among three other macros — promoted to a conic-gradient ring centrepiece with the absolute number and percent inside.
- Macro progress bars bumped from 6px to 10px with rounded ends and per-macro gradients (deep-sage → sage on calories, sage → sage-soft on protein, clay → clay-soft on fat).
- Empty-state copy (`No foods logged for this meal.`, `Nothing logged yet.`, `No data for this week yet.`) replaced with friendlier nudges; diary empty meals point at the **Add food** button.

---

## [v0.6.3] - 2026-05-01 — UI detail bug fixes (closes #46)

### Fixed
- Trailing `.00` on numeric values across Dashboard / Diary / Goals / professional client-detail.
- Raw ISO timestamps on Messages chat bubbles.
- Bare text-only Recipe cards.

---

## [v0.6.2] - 2026-05-01 — Persist accounts on Render via PostgreSQL (closes #44)

### Why
The H2 database file (`./data/goodfood.mv.db`) lives inside each container's local filesystem and is `.gitignore`d, so it never travels with the code. On Render's free tier the container is rebuilt on every deploy and on idle spin-up, which wipes `data/` and erases every account a user registered through the web UI. The seed accounts only appear permanent because `SeedData.insertIfEmpty()` re-runs against the fresh schema and re-creates them. The same dynamic explains why a Codespace registered account disappears the next time the codespace is recycled. For the assessment demo the marker needs to register an account and have it survive a redeploy, so production needs a database that lives outside the container.

### Changed
- **`Database.kt`** — new `resolveDbSettings()` helper. When the `DATABASE_URL` environment variable is present (Render injects this when a PostgreSQL service is linked to the web service) the libpq form `postgres://user:pass@host:port/db[?…]` is parsed into a JDBC URL plus separate credentials, and the connection goes through `org.postgresql.Driver`. When the variable is unset — Codespaces, local dev, CI — the existing H2 file path from `application.conf` is used unchanged. Any query string on the original URL (e.g. `sslmode=require` for an external Render endpoint) is preserved verbatim.
- **`build.gradle.kts`** — added `org.postgresql:postgresql:42.7.4` alongside the existing H2 driver. Both ship in the fat jar so the same artifact runs locally on H2 and on Render against PostgreSQL.

### Notes
- Existing Exposed queries are dialect-portable (`Recipes.title.lowerCase() like ?`, `FoodItems.name.lowerCase() like ?`); `lowerCase()` compiles to `LOWER(col)` which works on H2, MySQL, and PostgreSQL alike. Schema generation is delegated to `SchemaUtils.create(...)` and Exposed picks the right DDL per dialect.
- Render setup after merge: provision a PostgreSQL Free instance, attach it to the web service so `DATABASE_URL` is auto-injected, then redeploy. No code switch needed — the env var alone flips the runtime to PostgreSQL.

---

## [v0.6.1] - 2026-05-01 — Fix Render build (Gradle 8.5)

### Fixed
- **Render deploy** — bumped the Dockerfile build-stage image from `gradle:7.6-jdk17` to `gradle:8.5-jdk17`. The Shadow plugin in `build.gradle.kts` is at `com.gradleup.shadow:8.3.0`, which requires Gradle 8.3+; the old base image shipped Gradle 7.6, so `gradle shadowJar` failed with *"This version of Shadow supports Gradle 8.3+ only"* and Render builds exited with status 1. The new image matches the project's wrapper version (`gradle-8.5-bin.zip`). Local dev and GitHub Actions were unaffected because they use `./gradlew`, not the Docker base image's `gradle` binary.

---

## [v0.6.0] - 2026-05-01 — Warm wellness redesign + animated login background (closes #40)

The team sat with v0.5.0 for a couple of days and decided it was the wrong fit. The disciplined research-journal aesthetic looked sharp in isolation but read as clinical and intimidating for a consumer app whose entire purpose is to *encourage* people to log meals and try home cooking. The brief is about warmth and habit-building, not editorial gravity. v0.6.0 swaps the system out for one that fits the product.

### Changed
- **Palette** — cream / oat / sage / terracotta replaces ivory / slate / clay. Deep-forest text on warm cream surfaces. Sage as the calm primary; terracotta reserved as the single warm accent. Berry tone added for genuine alerts only.
- **Type** — consolidated to **Plus Jakarta Sans** at every level. The v0.5.0 sans + serif + mono trio was elegant but added formality the product doesn't need; one friendly geometric family across the system reads as "consumer wellness" rather than "magazine".
- **Geometry** — soft rounded corners throughout (8 / 14 / 16 / 20 px and pill). Buttons are now pills; modal corners 20 px; cards 16 px. Subtle layered shadows return as elevation cues — barely there but present, replacing the v0.5.0 zero-shadow rule.
- **Sidebar** — deep-forest band with cream text; active nav entry uses a soft sage-tinted background instead of the previous accent border.
- **Recipe cards** — inviting elevation on hover (translate-up + slightly heavier shadow). The home-cooking pillar of the brief is where the new system most obviously pays off.
- **Macro progress bars** — gradient fill from deep sage to sage on the calorie bar, solid sage on protein, muted sage on carbs, terracotta on fat.
- **Tabs in the auth card** — pill-tabs inside a warm tray with a soft shadow, replacing the underline-tab pattern.

### Added
- **Animated login background** — two large soft-edged colour fields (sage and terracotta tints) drift slowly behind the auth card. Pure CSS via `::before` and `::after` on `.auth-body`, `transform` and `opacity` only, GPU-composited, blurred for an organic feel. `prefers-reduced-motion` cuts the animation entirely. The auth card sits crisp on top — motion is wallpaper, not theatre.

### Notes
- Same constraint as v0.5.0: zero HTML class-name renames, JavaScript untouched, all 21 tests still green, dark mode keeps working (inverts into a deep-forest base with cream text).
- `DESIGN_DECISIONS.md` gains entry **D-12** explaining why v0.5.0 was retired.

---

## [v0.5.1] - 2026-05-01 — Food-search hint & threshold (closes #38)

### Changed
- The diary "Add food" search input now shows an inline hint underneath: *"Type at least 3 letters to search. Try ban, chi, or oat."* The placeholder also says *"Type at least 3 letters…"* instead of the vague *"Type to search…"*.
- The JS minimum-query-length constant is bumped from 2 → 3 so the actual behaviour matches the hint exactly (extracted as `MIN_QUERY_LEN` so the next change is a one-liner).
- `aria-describedby` wires the hint into the input's accessibility tree so screen-reader users hear the threshold too.

### Why
Testers (and one of our own teammates) kept typing 1–2 letters into the search and giving up when nothing appeared — the field looked broken on first interaction. The hint is the smallest possible fix.

---

## [v0.5.0] - 2026-05-01 — Visual identity refresh inspired by Anthropic

We've spent the last few weeks looking at how research-led companies communicate visually, and Anthropic's site (anthropic.com) kept coming up as a reference the team admired — the warm parchment ivory background instead of clinical white, the strict alternation of light and slate-dark surfaces, the serif-plus-grotesque type pairing that reads more "research journal" than "startup", and the discipline of holding the entire chromatic budget for a single terracotta accent. As a learning exercise we studied their public design tokens and component vocabulary, then translated the system onto the existing Good Food class names so we could see what our app looked like in that visual idiom without rewriting any templates.

The result is v0.5.0 — same product, redesigned skin.

### Changed
- **Palette** — replaced the v0.4.0 emerald token system with the Anthropic-inspired palette: ivory (`#faf9f5`) page base, near-black slate (`#141413`) primary text, ivory-medium / oat for surfaces, clay (`#d97757`) reserved as the one accent.
- **Surface alternation** — page base ivory; cards 8 px radius on ivory-medium / oat; sidebar a slate-dark band; "feature" cards 24 px radius on slate-dark with ivory text. Zero gradients, zero blur transitions.
- **Typography** — switched to `Inter` (sans), `Playfair Display` (serif), and `JetBrains Mono` (mono) via Google Fonts. Type scale aligned to the Anthropic spec: 12 → 15 → 18 → 20 → 24 → 61 → 91 px. Body uses `-0.002em` tracking; display sizes use `-0.02em`.
- **Geometry** — buttons radius `0`; the primary "Sign in" / "Add food" CTA gets the asymmetric `0 0 8px 8px` Anthropic signature; cards radius `8`; dark feature cards radius `24`. **All `box-shadow` rules deleted.** Depth is now conveyed by surface contrast and 1 px hairlines only.
- **Emphasis** — headline keywords use a thick `text-decoration: underline` (`.text-emphasis` utility) rather than colour, matching the Anthropic underline-as-accent convention.
- **Metadata labels** — `DATE`, `CATEGORY`, time-on-task and stat captions now use `JetBrains Mono` 12 px in uppercase with `0.06em` tracking, matching the Anthropic editorial/data-label pattern.
- **Forms** — inputs are flat 1 px hairlines, focus ring is `clay`-coloured.
- **Dark mode** — kept the toggle from v0.4.0 but inverted to a slate-dark base with ivory text (Anthropic's reference is light-only; we didn't want to drop a feature we'd already shipped).

### Notes
- **Zero template / class-name changes.** Every existing class (`.btn`, `.card`, `.sidebar`, `.macro-card`, `.recipe-card`, `.conv-list`, `.bubble`, `.modal`, `.toast`, `.skeleton`) is preserved with the same selector — only its styling has been redrawn. HTML templates were not touched.
- All 21 tests still pass. Mobile drawer + theme toggle JS still work.

### Inspiration
- anthropic.com — the design tokens and component vocabulary we studied for this refresh. Substitute fonts (Inter, Playfair Display, JetBrains Mono) are the ones the Anthropic style reference recommends for non-licensed deployments.

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
