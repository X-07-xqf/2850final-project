# Sage
A web app where subscribers log meals, set macro goals, browse recipes, and message a health professional. Professionals can see every client's
day at a glance and message back.

[![build-and-test](https://github.com/X-07-xqf/2850final-project/actions/workflows/build.yml/badge.svg)](https://github.com/X-07-xqf/2850final-project/actions/workflows/build.yml)

**Live demo:** https://two850final-project.onrender.com

COMP2850 Group 50 — University of Leeds.

## Users

Subscribers log meals, set calorie + macro goals, browse and favourite recipes, and message a nutrition professional.
Professionals can see every client's day at a glance, notice when clients are off-track (80–100% calorie band), and chat back.

## Tech stack

Backend: Kotlin, Ktor, Exposed ORM, BCrypt, JDK 17
Frontend: Thymeleaf templates, plain CSS (no framework), vanilla JS
Database: H2 in-memory in dev, PostgreSQL on Render in production
CI: GitHub Actions — `build`, `test`, `detekt`, JaCoCo coverage report

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
```

A demo subscriber and professional are seeded on first boot:
- `alice@email.com` / `password123` (subscriber)
- `sarah@clinic.com` / `password123` (professional)


