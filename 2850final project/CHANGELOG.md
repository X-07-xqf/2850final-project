# Changelog

Format tightened on 2026-05-05 to short Keep-a-Changelog style. Earlier
verbose entries are in git history (`git log -p CHANGELOG.md`).

## [0.6.43] - 2026-05-06
### Fixed
- iOS Safari rendered the mobile sidebar text blurry when the drawer opened. (#136)

## [0.6.42] - 2026-05-05
### Fixed
- Mobile polish: stack header rows, full-width filter selects, tighter paddings, larger tap targets. (#134)

## [0.6.41] - 2026-05-05
### Added
- 14 more recipes; total now 23. (#130)

## [0.6.40] - 2026-05-05
### Changed
- Rewrote README. (#127)

## [0.6.39] - 2026-05-05
### Fixed
- Chat send: surface server failures so silent dropped messages can't recur. (#126)

## [0.6.38] - 2026-05-05
### Changed
- Tightened the four landing-page subtitles. (#124)

## [0.6.37] - 2026-05-05
### Fixed
- Messages dropped on send (multipart vs urlencoded). (#122)
### Added
- 4-second polling endpoint for cross-device live updates. (#122)

## [0.6.36] - 2026-05-04
### Added
- Password complexity at registration (upper + lower + digit). (#120)

## [0.6.35] - 2026-05-04
### Added
- Recipe filters: calories, protein, cooking time. (#118)

## [0.6.34] - 2026-05-04
### Changed
- Pro Clients: 80–100% On Track band, uncapped over-eating %. (#116)

## [0.6.33] - 2026-05-04
### Added
- Visual food picker grid in the diary modal; 37 more foods seeded. (#114)

## [0.6.32] - 2026-05-04
### Fixed
- Login page now scrolls on short viewports. (#112)

## [0.6.31] - 2026-05-04
### Changed
- Pro Clients lists every subscriber, not just supervised ones.
- Detail page enriched with profile meta, weekly chart, Open chat button. (#110)

## [0.6.30] - 2026-05-03
### Added
- Messages directory: subscribers see all professionals; pros see all clients. (#108)

## [0.6.29] - 2026-05-03
### Fixed
- Dashboard "For tonight" now shows real recipe images, not the emoji fallback. (#106)

## [0.6.28] - 2026-05-03
### Changed
- Liquid Glass pass: translucent cards + atmospheric backdrop layer. (#104)

## [0.6.27] - 2026-05-03
### Changed
- `.main` widened from 1200 → 1680 px; `clamp()` padding for fluid spacing. (#102)

## [0.6.26] - 2026-05-03
### Added
- Telegram-style chat: sticky header, bubble tails, date separators, pill composer. (#100)

## [0.6.25] - 2026-05-03
### Added
- Dashboard secondary modules: This week + Streak + For tonight + Coach corner. (#98)

## [0.6.24] - 2026-05-03
### Changed
- Unified all logos to one oil-paint look across small and large sizes. (#96)

## [0.6.23] - 2026-05-03
### Changed
- Landing eyebrows: dropped tracked-caps, switched to italic Cormorant. (#94)

## [0.6.22] - 2026-05-03
### Changed
- Logo gets oil-paint texture; "Sage" wordmark in Cormorant Garamond italic. (#92)

## [0.6.21] - 2026-05-03
### Changed
- Landing page is the entry for everyone; CTAs adapt to session. (#90)

## [0.6.20] - 2026-05-03
### Added
- Public landing page at `/` with scroll-reveal animations. (#88)

## [0.6.19] - 2026-05-03
### Changed
- Logo refined: solid asymmetric leaf with negative-space midrib. (#86)

## [0.6.18] - 2026-05-03
### Changed
- Rebrand: Good Food → Sage. Salad emoji → SVG sage-leaf. (#84)

## [0.6.17] - 2026-05-03
### Changed
- Sidebar sticks to viewport on long pages. (#82)

## [0.6.16] - 2026-05-03
### Changed
- Weekly calorie chart: 12 px pill bars with empty / over / today states. (#80)

## [0.6.15] - 2026-05-03
### Changed
- Goals page stacks vertically: targets on top, weekly chart full-width below. (#78)

## [0.6.14] - 2026-05-02
### Fixed
- Dark mode: brand tones flip; scrim + emoji halo tokenized. (#71)

## [0.6.13] - 2026-05-02
### Added
- Recipe → author chat shortcut on the recipe detail page. (#68)
### Fixed
- Universal `prefers-reduced-motion: reduce` clamps all motion globally.

## [0.6.12] - 2026-05-02
### Changed
- Goals chart promoted to 60% of the row; week navigation; profile fav cards as thumbnails. (#66)

## [0.6.11] - 2026-05-02
### Added
- Motion pass: stagger reveals, ring fill, count-up, one-shot shimmer. (#64)

## [0.6.10] - 2026-05-02
### Fixed
- A11y / perf audit: focus-visible, skip-link, image dimensions. (#62)

## [0.6.9] - 2026-05-02
### Fixed
- Demo polish: today-anchor seeds, "Not rated yet" copy, empty-day CTA. (#60)

## [0.6.8] - 2026-05-02
### Added
- 6 more recipes seeded into the catalogue. (#58)

## [0.6.7] - 2026-05-02
### Changed
- UI tier-up: unified 14 px radius family, larger card padding, mint accent. (#56)

## [0.6.6] - 2026-05-02
### Added
- Real Unsplash cover photos on recipes; per-serving nutrition; Featured strip. (#54)

## [0.6.5] - 2026-05-02
### Changed
- Final polish: macro typography, date prominence, filter row, sidebar tone. (#52)

## [0.6.4] - 2026-05-02
### Changed
- Dashboard polish: calorie ring centrepiece, thicker macro bars, warmer empty states. (#50)

## [0.6.3] - 2026-05-01
### Fixed
- UI bugs: trailing `.00` in numbers, raw ISO timestamps in chat, bare recipe cards. (#46)

## [0.6.2] - 2026-05-01
### Fixed
- Render free tier wiped H2 on every redeploy. Switched to PostgreSQL when `DATABASE_URL` is present. (#44)

## [0.6.1] - 2026-05-01
### Fixed
- Render Docker build: bumped Gradle base image 7.6 → 8.5 to match the project wrapper.

## [0.6.0] - 2026-05-01
### Changed
- Warm wellness redesign: cream / sage / terracotta palette, Plus Jakarta Sans, soft rounded geometry.
### Added
- Animated login background — drifting sage and terracotta tints. (#40)

## [0.5.1] - 2026-05-01
### Changed
- Diary "Add food" search: inline hint + minimum query length bumped 2 → 3. (#38)

## [0.5.0] - 2026-05-01
### Changed
- Visual identity refresh inspired by Anthropic: ivory base, slate text, clay accent, Inter + Playfair Display + JetBrains Mono.

## [0.4.7] - 2026-05-01
### Added
- Round-1 UX-test report (`UX_TESTING.md`). (#31)
- Integration tests + security regression tests. (#32, #33)
- `DESIGN_DECISIONS.md`. (#34)

## [0.4.6] - 2026-05-01
### Added
- Detekt static analysis in CI. (#25)
- Class diagram, KDoc on services, user stories, accessibility audit. (#26, #27, #28, #29)

## [0.4.5] - 2026-05-01
### Added
- GitHub Actions `build-and-test` workflow. (#21)
- Acceptance-criteria headers on every test file. (#22)
### Changed
- README pointer: `ui-prototype/` → `UI_wireframes.md`. (#23)

## [0.4.4] - 2026-04-30
### Fixed
- IDOR on professional client view + advice endpoint. (#16, #17)
- Session cookie `HttpOnly` and `SameSite=Lax`. (#18)
- LIKE wildcard injection in food / recipe search. (#19)

## [0.4.3] - 2026-04-29
### Added
- Cursor acknowledgment backfilled in `AI_USAGE.md`.

## [0.4.2] - 2026-04-29
### Added
- `AI_USAGE.md` log; AI-acknowledgment headers in `styles.css` and `app.js`.

## [0.4.1] - 2026-04-29
### Added
- `.devcontainer/devcontainer.json` for GitHub Codespaces / VS Code Dev Containers.

## [0.4.0] - 2026-04-29
### Added
- Dark mode (auto / light / dark) with persisted user choice.
- Mobile drawer sidebar below 840 px.
- Component polish: hover lift, animated bars, modal scale-in, toast region, skeleton loader.
- `:focus-visible` ring + `prefers-reduced-motion` support.

## [0.3.0] - 2026-03-24
### Changed
- Restructured Kotlin from layer-based to feature-based modules.

## [0.2.0] - 2026-03-24
### Added
- Full Ktor backend: 12 tables, 11 Thymeleaf templates, BCrypt auth, subscriber + professional features.

## [0.1.0] - 2026-03-24
### Added
- Initial design phase: ER diagram, UI wireframes, HTML/CSS prototype.
</content>
</invoke>