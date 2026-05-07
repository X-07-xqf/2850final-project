# AI usage

This module is **amber-rated** for generative AI (COMP2850 Group Project
brief, Section 7). Allowed uses are: proofreading and translating text,
generating spoof data for testing, summarising / explaining the brief,
support with debugging, support understanding a concept, and support
developing front-end code.

The team chose every design direction, every feature, and every
trade-off. AI was used as a pair-programmer for the front-end and as a
debugging rubber duck. Every line was reviewed and tested before merge.

## Tool

- **Claude Opus 4.6** (Anthropic) — chat interface and CLI assistant.

## Where it shows up in the code

Inline comments in the form `used Claude Opus 4.6 to <verb> …` mark each
spot. Verbs are constrained to the four allowed by the brief:

| Verb | Means | Brief mapping |
|---|---|---|
| explain | unpack a browser quirk or concept | "explain parts of the specification", "supporting you with understanding a concept" |
| suggest | propose a CSS / JS pattern | "support you in developing front-end code" |
| translate | convert between formats / palettes | "proofread and translate text" |
| describe where the bug was | locate a failure | "support you with debugging" |

### Files annotated

- `src/main/resources/static/css/styles.css` — 6 inline notes (dark-mode
  token translation, liquid-glass shadow, viewport + stacking context,
  weekly-chart pill bars, mobile back arrow cascade order, iOS Safari
  backdrop-filter compositing).
- `src/main/resources/static/js/app.js` — 4 inline notes (autosizing
  textarea, multipart-vs-urlencoded send bug, visibility-aware polling,
  Date → hh:mm bubble timestamp).

Templates (`*.html`), Kotlin services, repositories, and tests were
written by the team. AI was not used to draft business logic, database
queries, security checks, or any back-end code.

## Why this matters

The team is graded on the work, not the tool. Acknowledging where AI
helped is part of academic integrity — and lets the marker focus on the
parts the team owns end-to-end (requirements gathering, personas, job
stories, UX testing, system design, the Kotlin back-end, the test
suite, the demo).
