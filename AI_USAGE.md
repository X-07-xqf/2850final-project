# AI Usage Log

This project is COMP2850 Software Engineering — Group Project at the University of Leeds. The assignment is rated **amber** for use of generative AI, meaning AI tools may be used in a *supportive* role and **must be acknowledged**. This document is the canonical record of where and how generative AI assisted this codebase.

> *Reference:* COMP2850 Assessment Brief, Section 7 — "Academic misconduct and plagiarism / Generative AI". The brief explicitly permits AI for: proofreading, spoof test data, summarising the spec, debugging support, concept explanation, and **front-end code support**.

## Contributors using AI

- **Charlie Wu** (`Charlie-920`) — used Claude Opus 4.6 via Claude Code CLI for the contributions logged below. All AI-generated code was reviewed, tested, and merged manually.

## Tool

| Tool | Model | Vendor | Mode of use |
|---|---|---|---|
| Claude Code CLI | Claude Opus 4.6 | Anthropic | Pair-programming; AI drafts, human reviews + tests + merges |

## Log of AI-assisted contributions

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
