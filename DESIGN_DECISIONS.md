# Design Decisions

A short, chronological record of the significant design and architecture choices made on the project. Each entry says **what changed**, **why**, and what the **alternatives** were. Small refactors are skipped — only changes that altered the user-facing or developer-facing experience are listed.

---

## D-1 — Feature-module package layout (v0.3.0)

**What.** Restructured `src/main/kotlin/com/goodfood/` from layer-based folders (`models/`, `routes/`, `services/`, `plugins/`) to feature-based modules (`auth/`, `diary/`, `recipes/`, `goals/`, `messages/`, `professional/`, `profile/`, `seed/`, `config/`). Each feature folder now contains its own table objects, routes, and service.

**Why.** The previous layer-based layout meant changing one feature touched four directories. Feature-based modules localise the cognitive cost: editing the diary feature only requires opening one folder. This also matches how the team mentally reasons about scope ("the diary stuff").

**Alternatives considered.** Keep the layer-based layout (rejected — cross-cutting changes were noisy in PRs). A more strict hexagonal architecture (rejected — overkill for a 7-week student project).

---

## D-2 — H2 file-mode database with seed-on-empty (v0.2.0)

**What.** Database is H2 in MySQL-compatible mode, file-backed at `./data/goodfood`. `SeedData.insertIfEmpty()` runs on every boot and inserts the demo users / foods / recipes only when `users` is empty.

**Why.** The brief expects the marker to clone, run, and demo with zero setup. A persistent file-based H2 means logged data survives between gradle runs (good for multi-day demos) without needing the marker to install MySQL or Postgres. The "insert if empty" guard means re-running gradle does not duplicate seed rows.

**Alternatives considered.** In-memory H2 (rejected — survives single boot only). Embedded MySQL via testcontainers (rejected — heavy, requires Docker for marker). PostgreSQL with a managed connection (rejected — outside scope).

---

## D-3 — Server-side rendering with Thymeleaf, no SPA (v0.2.0)

**What.** All pages are server-rendered HTML via Thymeleaf templates. JavaScript is only used for progressive enhancement (theme toggle, modal, food-search autocomplete).

**Why.** Aligns with the marker's expected stack (Kotlin/Ktor) and the brief's scope. Removes the need for a separate front-end build pipeline. Makes the page rendering observable in DevTools without source maps. Easier to argue accessibility wins because everything is real HTML.

**Alternatives considered.** React/Vue + JSON API (rejected — doubles the surface area without adding marks; less consistent with the spec). HTMX (considered — would have been fine, but kept things simpler with vanilla Thymeleaf).

---

## D-4 — Visual refresh + dark mode token system (v0.4.0)

**What.** `static/css/styles.css` rewritten around ~30 semantic colour tokens (`--color-surface`, `--color-text-strong`, `--sidebar-bg-pro`, etc.). Light theme is the default; dark theme is delivered via a single `[data-theme="dark"]` selector at the root, with auto-fallback to `prefers-color-scheme`. A localStorage-backed manual toggle persists the user's choice across reloads.

**Why.** The previous palette of ~10 hard-coded hex values made it impossible to add a dark mode without rewriting half the file. Tokenising the palette pays back twice: dark mode ships in one branch, and any future palette change is a one-token edit.

**Alternatives considered.** CSS variables but only a single colour (`--primary`) — rejected, dark mode would still need duplication. CSS-in-JS — rejected, no JS framework on this project. A separate dark stylesheet — rejected, harder to keep in sync.

**Trade-off accepted.** The token rewrite was a large-diff change; we chose to land it in a single PR with a "no behaviour change" commitment rather than dribble it across many small PRs and risk inconsistency.

---

## D-5 — Mobile drawer instead of squashed top-bar nav (v0.4.0)

**What.** Below 840 px viewport width the sidebar is no longer rendered as a horizontal bar at the top of the page. Instead it becomes a fixed-position drawer that slides in from the left when a hamburger toggle is tapped, with a backdrop that closes it on tap / Escape / nav-link click.

**Why.** The previous top-bar layout broke on phones — long nav labels wrapped onto two rows and the active-link state became hard to see. The drawer pattern is industry-standard, gets out of the way of the content, and works with a single 40 × 40 hit-target.

**Alternatives considered.** A bottom-tab bar (rejected — the project has 6 nav items, exceeding the 4–5 sweet spot for tab bars). A "more" overflow menu (rejected — adds an extra click for common destinations).

---

## D-6 — IDOR-safe authorisation pattern on professional routes (v0.4.4)

**What.** Added `hasActiveRelationship(professionalId, subscriberId)` helper to `ProfessionalRoutes.kt`, gating both `GET /pro/client/{id}` and `POST /pro/client/{id}/advice`. The route returns 302 to `/pro/dashboard` when no active row exists in `client_relationships`.

**Why.** The previous code only checked the *role* on the session ("are you a professional?"), not the *relationship* ("are you THIS subscriber's professional?"). This is the textbook IDOR pattern. Centralising the check in one helper means the same gate is reused on every future pro-only route — adding new endpoints does not require remembering to write the check.

**Alternatives considered.** A Ktor `authenticate("pro-client")` configuration (rejected — overkill for a single helper, and Ktor's auth plugin is geared at user identity not row-level ownership). Inline the check at every callsite (rejected — easy to forget on the next endpoint).

---

## D-7 — Cookie hardening: HttpOnly + SameSite=Lax (v0.4.4)

**What.** `user_session` cookie now sets `httpOnly = true` and `extensions["SameSite"] = "Lax"`. `Secure` is **not** forced — the project runs over plain HTTP in dev / Codespaces, and forcing Secure would silently drop the cookie there. A comment in `Security.kt` documents how to flip it on for production TLS.

**Why.** `HttpOnly` removes one whole class of session-theft via XSS. `SameSite=Lax` removes one whole class of CSRF via cross-site form submission. Together they are defence-in-depth — the app is more robust even if some other layer fails.

**Alternatives considered.** `SameSite=Strict` (rejected — would break top-level navigation from external links e.g. password-reset emails, which is the canonical use-case Lax allows). Implementing a CSRF token plugin (deferred — the team would still set SameSite=Lax for defence-in-depth, and the rubric does not require both).

---

## D-8 — LIKE wildcard escaping rather than rewriting search (v0.4.4)

**What.** Both `searchFood()` (DiaryService) and `searchRecipes()` (RecipeService) now run user input through a small `escapeLikePattern()` helper that backslash-escapes `\`, `%`, `_` before substituting into the `LIKE` clause.

**Why.** A user could previously submit `%` and dump the entire `food_items` table through the autocomplete endpoint. This is an information-disclosure / search-amplified-DoS vector. Escaping is the minimum viable fix; full-text search would be a 10× bigger change for marginal gain.

**Alternatives considered.** Use Postgres ILIKE / full-text indexes (rejected — would mean swapping the database). Block any search shorter than 2 chars (rejected — partial mitigation, does not handle `__a`).

---

## D-9 — AI usage transparency rather than hiding it (v0.4.2 onward)

**What.** Created `AI_USAGE.md` at the repo root logging every AI-assisted contribution: model used, what was drafted, what the human verified. Settings configured so commits and PRs do **not** carry `Co-Authored-By: <AI>` trailers (which would otherwise make the AI appear in the GitHub contributors panel).

**Why.** The COMP2850 brief is amber-rated for generative AI: use is permitted **with acknowledgment**. Hiding AI use would be plagiarism. Surfacing AI use as a `Co-Authored-By` git trailer would put a robot avatar in the contributors panel, which misrepresents the team's contribution. The middle path — explicit acknowledgment in code comments + a central log + clean commit attribution — meets the rubric without bot signage.

**Alternatives considered.** No acknowledgment (rejected — academic misconduct under the brief). Co-Authored-By trailer on every AI-assisted commit (rejected — visually misleading on the contributors panel). Mention only in the individual reflection (rejected — markers grading the codebase would not see it).

---

## D-12 — Warm wellness redesign with animated login background (v0.6.0)

**What.** Replaced v0.5.0's research-journal palette with a consumer-wellness system: warm cream surfaces, deep-forest text, sage green primary, terracotta as a single warm accent reserved for warnings and empty states. Type stack consolidated to one friendly geometric sans (Plus Jakarta Sans). Geometry shifted to soft rounded corners (8 px / 14 px / 16 px / 20 px / pill) and subtle layered shadows for elevation. The login page background now hosts two slowly drifting soft-edged colour fields behind the auth card, animated entirely with CSS keyframes so the page feels alive without any JavaScript.

**Why.** The product owner's brief is explicitly about *encouraging* people to log their meals and try home cooking. The v0.5.0 aesthetic was disciplined but read as "research institution"; for a consumer wellness product that asks users to stick with a daily logging habit, the system should feel warm, friendly, and rewarding rather than austere. The brief's secondary pillar — home cooking — also benefits: recipe cards now have inviting elevation and rounded corners instead of the sharp newsprint feel.

**Trade-off accepted.** Class names stay; only `static/css/styles.css` is rewritten (~900 lines replaced). The v0.4.0 dark-mode toggle is preserved by inverting the warm palette into a deep-forest base with cream text. Plus Jakarta Sans alone replaces the v0.5.0 sans + serif + mono trio — losing the editorial pairing in exchange for a more contemporary consumer-app feel; the trade is appropriate for the product's audience.

**Animated background, performance.** The two drifting blobs use `transform` only (GPU-friendly), absolute-positioned outside the viewport, blurred with `filter: blur(40-50px)` so the pixel cost is amortised over a small composited region rather than per-pixel paint. `prefers-reduced-motion` collapses both keyframe durations to ~0 ms via the global rule. Print stylesheet hides them.

**Alternatives considered.** Cookbook-editorial direction (deep serifs, food photography, magazine grid — rejected because we don't have shoot-quality food imagery and the palette would clash with the dietitian-supervision module). Clinical / data-rich direction (rejected because the brief emphasises encouragement, not clinical data display). Keep v0.5.0 unchanged (rejected — the team explicitly didn't like how it landed).

---

## D-11 — Visual identity refresh inspired by Anthropic (v0.5.0)

**What.** Replaced the v0.4.0 emerald-green token system with a palette and component vocabulary inspired by anthropic.com: a warm parchment ivory (`#faf9f5`) page base, near-black slate (`#141413`) primary text, the entire chromatic budget reserved for a single terracotta accent (`#d97757`). Type stack switched to `Inter` + `Playfair Display` + `JetBrains Mono` (the substitutes recommended by the Anthropic style reference for non-licensed deployments). Buttons now have a `0` border-radius, with the primary CTA carrying the asymmetric `0 0 8px 8px` Anthropic signature. Cards are `8px` radius; dark editorial cards `24px`. **Zero box-shadows** anywhere — depth is conveyed by surface contrast and 1 px hairlines. Headline emphasis uses a thick text-decoration underline rather than colour.

**Why.** The team wanted a visual language that read as research-institution rather than fitness-startup — the "evidence-based healthy eating" positioning fits parchment-and-broadsheet better than emerald-and-shadows. The Anthropic site demonstrates exactly that direction: achromatic discipline, typographic gravity, hard-edged surfaces. We adopted the public design system (palette, type roles, geometry) as a learning exercise — the goal was to internalise how a token-driven design discipline differs from the more decorative starting point of v0.4.0.

**Trade-off accepted.** The token rewrite is large-diff (~700 lines of CSS replaced) but the class-name surface stays identical, so HTML templates need zero edits. We also kept the v0.4.0 dark-mode toggle even though the Anthropic reference is light-only; in dark we invert into a slate-dark base with ivory text.

**Alternatives considered.** Keep the green palette and bolt on Anthropic-style typography (rejected — half-measures land in the uncanny valley between two systems). Drop the dark-mode toggle to match Anthropic's light-only stance (rejected — we'd already shipped it and had positive feedback on it). Use the actual Anthropic Sans / Serif fonts (rejected — proprietary; their style reference recommends Inter / Playfair Display as substitutes for non-licensed work).

---

## D-10 — CI workflow with non-blocking Detekt (v0.4.5 / v0.4.6)

**What.** GitHub Actions `build-and-test` workflow runs `./gradlew build test` on every push to main and every PR. Detekt was added in v0.4.6 as a separate step with `continue-on-error: true` and detached from the default `check` task; its HTML report is uploaded as an artifact.

**Why.** The team needs CI for the marking rubric (Excellent tier on Use of Git requires Actions). But the codebase had ~50 baseline Detekt findings on first run; failing CI on day-one would block every PR while we cleaned them up. Non-blocking-with-report lets the team see the findings on every run and tighten rules sprint-by-sprint without halting work.

**Alternatives considered.** Block CI on Detekt errors immediately (rejected — would have caused all 5 v0.4.6+ PRs to fail until baseline was clean). Skip Detekt entirely (rejected — the brief explicitly names Detekt as a recommended tool).
