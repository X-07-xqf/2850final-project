# Sage

A food diary, a recipe browser, and a way to message a real nutritionist when you actually want one. Built as the COMP2850 group project.

Live demo: https://two850final-project.onrender.com
(First request after a quiet hour takes ~30s — Render's free tier sleeps.)

## What's in it

Two role-flipped sides sharing the same database:

- **Subscriber** — log meals, set macro goals, browse recipes, save favourites, message a coach.
- **Professional** — see every subscriber's day at a glance, drill into any client's week, message back.

The role flag picks which sidebar you see; everything else lives in shared tables.

## Try it without registering

These accounts are seeded on the first boot of every fresh database, so they exist on the live site, in Codespaces, and on anyone's local clone.

| | email | password |
|---|---|---|
| Subscriber (today's meals already logged) | `alice@email.com` | `password` |
| Subscriber (empty plate) | `bob@email.com` | `password` |
| Professional | `sarah@clinic.com` | `password` |

If you register your own account, it lives only in the database it was registered against — Render's Postgres and your local H2 don't talk to each other.

## Stack

Ktor + Kotlin on the back, Thymeleaf for views, plain CSS and vanilla JS on the front. No React, no Tailwind, no SPA — server-rendered HTML with a sprinkle of `fetch` for the bits that benefit from feeling live (chat polling, food picker, dashboard count-ups).

Database is H2 in dev (zero setup), Postgres on Render. Exposed ORM either way.

## Running locally

```bash
./gradlew run
```

JDK 17. Opens on port 8080. The seed accounts above are inserted on first boot.

## Things we know are rough

- Chat uses 4-second polling, not WebSockets. Fine for a two-device demo, not Slack-tier.
- No image uploads — recipes show a handful of curated Unsplash photos; the food picker uses emoji on category-tinted gradients.
- The "Active recently" green dot on chat avatars is decorative — we don't actually track presence.
- Render's free tier cold-starts; the first request after idle takes a beat.

## Where things live

```
src/main/
├── kotlin/com/goodfood/
│   ├── Application.kt    — Ktor entry point
│   ├── auth/             — login, register, session
│   ├── diary/            — daily food log + dashboard
│   ├── recipes/          — recipe browse + detail
│   ├── goals/            — macro targets + weekly chart
│   ├── messages/         — 1:1 chat with polling
│   ├── professional/     — pro client list + detail
│   ├── profile/          — favourites, settings
│   ├── seed/             — the demo accounts above
│   └── config/           — Database, Routing, Security, Templating
└── resources/
    ├── templates/        — Thymeleaf HTML
    └── static/           — one styles.css, one app.js
```

`CHANGELOG.md` has the build-by-build history.
