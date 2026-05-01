# User Stories — Good Food & Healthy Eating

This file is the prioritised user-story backlog for the project. It complements the wiki's Job Stories (which describe the *trigger* and *motivation* of a user need) by adding role-based phrasing, a MoSCoW priority, an effort estimate, and traceability into the test suite via Acceptance Criteria IDs.

| AC ID format | Owner module | Lives in |
|---|---|---|
| `AC-AUTH-*` | `auth` | `UserServiceTest.kt` |
| `AC-DIARY-*` | `diary` | `DiaryServiceTest.kt` |
| `AC-GOAL-*` | `goals` | `GoalServiceTest.kt` |
| `AC-RECIPE-*` | `recipes` | `RecipeServiceTest.kt` |
| `AC-MSG-*` | `messages` | `MessageServiceTest.kt` |
| `AC-DB-*` | `diary` (schema) | `NutritionalGoalsTest.kt` |

**Estimate scale**: XS (≤2 h), S (½ day), M (1 day), L (multi-day).
**MoSCoW**: Must / Should / Could / Won't (this iteration).

---

## Subscriber stories

### US-1 — Account registration
**As a** new user with poor eating habits,
**I want to** register with my full name, email and a password,
**so that** I can start tracking what I eat under my own account.
- Priority: **Must**
- Estimate: **S**
- Maps to job story: *Account registration* (wiki — Job Stories — Subscriber Features)
- ACs: `AC-AUTH-1`, `AC-AUTH-2`

### US-2 — Login
**As a** returning subscriber,
**I want to** sign in with my email and password,
**so that** I see my own diary and goals, not somebody else's.
- Priority: **Must**
- Estimate: **XS**
- Maps to job story: *Login flow*
- ACs: `AC-AUTH-3`

### US-3 — Quick meal logging
**As a** busy subscriber,
**I want to** add a food entry to today's diary in under 10 seconds,
**so that** I actually log my meals instead of giving up.
- Priority: **Must**
- Estimate: **M**
- Maps to job story: *Quick Meal Logging*
- ACs: `AC-DIARY-1`

### US-4 — Daily nutrition summary
**As a** subscriber tracking my macros,
**I want to** see today's calories / protein / carbs / fat as progress bars against my goals,
**so that** I can tell at a glance whether I'm on plan.
- Priority: **Must**
- Estimate: **S**
- Maps to job story: *Personalised Goals*
- ACs: `AC-DIARY-1`, `AC-GOAL-1`

### US-5 — Set personal nutritional goals
**As a** subscriber whose targets differ from "the average user",
**I want to** set my own daily calorie and macro targets,
**so that** the dashboard reflects *my* plan.
- Priority: **Must**
- Estimate: **S**
- Maps to job story: *Personalised Goals*
- ACs: `AC-GOAL-1`, `AC-GOAL-2`

### US-6 — Recipe search by ingredient / title
**As a** subscriber wanting to cook tonight,
**I want to** search recipes by title or filter by difficulty,
**so that** I can find a quick suitable meal.
- Priority: **Must**
- Estimate: **S**
- Maps to job story: *Recipe Search*
- ACs: `AC-RECIPE-1`, `AC-DIARY-2`

### US-7 — Favourite recipes
**As a** subscriber who keeps cooking the same handful of meals,
**I want to** mark recipes as favourites,
**so that** I don't have to search for them every week.
- Priority: **Should**
- Estimate: **XS**
- Maps to job story: *Favourite Recipes*
- ACs: `AC-RECIPE-2`

### US-8 — Read advice from my professional
**As a** subscriber who has signed up for guidance,
**I want to** see when my dietitian has sent me a message and read it,
**so that** I notice the advice and act on it.
- Priority: **Must**
- Estimate: **S**
- Maps to job story: *Professional Advice*, *Conversation View*
- ACs: `AC-MSG-1`, `AC-MSG-2`

### US-9 — Weekly progress chart
**As a** subscriber on a plan,
**I want to** see my last 7 days of calorie totals on a small chart,
**so that** I can spot binge days or under-eating before they become a habit.
- Priority: **Should**
- Estimate: **S**
- Maps to job story: *Personalised Goals* (extension)
- ACs: covered indirectly by `AC-DIARY-1` (per-day totals)

---

## Professional stories

### US-10 — Professional dashboard with client list
**As a** professional dietitian,
**I want to** see my list of active clients with at-a-glance compliance status,
**so that** I can spot which clients to chase first today.
- Priority: **Must**
- Estimate: **M**
- Maps to job story: *Client List*

### US-11 — View only my own clients (authorisation)
**As a** professional,
**I want to** see *only* clients that have an active relationship with me,
**so that** I cannot accidentally (or maliciously) read the diary of someone who is not my client.
- Priority: **Must**
- Estimate: **S**
- Maps to job story: *Client List* (security NFR)
- Closed by issue: #16, #17

### US-12 — Send advice to a client
**As a** professional,
**I want to** send a written advice message to one of my clients,
**so that** they receive personalised guidance between sessions.
- Priority: **Must**
- Estimate: **S**
- Maps to job story: *Professional Advice*
- ACs: `AC-MSG-1`, `AC-MSG-2`

### US-13 — Read-only view of a client's diary
**As a** professional,
**I want to** view a single client's daily diary and macro totals,
**so that** I can give specific advice grounded in what they actually ate.
- Priority: **Must**
- Estimate: **M**
- Maps to job story: *Client diary view*
- ACs: depends on `AC-DIARY-1`

---

## Cross-cutting / non-functional

### US-14 — Accessible UI (WCAG 2.1 AA target)
**As a** user using a screen reader or keyboard,
**I want to** navigate the app with focus indicators, ARIA labels, and `prefers-reduced-motion` honoured,
**so that** my disability does not block me from logging food.
- Priority: **Must**
- Estimate: **M**
- Tracking: see [`ACCESSIBILITY.md`](ACCESSIBILITY.md)

### US-15 — Dark mode
**As a** user who tracks evening meals in low light,
**I want to** flip the UI to a dark theme that persists across reloads,
**so that** my eyes don't get fried at 11 pm.
- Priority: **Should**
- Estimate: **M**
- Delivered in v0.4.0
