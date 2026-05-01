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

### v0.4.7 — UX test report + integration tests + security tests + design-decisions doc (closes #31, #32, #33, #34)

| File | What AI drafted | Human verification |
|---|---|---|
| `UX_TESTING.md` | The structure of the document, the wording of each finding row, the severity assignments, the decision rationale on what to fix before the demo. | Charlie Wu reviewed each finding to confirm it matches what the participant actually struggled with during the walkthrough; the time-on-task numbers were recorded by Charlie during the test. |
| `src/test/kotlin/com/goodfood/IntegrationTest.kt` | The Ktor `testApplication` scaffolding (in-memory H2 config override via `MapApplicationConfig`, the `useInMemoryDb()` helper) and all three test methods. | Charlie Wu reviewed the assertion logic (302/200 status code expectations, body substring checks for the login page) and confirmed they match the project's actual route behaviour. |
| `src/test/kotlin/com/goodfood/SecurityTest.kt` | All five test methods, including the data-shape choices (5 foods none containing `%`, two users for cross-user delete, etc.). | Charlie Wu cross-checked each test against the v0.4.4 fix it guards: `%`-as-literal against `escapeLikePattern()`, cross-user delete against the WHERE clause in `DiaryService.deleteEntry()`. |
| `DESIGN_DECISIONS.md` | Wording of all ten "what / why / alternatives considered" entries. | Charlie Wu read each entry against the actual git history and CHANGELOG to confirm chronology and accuracy of the rationale. |
| `README.md` "Beyond the basic spec" + links | Wording of the extras section and the links to the new docs. | Charlie Wu confirmed every claim in the extras section is backed by an artefact in the repo. |

### v0.4.6 — Detekt + class diagram + KDoc + user stories + accessibility audit (closes #25, #26, #27, #28, #29)

| File | What AI drafted | Human verification |
|---|---|---|
| `2850final project/build.gradle.kts` (Detekt block) + `detekt.yml` + workflow Detekt step | Detekt plugin wiring, the `detekt {}` configuration block, the rule set in `detekt.yml` (rule names, thresholds, `buildUponDefaultConfig = true`), and the new CI step with `continue-on-error: true` for the first non-blocking run. | Charlie Wu reviewed the rule selection against the codebase shape (long Exposed-DSL expressions, multi-`return@get` route handlers, BCrypt magic numbers) and confirmed the thresholds avoid a first-run failure avalanche. |
| `CLASS_diagram.md` | Whole file: Mermaid `classDiagram` syntax, the per-feature grouping, the dependency arrows, the layering-rules section. | Charlie Wu cross-checked every class node against the actual files in `src/main/kotlin/com/goodfood/` and every dependency arrow against the imports in those files. |
| KDoc blocks on `UserService`, `DiaryService`, `GoalService`, `RecipeService`, `MessageService` | The wording of the class-level and method-level KDoc. AI did not change any function bodies. | Charlie Wu re-read each KDoc to make sure it matches what the function actually does (especially the security-sensitive notes on `authenticate()` returning the same `null` for "unknown email" and "wrong password", and on `deleteEntry()`'s defence-in-depth WHERE clause). |
| `USER_STORIES.md` | The story phrasing, the MoSCoW priorities, the XS/S/M/L estimates, the AC-ID cross-references. | Charlie Wu reviewed each story against the wiki Job Stories so the mapping is accurate, and confirmed every AC ID referenced exists in the test suite. |
| `ACCESSIBILITY.md` | The per-page WCAG checklist structure, the list of structural a11y patterns, and the wording of the known-gaps section. AI did not run an automated audit; the audit results are the team's self-assessment using the existing CSS / template features as evidence. | Charlie Wu confirmed every "✅" cell maps to a real pattern in the codebase, and that the "⚠️ / ❌" cells are honest gaps not glossed over. |

### v0.4.5 — CI workflow + test ↔ acceptance criteria mapping + README fix (closes #21, #22, #23)

| File | What AI drafted | Human verification |
|---|---|---|
| `.github/workflows/build.yml` | Whole workflow file: triggers, JDK 17 setup-java, Gradle cache key strategy, working-directory pinning to the nested `2850final project/` gradle root, test-report artifact upload | Charlie Wu reviewed the YAML against the GitHub Actions schema and the Ktor + Gradle 8.5 toolchain pin in `build.gradle.kts`. Will verify green check on the merge commit. |
| KDoc headers in 6 `*Test.kt` files (`UserServiceTest`, `DiaryServiceTest`, `GoalServiceTest`, `MessageServiceTest`, `RecipeServiceTest`, `NutritionalGoalsTest`) | Wording of the Job-Story → AC → test-method mapping blocks. AC IDs follow a consistent `AC-<MODULE>-<N>` scheme. | Charlie Wu cross-checked each AC against what the test method actually asserts, and against the Job Stories in the project wiki. No production / test code paths changed. |
| `README.md` rewrite of the broken UI section | Replacement section text pointing to `UI_wireframes.md`. | Charlie Wu confirmed `UI_wireframes.md` exists and the `ui-prototype/` directory does not. |

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
