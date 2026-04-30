# AI Usage Log

This project is COMP2850 Software Engineering — Group Project at the University of Leeds. The assignment is rated **amber** for use of generative AI, meaning AI tools may be used in a *supportive* role and **must be acknowledged**. This document is the canonical record of where and how generative AI assisted this codebase.

> *Reference:* COMP2850 Assessment Brief, Section 7 — "Academic misconduct and plagiarism / Generative AI". The brief explicitly permits AI for: proofreading, spoof test data, summarising the spec, debugging support, concept explanation, and **front-end code support**.

## Contributors using AI

- **Charlie Wu** (`Charlie-920`) — used Claude Opus 4.6 via Claude Code CLI for the contributions logged below. All AI-generated code was reviewed, tested, and merged manually.

## Tools

| Tool | Model | Vendor | Mode of use |
|---|---|---|---|
| Claude Code CLI | Claude Opus 4.6 | Anthropic | Pair-programming; AI drafts, human reviews + tests + merges |
| Cursor | (in-editor model, not specifically logged at the time) | Anysphere | In-editor inline completion / chat; used for build-config and Dockerfile fixes early in the project. Each Cursor-assisted commit carries a `Made-with: Cursor` git trailer. |

## Log of AI-assisted contributions

### Pre-v0.4.0 — Cursor-assisted build/deploy fixes (chlwu0777)

These commits carry the `Made-with: Cursor` git trailer as the contemporaneous acknowledgment. Listed here for completeness:

| Commit | Scope | Human verification |
|---|---|---|
| `32cf2c4` | feat: add Ktor full-stack web application with feature-based module structure | Charlie Wu reviewed and tested locally before push. |
| `7ce9809` | fix: add Dockerfile to repo root for Render.com deployment | Confirmed Render build succeeded. |
| `47bb180` | fix: use JSON array syntax in Dockerfile COPY for paths with spaces | Verified `docker build` against the spaced project path. |
| `57adf8b` | fix: upgrade Ktor plugin to 2.3.12 for Gradle 8.5 compatibility | Verified `./gradlew run` boots without plugin errors. |
| `0b0da55` | fix: replace io.ktor.plugin with shadow plugin for Gradle 8+ compatibility | Verified `./gradlew shadowJar` produces a runnable fat-jar. |

### v0.4.0 — UI visual refresh, dark mode, component polish (PR #2)

| File | What AI drafted | Human verification |
|---|---|---|
| `2850final project/src/main/resources/static/css/styles.css` | Whole-file rewrite (~767 added lines): semantic color tokens, light/dark theme variables, `:focus-visible` rings, animation keyframes (`shimmer`, `bubble-in`, `toast-in`, `skeleton`), card hover lift, modal scale-in + backdrop blur, mobile drawer styles, tablet breakpoint, `prefers-reduced-motion` rule, print stylesheet | Charlie Wu ran `./gradlew run`, navigated every page, tested light/dark toggle, resized to <840 px and <480 px in Chrome DevTools, confirmed no console errors, no layout regressions. |
| `2850final project/src/main/resources/static/js/app.js` | Added `initTheme()` (theme-toggle handler with localStorage persistence + flash-of-wrong-theme prevention) and `initSidebarDrawer()` (mobile drawer open/close logic) | Charlie Wu tested theme persistence across reloads, drawer open via hamburger, close via backdrop / Escape / nav-link click. |
| `2850final project/src/main/resources/templates/auth/login.html` | Inserted floating theme-toggle button (3 lines) | Charlie Wu visually verified position and dark-mode contrast. |
| `2850final project/src/main/resources/templates/{subscriber,professional}/*.html` (10 files) | Inserted hamburger toggle, sidebar backdrop, and theme-toggle button (3 lines added per file via a Python script) | Charlie Wu spot-checked dashboard, diary, recipes, professional dashboard, professional client-detail. |

### v0.4.1 — Codespace devcontainer (PR #3)

| File | What AI drafted | Human verification |
|---|---|---|
| `.devcontainer/devcontainer.json` | Whole file: pinned JDK 17 base image, port 8080 forward + auto-open-browser, `postCreateCommand` to warm Gradle, recommended VS Code extensions | Charlie Wu validated against the project's `build.gradle.kts` `jvmToolchain(17)` requirement and confirmed the structure follows the official devcontainer schema. |

### v0.4.2 — AI acknowledgment & code comments (this PR)

| File | What AI drafted | Human verification |
|---|---|---|
| Header comments in `styles.css` and `app.js` | Wording of the AI-acknowledgment block | Charlie Wu reviewed the wording and confirmed it accurately describes what AI did and what the human did. |
| `AI_USAGE.md` (this file) | Initial structure and entries | Charlie Wu reviewed every entry for accuracy. |

### v0.4.4 — Critical security fixes (closes #16, #17, #18, #19)

| File | What AI drafted | Human verification |
|---|---|---|
| `professional/ProfessionalRoutes.kt` — new `hasActiveRelationship()` helper + two authorisation gates on `GET /pro/client/{id}` and `POST /pro/client/{id}/advice` | Helper structure and the gate pattern (`return@get respondRedirect` on missing relationship) drafted with Claude Opus 4.6 acting as a Kotlin pair-programmer. | Charlie Wu reproduced the original IDOR with seeded users (Sarah supervises only Alice), confirmed `/pro/client/2` redirects to dashboard after the fix, confirmed `/pro/client/1` (the legitimate client) still works, posted advice to a non-client and verified no message row is created. |
| `config/Security.kt` — `cookie.httpOnly = true` and `cookie.extensions["SameSite"] = "Lax"` plus a comment block explaining why `Secure` is left off in dev | Wording of the comment block; the two cookie property lines are standard Ktor API and were verified against the Ktor 2.3.7 docs. | Charlie Wu logged in via Chrome DevTools → Application → Cookies, confirmed the `user_session` cookie now shows `HttpOnly ✓` and `SameSite Lax`. |
| `diary/DiaryService.kt` and `recipes/RecipeService.kt` — private `escapeLikePattern()` helpers escaping `\`, `%`, `_`, applied to the `LIKE` clauses in `searchFood()` and `searchRecipes()` | The escape helper and its three-step `replace()` chain. | Charlie Wu searched for `%` in food search → returned 0 rows (was previously the entire 50+ table); searched `apple` → still returns the apple food items as expected; same checks against recipe search. |

## Code/files NOT touched by AI

The entire **backend** (Kotlin, Ktor, Exposed, routes, services, models, seed data, SQL files) was authored by the team. AI's role was strictly front-end polish + dev tooling.

- `src/main/kotlin/com/goodfood/**` — team
- `src/main/resources/*.sql` — team
- `src/main/resources/application.conf` — team
- `build.gradle.kts`, `settings.gradle.kts` — team
- `Dockerfile`, `ER_diagram.md`, `UI_wireframes.md`, `README.md` — team
- The pre-v0.4.0 baseline of `styles.css` and `app.js` — team

## Policy going forward

- AI suggestions land in a feature branch, never directly on `main`.
- Every AI-assisted commit / PR has the model and scope acknowledged in the PR description and (where the change is non-trivial) in a header comment in the file.
- The committer / author of every commit is the human team member running the tool — never the AI service account. We do not use `Co-Authored-By: <AI>` git trailers because the assignment expects human authorship with AI in a supportive role; the trailer would misrepresent contribution attribution.
- Updates to this log go in the same PR as the AI-assisted change.
