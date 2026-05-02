# UX Testing — Round 1

This document records our first round of usability testing on the Good Food web app.
Round 1 was a "moderated walkthrough" style test, with one observer, one participant and a think-aloud protocol, with the goal being to surface obvious friction before the demo on 11 May.

## Test setup

* App version: `v0.4.5` (post-CI/CD merge, pre-Detekt baseline cleanup).
* Browser / viewport: Chrome 124 on macOS, default desktop window 1440 × 900; second pass at iPhone 13 viewport (390 × 844) via DevTools device toolbar.
* Account used: seeded subscriber `alice@email.com` / `password`.
* Date: 2026-04-30.
* Participant role: a teammate not directly involved in writing the front-end CSS, asked to perform the scenarios cold.
* Observer: Charlie Wu (took notes, did not coach).

## Scenarios

The participant was asked to complete five tasks in order, without prior demo:

1. Sign in as Alice and reach today's dashboard.
2. Log a meal: add oatmeal (any quantity) to today's breakfast.
3. Find a recipe for salmon and add it to favourites.
4. Set a daily calorie goal different from the default and confirm the dashboard reflects it.
5. Read a message from your dietitian and reply with one line.

Side observations were captured throughout (not tied to a single scenario): visual polish, sidebar nav clarity, dark-mode behaviour, focus visibility, mobile-drawer behaviour.

## Findings

| #   | Severity  | Where                    | Observation                                                                                                                                                                                           | Decision                                                                                                                                                                                                                 |
| --- | --------- | ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| F-1 |  High   | Diary "Add food" modal   | After typing "oatmeal" the participant saw the dropdown but did not realise they had to click a result before the green "Add to diary" button activates. They tried submitting twice without selecting. | Fix before demo — the submit button is disabled but there is no helper text explaining why. Add a small inline hint like "Pick a food from the list to enable Add". Tracked in this PR's notes for a v0.4.7 follow-up. |
| F-2 |  Medium | Sidebar                  | The participant initially missed the "Goals" entry because the nav-link styling makes inactive items relatively low-contrast.                                                                         | Fix before demo — bump inactive sidebar link colour from `rgba(255,255,255,0.88)` → `rgba(255,255,255,0.95)`. Trivial CSS change, queued for v0.4.7.                                                                     |
| F-3 |  Low    | Recipe detail page       | Star-rating buttons are clickable but do not announce their pressed state via `aria-pressed`. Sighted users get the colour; screen-reader users do not.                                               | Fix in v0.4.7 — already documented in `ACCESSIBILITY.md` as a known gap.                                                                                                                                                 |
| F-4 |  Medium | Mobile drawer (≤ 840 px) | Drawer opens correctly; closing via the backdrop works; but the participant did not realise they could use the hamburger button on each subsequent page (it is icon-only).                            | Defer — `aria-label="Open menu"` is in place, the visual icon is conventional, no further hint needed for sighted users. Documented for future round.                                                                    |
| F-5 |  Low    | Dark mode                | Theme toggle works. Participant noted a sub-second flash of light theme on first navigation only when reloading from cache — appears to be a ServiceWorker / cache-priming artifact, not a CSS bug.   | Defer — non-reproducible on subsequent attempts; not worth chasing pre-demo.                                                                                                                                             |
| F-6 |  Medium | Login form               | Error message "Invalid credentials" appears above the form but is not announced to screen readers (no `role="alert"` / `aria-live`).                                                                  | Fix before demo — one-line template change, queued for v0.4.7.                                                                                                                                                           |
| F-7 |  Low    | Dashboard progress bars  | Participant immediately understood the four bars; commented positively on the colour gradient on the calories bar.                                                                                    | Keep — design decision validated.                                                                                                                                                                                        |
| F-8 |  Medium | Recipe search            | Filter dropdown for difficulty has no visible label until the user clicks it.                                                                                                                         | Defer — the surrounding form does have a `<label>`, just visually compact. Will revisit if a second tester also stumbles.                                                                                                |

## Time-on-task

| Scenario                         | Time | Result                  |
| -------------------------------- | ---- | ----------------------- |
| 1. Sign in                       | 0:18 |  no friction            |
| 2. Log a meal (oatmeal, breakfast) | 1:42 |  2 retries due to F-1 |
| 3. Favourite a salmon recipe     | 0:55 | Success                 |
| 4. Set a daily calorie goal      | 0:34 | Success                 |
| 5. Read + reply to a message     | 0:48 | Success                 |

Total: 4 m 17 s for the full happy path on a cold start.

## What we will change before the demo (v0.4.7)

1. F-1 — Add a `field-hint`-styled paragraph inside the food modal: "Pick a food from the list to enable the Add button."
2. F-2 — Bump inactive nav-link contrast.
3. F-6 — Wrap the login error in `<div role="alert" aria-live="polite">…</div>`.

These are scoped for a quick v0.4.7 PR — small enough to land before 11 May without risking regressions.

## What we will defer to a future iteration

* F-3 star-rating `aria-pressed` (already tracked in `ACCESSIBILITY.md`)
* F-4 mobile-drawer hint
* F-5 flash-of-light-theme on cached page
* F-8 difficulty filter label visibility — pending Round 2 corroboration

## Round 2

Planned post-demo: invite a second participant outside the team, repeat scenarios 1-5, plus add scenario 6 ("Send advice to a client") to cover the professional flow. Findings will land in `UX_TESTING.md` under a new `## Round 2` section.

