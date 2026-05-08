# Sage

A wellness web app where subscribers log meals, set macro goals, browse
recipes, and message a nutrition professional. Pros see every client's
day at a glance and chat back.

[![build-and-test](https://github.com/X-07-xqf/2850final-project/actions/workflows/build.yml/badge.svg)](https://github.com/X-07-xqf/2850final-project/actions/workflows/build.yml)

**Live demo:** https://two850final-project.onrender.com

COMP2850 Group 50 — University of Leeds, 2025–26.

---

## Users

- **Subscribers** log meals, set calorie + macro goals, browse and favourite
  recipes, and message a nutrition professional.
- **Professionals** see every client's day at a glance, flag clients who
  are off-track (80–100% calorie band), and chat back.

## Tech stack

- **Backend:** Kotlin, Ktor, Exposed ORM, BCrypt, JDK 17
- **Frontend:** Thymeleaf templates, plain CSS (no framework), vanilla JS
- **Database:** H2 in-memory in dev, PostgreSQL on Render in production
- **CI:** GitHub Actions — `build`, `test`, `detekt`, JaCoCo coverage report

```
2850final project/
└── src/main/
    ├── kotlin/com/goodfood/
    │   ├── Application.kt
    │   ├── auth/         diary/         recipes/       goals/
    │   ├── messages/     professional/  profile/
    │   ├── seed/         config/        util/
    └── resources/
        ├── templates/    static/        application.conf
```

## Quick start

```bash
cd "2850final project"
./gradlew run           # http://localhost:8080
./gradlew test          # run unit + integration + security tests
./gradlew detekt        # static analysis (zero warnings on main)
```

A demo subscriber and professional are seeded on first boot:
- `alice@email.com` / `password123` (subscriber)
- `sarah@clinic.com` / `password123` (professional)

## Tests

```bash
./gradlew test
```

- 7 service-level unit tests
- 6 integration tests (Ktor `testApplication`, real H2 per test)
- 5 security regression tests (LIKE injection, IDOR, session)

A coverage report is generated automatically and uploaded as the
`jacoco-coverage` artifact on every CI run — open it from the
[Actions page](https://github.com/X-07-xqf/2850final-project/actions).

## Documentation

All planning, design, and audit docs live in the [wiki](https://github.com/X-07-xqf/2850final-project/wiki):

- [Personas](https://github.com/X-07-xqf/2850final-project/wiki/Personas)
- [Job Stories](https://github.com/X-07-xqf/2850final-project/wiki/Job-Stories) and [User Stories](https://github.com/X-07-xqf/2850final-project/wiki/User-stories)
- [Database ER Diagram](https://github.com/X-07-xqf/2850final-project/wiki/Database-ER-Diagram)
- [Design and Planning](https://github.com/X-07-xqf/2850final-project/wiki/Design-and-Planning)
- [Design Decisions](https://github.com/X-07-xqf/2850final-project/wiki/Design-Decisions)
- [UX Testing](https://github.com/X-07-xqf/2850final-project/wiki/UX-Testing) — three rounds
- [Accessibility Audit](https://github.com/X-07-xqf/2850final-project/wiki/Accessibility-Audit) — WCAG 2.1 AA
- [Meeting & Retrospective Notes](https://github.com/X-07-xqf/2850final-project/wiki/Meeting-&-Retrospective-Notes)
- [Team Members](https://github.com/X-07-xqf/2850final-project/wiki/Team-Members)

In-repo:
- [`changelog.md`](2850final%20project/changelog.md) — release notes per Keep-a-Changelog
- [`ai_usage.md`](2850final%20project/ai_usage.md) — generative AI acknowledgment per COMP2850 amber guidance

## License

Coursework. Not for redistribution.
