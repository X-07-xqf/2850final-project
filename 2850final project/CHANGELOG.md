# Changelog - Sage — Healthy Eating

All notable changes to this project will be documented in this file.

---

## [v0.6.31] - 2026-05-04 — Pro Clients: show ALL subscribers + enrich detail page (closes #109)

### Changed
- `/pro/dashboard` (the Clients overview) — was joining `ClientRelationships` and only showing supervised subscribers. Now queries `Users.role = 'subscriber'` directly and lists every registered subscriber, ordered by full name. Today's calories / goal / compliance / status are computed per row exactly as before, so the existing table markup stays unchanged.
- `/pro/client/{id}` — dropped the `hasActiveRelationship` gate. A defence-in-depth check is kept (the URL must point at a row whose `role = 'subscriber'`), so pros can't scrape pro-on-pro detail pages by guessing IDs.
- `POST /pro/client/{id}/advice` — gate also dropped.
- `hasActiveRelationship` function kept in code (annotated `@Suppress("unused")`) so the stricter access model can be re-wired later without re-implementing the relationship lookup.

### Added (client detail page)
- **Profile sub-line** under the page title showing the client's email + capitalised role + month they joined ("alice@…  · subscriber · Joined March 2026").
- **"Open chat →"** button alongside the date nav, linking to `/messages/{id}` so the pro can pop straight into the conversation thread instead of using the inline advice form.
- **"This week" card** — 7-day calorie ladder for the client using the same `.dashboard-week__chart` markup the subscriber dashboard uses. Today's row gets the mint pill, days with no entries show a dashed track. Reuses existing CSS — zero new styling for the chart itself.

### Implementation notes
- Two new CSS rules (`.client-detail__meta` + `.client-detail__actions`) for the profile sub-line and action-button row layout. Everything else is reused.
- Date nav buttons in the header switched to `btn--small` so the "Open chat →" button fits next to them without wrapping at typical desktop widths.

### Security trade-off (deliberate)
- Earlier issues #16 and #17 added the `hasActiveRelationship` gate against IDOR — any pro reading any client's data. Per the v0.6.31 product request, that gate is removed: any registered professional can read any subscriber's diary and message them. The reduced-friction model fits the small-team / classroom context this project targets; if the threat model changes the gate can be re-enabled by uncommenting the call sites.

---

## [v0.6.30] - 2026-05-03 — Messages: anyone can DM anyone — full directory of opposite-role users (closes #107)

### Added
- **Directory section** in both subscriber and professional message sidebars listing every user of the opposite role you haven't yet exchanged messages with. Subscribers see "All professionals", professionals see "All clients". Conversations you've already started stay at the top in their own section; the directory sits below the divider.
- New `MessageService.getEligibleNewPartners(userId, currentUserRole)` returns users with the opposite `role`, excluding the current user and excluding the IDs already in the conversation history.

### Changed
- `MessageRoutes` (both `GET /messages` and `GET /messages/{partnerId}`) now pass `directory` in addition to the existing `partners`.
- Subscriber + professional message templates updated with the new section. Empty-state copy refreshed: `No conversations yet — start one below.` (subscriber) / `No conversations yet — pick a client below to start one.` (professional).
- Directory rows reuse the existing `.conv-list__row` markup so the search filter and `data-name` attribute keep working across both sections without any JS change.
- Directory items get a subtle `+` glyph in a dashed circle (replaces the unread badge slot) so they read as "start a chat" affordances.

### Implementation notes
- `GET /messages/{partnerId}` already worked for never-messaged partners (it fetches the partner via `UserService.getById` and `MessageService.getConversation` returns an empty list cleanly), so no route changes were needed beyond passing the directory list.
- Initials computed the same way as for conversation partners (first letter of each space-separated word) so avatars render consistently between the two sidebar sections.
- The directory section's `.conv-list__divider` header sits below the conversations list with `margin: 18px 16px 6px` so the two sections feel related but distinct.

### Out of scope
- Subscriber-to-subscriber DMs and pro-to-pro DMs (the requirement was "professionals talk to clients, clients talk to professionals", not full social networking).
- Authorization gating beyond the existing pro-only `hasActiveRelationship` check on the professional dashboard endpoints — basic messaging stays open to any logged-in user-pair.

---

## [v0.6.29] - 2026-05-03 — Dashboard "For tonight" cards now show real recipe images (closes #105)

### Fixed
- The 3 seed recipes (`Grilled Chicken Salad` / `Overnight Oats Bowl` / `Grilled Salmon with Veggies`) all have real Unsplash `imageUrl` values seeded by `SeedData.backfillImageUrls()`, but the dashboard's "For tonight" section was rendering the emoji fallback every time because `DashboardRoutes.kt`'s `featured` map projection silently dropped the `imageUrl` field.
- Added `imageUrl` to the controller's projection (passed through as nullable so the template can fall through to the emoji when a recipe genuinely has no image).
- Mirrored the `recipes.html` pattern in `subscriber/dashboard.html`: `<img th:if="${f.imageUrl != null}">` first, emoji span as the fallback. Cover gradient class only applied when there's no image (so the image isn't tinted by a sage/clay/berry overlay).
- New `.dashboard-tonight__img` CSS rule: `object-fit: cover` + `width/height: 100%` + a subtle `scale(1.04)` zoom on card hover, matching the existing `.recipe-card__cover-img` behaviour on the recipes grid.

---

## [v0.6.28] - 2026-05-03 — Liquid Glass pass — translucent cards + atmospheric backdrop (closes #103)

CSS-only rendition of Apple's Tahoe / iOS 26 Liquid Glass material. Reference: [rdev/liquid-glass-react](https://github.com/rdev/liquid-glass-react) (which is React + WebGL — we hit ~95% of the visual via `backdrop-filter` + multi-layer shadows + an atmospheric backdrop layer).

### Added
- **Liquid-glass token family** in all three `:root` variants (light, `[data-theme="dark"]`, `prefers-color-scheme: dark`):
  - `--glass-bg / --glass-bg-warm / --glass-bg-tinted` — translucent surface tints (~58–62% alpha)
  - `--glass-blur / --glass-blur-strong` — `saturate(180%) blur(20-28px)`
  - `--glass-border / --glass-border-soft` — thin edge highlight (white in light, faint white in dark)
  - `--glass-shadow / --glass-shadow-strong` — multi-layer recipe: top `inset 0 1px 0` highlight + bottom `inset 0 -1px 0` shadow + soft outer drop
- **Atmospheric backdrop layer** at `.app-body::before` — three soft radial-gradient blobs (sage / clay / berry) fixed to the viewport. Subtle (≤30% peak opacity in light, ≤42% in dark) so they don't compete with content but provide colour for the glass surfaces above to refract. Without this layer, glass cards on a flat cream page would just look like translucent rectangles.

### Changed
- `.card` (every card, 69 placements across the app) — solid `var(--color-surface)` swapped to `var(--glass-bg-warm)` with `backdrop-filter: var(--glass-blur)`, edge highlight via `var(--glass-border)`, multi-layer `var(--glass-shadow)`. Cards now read as thick translucent material with light catching the top edge.
- `.chat-main__head` — sticky chat header is the canonical glass use case (refracts the messages scrolling beneath it). Now uses `--glass-blur-strong` plus a single `inset 0 1px 0 rgba(255,255,255,0.6)` highlight stripe along the top edge.
- `.modal__panel` — strong-blur glass panel over the existing scrim, so the page behind reads as an impressionistic backdrop instead of detail noise.
- `.landing-hero__metric` — the three floating metric chips (`1,842 kcal today` / `124 g protein` / `187 recipes saved`) are now glass tiles letting the sage brand-disc colour bleed through.

### Not changed
- Buttons (`.btn`), the composer textarea, sidebar nav, message bubbles, and `.card--feature-dark` keep their solid backgrounds — tactile / readability surfaces where translucency would hurt.
- All `-webkit-backdrop-filter` mirrors added everywhere `backdrop-filter` appears, for Safari.

### Implementation notes
- `app-body::before` sits at `z-index: 0`; `.app-layout` is bumped to `z-index: 1` so the layout (cards included) renders above the blobs. `.app-body` itself stays at the cream background.
- Hover transforms on cards (`.recipe-card:hover { transform: translateY(-2px) }` etc.) create a stacking context that briefly disables `backdrop-filter` during the hover. Acceptable trade-off — solid hover state is fine, the glass settles back when not interacting.
- Total CSS added: one `::before` block + ~30 lines of token declarations across three `:root` variants. No new HTML structure, no new classes applied to templates — every existing `.card` automatically inherits the new look.

---

## [v0.6.27] - 2026-05-03 — Fluid main width — pages no longer cap at 1200px on wide displays (closes #101)

### Changed
- `.main` `max-width: 1200px` → `1680px`. Every authenticated page used to stop ~30-40% short of the viewport on a 1440 / 1920 / 2560+ display. Now scales out 40% wider while still capping for readability on 4K monitors.
- `.main` padding moved from a static `32px 36px` to `clamp(28px, 3vh, 40px) clamp(28px, 4vw, 72px)` so the breathing room scales with the viewport — tighter on tablets, more generous on widescreens, capped both ways.
- `.main--chat` matches the same `1680px` cap (was `1280px`). The chat self-constrains via its conversation-rail width + bubble `max-width: 72%`, so it can ride the wider container without bubbles spanning awkwardly far.

### Not changed
- All sub-layouts inside `.main` already use `width: 100%` cards, `auto-fill` grids, or `fr` units — they grow naturally with the wider container without any additional changes. Nothing visually breaks; pages just use the screen they're given.
- Mobile / tablet overrides (`@media (max-width: 1024px)`, `840px`, `600px`) untouched. They still pin to tighter padding for those viewports.

---

## [v0.6.26] - 2026-05-03 — Messages: Telegram-style polish + smooth interactions (closes #99)

### Added (visual)
- **Sticky chat header** with partner avatar + name + role + "Active recently" status line. Replaces the bare strip that just showed a name.
- **Search input** at the top of the conversation list — JS-side `data-name` substring filter on `[.conv-list__row]`, with an empty-state hint when no rows match.
- **Bubble tails** — small CSS pseudo-element triangles pointing to the sender's bottom corner (Telegram signature). No extra SVG; pure `clip-path: polygon(...)`.
- **Date separators** — sticky `Today / Yesterday / Monday / MMM d, yyyy` pills between message clusters. `MessageRoutes` groups the conversation list by `sentDate`; the template renders `[.chat-day][.chat-day__sep]` blocks. Pills sit on `var(--overlay-strong)` and stick at `top: 8px` while scrolling.
- **Online dots** on every avatar — small mint disc with a 2px disc-coloured border so it reads cleanly on hover/active states. (Visual cue only — no presence tracking.)
- **Dotted wallpaper** in the chat-scroll background: `radial-gradient(circle at 1px 1px, ...) 22px 22px` over the existing chat gradient, like Telegram's wallpaper layer.
- **Round pill composer input** + **circular filled send button** with paper-plane SVG. Replaces the rectangular textarea + button combo.
- **Active conversation** in the list now flips to the sage-primary background with cream text and an inverted avatar; the unread pill flips colors too. More legible than the previous subtle tint.

### Added (smooth interactions, all in `initChatPage()` in `app.js`)
- **Auto-scroll to bottom** on page load (no smooth, instant — avoids the visible top-then-jump).
- **Auto-resize composer textarea** as the user types (1 row → up to 140px, capped).
- **Send on Enter** (`Shift+Enter` newline). Send button disabled when input is empty.
- **Optimistic AJAX submit** — `fetch(POST, FormData)` instead of native form submit. The bubble appends locally with a `bubble--pending` class showing "sending…" until the server response lands; on failure it flips to `bubble--failed`. Page no longer reloads on send. The optimistic bubble lives inside the latest `.chat-day` group, or creates a fresh "Today" group if the thread was empty.
- **Composer auto-focus** on page load so a returning user can just start typing.

### Changed
- `MessageService.getConversation` now returns `sentDate` (`LocalDate`) and `sentTime` (`HH:mm`) per message in addition to the existing `sentAt` formatted string. Used by the date-grouper and the new bubble timestamps.
- `MessageRoutes` exposes `conversationGroups` (date-grouped) + `activePartnerRole` to both subscriber and professional templates.

### Implementation notes
- All polish reuses existing tokens: `--color-primary`, `--bubble-them-bg`, `--overlay-strong`, `--color-mint`, `--color-cream`. Dark mode flips automatically — no separate dark CSS.
- Date-group separator pill takes `--overlay-strong` (forest in light, near-black in dark) which already adapts.
- `requestSubmit()` triggers the form's submit handler so all the AJAX logic stays on `submit`, not an explicit button click — keyboard users (Enter) and mouse users hit the same code path.
- Sticky header uses `position: sticky; top: 0` inside the chat-main flex column. Chat-day separators stick within the scroll container.

### Subscriber + Professional both updated
- `subscriber/messages.html` and `professional/messages.html` both refactored to the same new structure. Two slightly different copy strings ("Search…" vs "Search clients…", empty-state copy) preserved.

---

## [v0.6.25] - 2026-05-03 — Dashboard: This week + Streak + For tonight + Coach corner (closes #97)

### Added
- **This week / Streak** asymmetric split row (`2fr / 1fr`) below the existing meals/empty-day section.
  - This week: compact 7-row weekly chart with sage gradient bars, today's row highlighted in `--color-mint-bg`, empty days show a dashed transparent track. Uses the existing `DiaryService.getWeeklySummary` snapshot.
  - Streak: italic Cormorant `<loggedDays> / 7` display number with a count-up animation, plus a row of 7 dots showing which days were logged. The italic display number ties back to the brand wordmark voice instead of yet another bold sans number.
- **For tonight** asymmetric recipe bento (`grid-template-columns: 2fr 1fr 1fr`). Picks 3 recipes via `RecipeService.getFeatured(3)`. The lead card shows a larger emoji cover + difficulty meta; the two trailing cards are compact. Avoids the taste-skill "3 equal cards in a row" ban via the asymmetric grid. Cards reuse the existing `recipe-card__cover--*` tone gradients so dark mode flips automatically.
- **Coach corner**: latest conversation partner via `MessageService.getConversationPartners(userId).firstOrNull()` — initials disc + name + role + truncated last message + Reply CTA. Empty-state variant when the user has no conversations yet, inviting them to open Messages.

### Changed
- `DashboardRoutes.kt` enriched: computes `weekly` (Mon–Sun calorie ladder with `pct`/`isToday`/`isLogged` flags), `loggedDays` count, `featured` (3 top recipes from `RecipeService`), and `latestPartner` (first conversation in the user's inbox).

### Implementation notes
- Mobile breakpoints: at `≤960px` the week/streak split collapses to a single column and the recipe bento goes to 2 columns with the lead card spanning both. At `≤600px` everything stacks to single column and the coach Reply button moves below the message.
- All numbers use `var(--font-mono)` + `tabular-nums` (week chart values) or italic Cormorant (streak count) — no Plus Jakarta Sans bold for data.

---

## [v0.6.24] - 2026-05-03 — Unify all logos to one oil-paint look, visible at small sizes too (closes #95)

### Changed
- Every logo placement now uses the **same** filter chain — `feTurbulence(baseFrequency: 0.85, numOctaves: 3, seed: 7)` → `feDisplacementMap(scale: 0.7)`. Previously the small variant ran `octaves: 2` / `seed: 5` and the hero ran `baseFrequency: 1.1, scale: 0.5`; the painterly effect read inconsistently, so we collapsed to one set.
- **Small-size SVGs bumped up** so the brushstroke noise actually has pixels to render: sidebar logo `18px → 22px` inside its 32px disc, auth login logo `26px → 32px` inside its 48px disc. (Landing nav stays at 22px.) feDisplacementMap of `scale: 0.7` viewBox units = ~0.5px on screen at 18px (sub-pixel, invisible) but ~0.65px at 22px and ~0.93px at 32px (visible brushy edges).
- Filter IDs cleaned up: small logos use `paint` / `grad`; landing hero uses `paintHero` / `gradHero` — only because both live in the same DOM, the params themselves are identical so the leaves render with the same texture density at their respective sizes.

---

## [v0.6.23] - 2026-05-03 — Eyebrows: drop tracked-caps AI pattern, italic Cormorant editorial (closes #93)

### Changed
- `.landing-hero__eyebrow` and `.landing-section__eyebrow` no longer use the `text-transform: uppercase; letter-spacing: 0.32em; font-size: 12px` recipe — that all-caps + wide-tracking + brief-tagline-in-eyebrow pattern is the Lovable / Bolt / v0.dev default and reads as AI-template at a glance.
- Both classes now use `var(--font-display)` (Cormorant Garamond) at `italic 500`, natural case, no extra letter-spacing, larger size (`clamp(17px, 1.5vw, 20px)` hero / `clamp(16px, 1.4vw, 19px)` section) — serifs need the size and don't survive tracked-out caps. Reads as British-editorial subhead instead of marketing eyebrow.
- Hero eyebrow copy: `SAGE — HEALTHY EATING, TRACKED SIMPLY` → `Healthy eating, tracked simply.` Drops the redundant brand prefix (the wordmark is right there in the nav above).
- Section eyebrows: `Track` / `Cook` / `Coach` → `Track.` / `Cook.` / `Coach.` Period gives single-word eyebrows editorial weight.

### Not changed
- No new font added. Cormorant Garamond is already imported (v0.6.22).
- All other typography (UI body, dashboard, mono numbers, sidebar wordmark) untouched.

---

## [v0.6.22] - 2026-05-03 — Logo oil-paint texture; "Sage" wordmark in Cormorant Garamond italic (closes #91)

### Changed
- **Logo painterly pass.** All 13 logo instances now wrap their leaf paths in a `<filter>` chain — `feTurbulence` (fractal noise, `baseFrequency: 0.85`, 2 octaves, `seed: 5`) feeding `feDisplacementMap` (`scale: 0.7`) — so path edges read as brush-flicked rather than CAD-clean. The leaf body fill switches from flat `currentColor` to a 3-stop `linearGradient` (`stop-opacity: 0.92 / 1 / 0.82`) so the pigment looks layered. The `var(--color-primary)` negative-space midrib survives the texture pass — brand wit intact.
- The landing hero leaf gets its own `id="paintLg"` / `id="gradLg"` defs (slightly tighter `baseFrequency: 1.1`, `numOctaves: 3`, smaller `scale: 0.5`) because at large display sizes the same noise-frequency would over-distort. No ID collision with the smaller nav SVG above it.
- **Wordmark typography flip.** Added Cormorant Garamond (Google Fonts) — a Garamond-tradition revival with the British book-typography lineage (Caslon → Baskerville → Penguin Classics → Vogue / Tatler mastheads). New `--font-display` token applied to:
  - `.sidebar__title` — `italic 600` at 22px in every sidebar
  - `.auth-title` — `italic 600` at 44px on the login disc
  - `.landing-nav__wordmark` — `italic 600` at 26px in the top nav
  - `.landing-hero__title-accent` — the "intention." accent word in the hero headline (the rest of "Eat with" stays in Plus Jakarta Sans), creating the classic British-editorial sans-with-one-italic-serif-word treatment
- Body and dashboard typography unchanged — Plus Jakarta Sans still drives all UI text. Serif is brand-only, per taste-skill's "NEVER serif on dashboards" rule (which targets body/UI text, not wordmarks).

---

## [v0.6.21] - 2026-05-03 — Landing at / for ALL users with session-aware CTAs (closes #89)

### Changed
- `Routing.kt`: `get("/")` no longer redirects authenticated users to their dashboard — the landing page is now the entry point for everyone. The session is passed through to the template so CTAs adapt.
- `landing.html`: every CTA cluster (top nav, hero, closing CTA band) splits into a `${session == null}` branch and a `${session != null}` branch via `<th:block>`.
  - Signed-out: `Sign in` / `Start free`, `Start free` / `I have an account`, `Create your account` / `Sign in` (unchanged copy).
  - Signed-in: `Go to dashboard` / `Sign out` everywhere; the closing band's headline becomes `Welcome back, <fullName>.` with a `Pick up where you left off.` subtitle pointing into the app.
- The pro/subscriber dashboard distinction is preserved — every "Go to dashboard" link uses `${session.role == 'professional'} ? '/pro/dashboard' : '/dashboard'` so practitioners still land in their portal.

---

## [v0.6.20] - 2026-05-03 — Public landing page at / with scroll-reveal animations (closes #87)

### Added
- New `templates/landing.html` served at `/` for unauthenticated visitors. Authenticated users still go straight to their dashboard. Replaces the previous behavior of dumping new visitors on a bare login form.
- **Asymmetric hero** (taste-skill rule: no centered hero when `DESIGN_VARIANCE > 4`): split-screen with `1.1fr 1fr` grid; left column carries the headline + sub + dual CTAs ("Start free" / "I have an account"); right column shows the brand disc with three floating metric chips (`1,842 kcal today` / `124 g protein` / `187 recipes saved`) — each chip drifts on a perpetual `hero-metric-float` keyframe with 6s alternating easing.
- **Three zig-zag feature sections** (Track / Cook / Coach) — section 2 reverses via `.landing-section--reverse` so visuals alternate sides; type-driven CSS-only mocks instead of stock photography (a calorie ring with macro bars / a stacked recipe-card vignette / a mock chat thread with breathing typing-dots).
- **Closing CTA band** in `--color-deep` (forest) with `Create your account` / `Sign in` buttons, then a minimal footer.
- **IntersectionObserver scroll reveals**: every `[data-reveal]` element gets `opacity: 0; transform: translateY(28px)` until it crosses 15% of the viewport, then transitions in over 0.8s with `cubic-bezier(0.16, 1, 0.3, 1)` easing and an `--i`-driven 90ms stagger. Falls back to instant visibility when `prefers-reduced-motion: reduce` or `IntersectionObserver` is unsupported.

### Changed
- `Routing.kt`'s root `get("/")` now renders the landing template directly for unauthenticated users (instead of `respondRedirect("/login")`); authenticated branch unchanged.
- `initCountUp()` in `app.js` now prefers a `data-count-up="..."` attribute value over text content, strips thousands-separator commas before parsing, and re-adds them via `toLocaleString("en-US")` if the source had them — backward compatible with the existing dashboard usage that sets the value via text content.
- New `initScrollReveals()` function added to `app.js`'s init sequence.

---

## [v0.6.19] - 2026-05-03 — Logo refinement: solid asymmetric leaf with negative-space midrib (closes #85)

### Changed
- Replaced the v0.6.18 hollow-outline + centered-midrib leaf (the LLM-default "minimal leaf" archetype, fragile at 18px) with a solid asymmetric silhouette + a negative-space cut for the midrib. Two-path SVG, single asset, used at all 12 logo placements (8 subscriber sidebars + 3 professional sidebars + 1 auth disc).
- **Body** is filled with `currentColor` so it inherits the disc's foreground (cream in light, dark forest in dark) — substantial at 18px instead of wispy.
- **Midrib** is a stroke painted with `var(--color-primary)` — i.e. the disc's own background color — so the cut visually punches through the leaf and reveals the disc behind. Same SVG, no extra assets, but a FedEx-arrow-tier negative-space wit moment.
- The leaf silhouette is intentionally asymmetric: denser curvature on the bottom-left, sharper terminus on the upper-right. Reads as a sage leaf with organic visual weight rather than a generic teardrop.

---

## [v0.6.18] - 2026-05-03 — Rebrand: Good Food → Sage, salad emoji → SVG sage-leaf mark (closes #83)

### Changed
- Product name renamed from "Good Food" to **Sage** across every user-facing string: 11 page `<title>`s, sidebar brand text in 8 templates, the auth login title. Rationale: the brand color is already `--color-sage-deep / --color-sage / --color-sage-bg` so the name and visual identity collapse into one cohesive thing; "sage" is a culinary herb (food-coded) and means "wise" (fits the "make smarter food choices" positioning); one syllable, real word, premium minimal feel.
- Logo `🥗` salad emoji replaced with an inline-SVG sage-leaf mark — a single confident curve plus midrib stroke, `stroke="currentColor"` so it inherits the brand-disc foreground color and flips correctly in dark mode. 18px in the 32px sidebar disc, 26px in the 48px auth-page disc.
- The professional sidebar's `📋` clipboard emoji is also replaced with the same Sage leaf so the brand mark is consistent across consumer + practitioner surfaces; "Pro portal" is kept as a contextual subtitle so practitioners still know which mode they're in.
- `.auth-logo` switched from text-centering (`line-height: 48px; text-align: center; font-size: 22px`) to flex-centering (`inline-flex` + `align-items: center` + `justify-content: center`) so the SVG centers cleanly without leftover text-rendering metrics.
- File-header doc comments in `styles.css`, `app.js`, and `UserService.kt` updated for consistency. `README.md` and the CHANGELOG title line updated to the new brand.
- Kotlin package `com.goodfood.*` is unchanged — this is a brand refresh, not an architectural refactor.

---

## [v0.6.17] - 2026-05-03 — Sidebar sticks to the viewport on long pages (closes #81)

### Changed
- `.sidebar` is now `position: sticky; top: 0; height: 100vh` on desktop so long pages (recipes, diary) only scroll the main column — the nav stays pinned. `align-self: flex-start` keeps the flex parent from stretching the sidebar to match `.main`'s height, and `overflow-y: auto` lets unusually short viewports scroll within the sidebar instead of clipping the logout button.
- The `≤840px` mobile drawer continues to use `position: fixed` (its own media-query rule wins), so no regression for the slide-in mobile menu.

---

## [v0.6.16] - 2026-05-03 — Weekly calorie chart: substantive bars with empty / over / today states (closes #79)

### Changed
- The `<progress>`-based weekly chart on `/goals` was anemic 6px line art that didn't distinguish "logged nothing" from "logged a tiny meal" or flag over-goal days. Replaced with a div-based 12px pill bar (`.weekly-chart__track` + `.weekly-chart__fill`) carrying four meaningful row states.
- `GoalRoutes.kt` now enriches each weekly row with `pct` (capped 0–100), `rowClass` (a space-prefixed string of `--empty` / `--over` / `--today` modifiers), and uses the calorie goal as the bar's natural max instead of a hardcoded 3000.
- **Default**: sage-deep → sage gradient fill, scaled to `(calories / goalCalories) * 100` (capped at 100%).
- **Empty** (`0 kcal`): track switches to a 1px dashed border so "no data" is visually distinct from "almost-empty"; calorie number softens to `--color-faint`.
- **Over goal** (>110% of target — 10% buffer so right-around-goal stays sage): fill flips to a clay/warn gradient.
- **Today**: row gets a soft mint pill background, day name promoted to sage-deep / weight 700 so users can see where they are in the week at a glance.
- Calorie numbers now use `var(--font-mono)` + `font-variant-numeric: tabular-nums` so the column lines up vertically; small uppercase `kcal` unit label sits next to each value.
- New `bar-grow` keyframe (`scaleX(0) → scaleX(1)`, `--ease-out`, 0.7s, staggered `70ms × var(--i)` with a 0.2s page settle) gated behind `prefers-reduced-motion: no-preference`.

---

## [v0.6.15] - 2026-05-03 — Goals page stacks vertically: targets on top, weekly chart full-width below (closes #77)

### Changed
- `.two-col--goals` is now a single full-width column at all breakpoints. The previous `2fr 3fr` split left a tall narrow form on the left with dead space below and squeezed the weekly bars into 60% of the row.
- Inside the Daily targets card, the 5 macro inputs (calories / protein / carbs / fat / fiber) now lay out as a horizontal grid (`.goals-form__fields`): 5-col on desktop, 3-col `<960px`, 2-col `<640px`, 1-col `<420px`. Form card stays short instead of stretching down by 5 stacked fields.
- Save button (`.goals-form__submit`) aligns to start so it doesn't stretch full-width across the now-wide form card.
- Dropped the obsolete `order: -1` mobile swap — natural document order (form first, chart below) is now what we want at every viewport.

---

## [v0.6.14] - 2026-05-02 — Dark-mode pass: brand tones flip, scrim + emoji halo tokenized (closes #70)

### Fixed
- Recipe-card covers (`--sage`, `--oat`, `--clay`, `--berry`) and progress-bar fills no longer read as floating light pastels on a dark page. The brand "soft" tones (`--color-sage-bg/soft/deep`, `--color-clay-bg/soft`, `--color-berry-soft`, `--color-cream-warm`, `--color-oat`) were only declared in the light `:root` and silently fell back in dark; now defined in both `:root[data-theme="dark"]` and the `prefers-color-scheme: dark` media block with darker tone-shifted equivalents.
- Featured-card badge (`.featured-card__badge`) was hardcoded `rgba(31, 42, 35, 0.78)` — a forest-tinted scrim that became invisible on the now-dark recipe cover gradients. Promoted to `--overlay-strong` (forest in light, near-black in dark).
- Modal backdrop and mobile-sidebar backdrop both used the same hardcoded `rgba(31, 42, 35, 0.45)`. Promoted to `--overlay-modal` and lightened to pure-black-at-55% in dark mode so the page-already-dark doesn't double-tint.
- Recipe-card emoji drop-shadow (`drop-shadow(... rgba(31, 42, 35, 0.12))`) was forest-tinted and disappeared on dark covers. Promoted to `--emoji-shadow` (stronger pure-black in dark).
- Once-only progress-bar shimmer was a hardcoded `rgba(255, 255, 255, 0.55)` white sweep — too cold/bright on a warm dark UI. Promoted to `--shimmer-highlight` (cream-at-22% in dark).
- Berry recipe-card gradient endpoint was a hardcoded `#f3c4cf`. Promoted to `--color-berry-tint` so the gradient tone-shifts with the rest of the card in dark mode.
- Sidebar theme-toggle hover border was hardcoded cream-at-20%. Promoted to `--sidebar-border-hover` for parity (still cream in light, soft cream in dark — the sidebar is dark in both modes, so this is mostly token hygiene).

---

## [v0.6.13] - 2026-05-02 — Strategy pass: recipe→author chat, mint earns its keep, reduced-motion guard (closes #68)

### Added
- Recipe detail now shows a mint-tinted "Message *Dr. Sarah Williams* →" pill under the rating row, linking to `/messages/<authorId>`. Hidden when the viewing user is the author. `RecipeService.getRecipeDetail` returns `authorId` + `authorName` so the template doesn't have to look them up.
- Mint accent now reads as a system, not an orphan token: Featured strip underline (v0.6.7) + Goals "This week" pill (v0.6.12) + Dashboard empty-day CTA gradient (v0.6.9) + new recipe→author chat button.

### Fixed
- Universal `prefers-reduced-motion: reduce` block clamps all animations / transitions to 0.01ms and disables continuous loops (login background blobs, progress-bar shimmer). Backstop to the per-feature gating already in place since v0.6.11.

---

## [v0.6.12] - 2026-05-02 — Layout pass: Goals chart promotion, week nav, profile fav cards, login toggle anchor (closes #66)

### Changed
- `.two-col--goals` now uses `grid-template-columns: 2fr 3fr` so the weekly chart card takes 60% of the row vs the form's 40%; on `<720px` the layout stacks chart-first.
- New `weekly-head` block in the chart card with the week label (e.g. `May 4 – May 10, 2026`) and a compact `← / This week / →` nav. `GoalRoutes` accepts `?week=YYYY-MM-DD` and snaps any non-Monday to its Monday; `DiaryService` gains a `getWeeklySummary(userId, monday)` overload.
- Profile favourites render as `.fav-card` rows with a 80×80 cover thumbnail (real Unsplash for seed recipes, emoji-on-tone fallback otherwise) plus title + difficulty + rating; "Not rated yet" replaces `★ 0.0` for unrated.
- Login dark/light theme toggle moved out of `position:fixed` page chrome and into the auth card itself (top-right corner, pill outline) — reads as part of the form, not a stray dev button.

---

## [v0.6.11] - 2026-05-02 — Motion pass: stagger reveal, ring fill, count-up, shimmer (closes #64)

### Added
- Stagger reveal on Dashboard macro rows + Recipes grid via `@keyframes stagger-rise` and `--i` index passed inline; 60ms delay per item, ~420ms duration, spring `cubic-bezier(0.16, 1, 0.3, 1)`.
- Calorie ring fills from 0% to target on mount via `@property --ring-pct` (registered custom property) + 0.85s spring transition.
- `initCountUp()` in `app.js`: `[data-count-up]` numeric elements tween from 0 to their final value over 700ms with ease-out cubic; preserves integer / single-decimal formatting from the rendered text.
- One-shot shimmer pass on the calorie / protein / carbs / fat progress bars 0.9s after page settle — fires once, never loops.
- All four motion features gated under `@media (prefers-reduced-motion: no-preference)`; `initCountUp()` early-returns when the user prefers reduced motion.

### Notes
- Hover transitions across cards / buttons already used the spring `--ease-out` curve from earlier passes; no change needed for that bullet.

---

## [v0.6.10] - 2026-05-02 — A11y / perf audit: focus, skip-link, image dims (closes #62)

### Fixed
- Global `:focus-visible` now pairs the soft `--ring` box-shadow with a 2px solid `--color-primary` outline + offset, so keyboard focus is unambiguously visible across themes and browsers.
- Skip-to-main-content link added to every authenticated app shell template (subscriber + professional, 10 templates) and revealed on focus; `<main>` carries `id="main"` as the target.
- Recipe cover `<img>` tags now declare `width="800" height="500"` for explicit aspect ratio, preventing CLS while the image loads.
- Confirmed already-in-place: `font-display: swap` (Google Fonts URL), `role="alert"` on login errors, and implicit `<label>`-wrap association on every form. No change needed; documented for the audit trail.

---

## [v0.6.9] - 2026-05-02 — Demo polish: today-anchor seeds, unrated rating, empty-day CTA (closes #60)

### Fixed
- New idempotent `SeedData.ensureAliceHasTodayEntries()` keeps the demo account populated for `LocalDate.now()` on every boot — Dashboard never opens to a hostile zero state.
- Recipe meta line and Featured cover badge no longer render `★ 0.0 (0)` / `★ 0` for unrated recipes; show `Not rated yet` (italicised) in the meta line and hide the badge entirely.
- Empty-day Dashboard collapses four `Nothing here yet.` rows into one mint-tinted CTA card pointing at Diary.

---

## [v0.6.8] - 2026-05-02 — Six more recipes seeded into the catalogue (closes #58)

### Added
- Six new recipes covering breakfast / lunch / dinner: Avocado Toast, Greek Yogurt Parfait, Tofu & Brown Rice Bowl, Avocado Egg Wrap, Tofu Broccoli Stir-Fry, Lentil & Sweet Potato Curry. Each ships with a real Unsplash cover, an ingredient list linked to the existing `food_items` library (so per-serving nutrition computes automatically), and step-by-step instructions.
- New `SeedData.backfillExtraRecipes()` — idempotent, runs on every boot, inserts only when the recipe title is missing — so the live Render PostgreSQL picks up the six new recipes on the next deploy without a fresh seed.

---

## [v0.6.7] - 2026-05-02 — UI tier-up: unified radius, larger card padding, mint accent (closes #56)

### Fixed
- Corner radii collapsed onto a single 14px family — `--radius-md` 16→14, `--radius-sm` 8→10, `--radius-lg` 20→18 — so cards / buttons / modals all live in the same geometric tier.
- `.card` padding bumped from 24px to 28px, `.recipe-card__body` from 18px to 20px so cards breathe like the dashboard summary already does.
- New `--color-mint` (#b1dbb8) and `--color-mint-bg` (#e1f4df) palette tokens with dark-mode counterparts; the `Featured this week` heading underline now uses mint as a distinct accent from the rest of the sage-on-cream layout.

---

## [v0.6.6] - 2026-05-02 — Recipes: real cover photos, per-serving nutrition, Featured strip (closes #54)

### Fixed
- Seed recipes (`Grilled Chicken Salad`, `Overnight Oats Bowl`, `Grilled Salmon with Veggies`) now ship with real Unsplash cover photos via an idempotent `SeedData.backfillImageUrls()` that runs on every boot. User-added recipes still fall back to the emoji cover.
- Recipe cards now show per-serving calories and protein under the description (`320 kcal · 28g protein per serving`) — same nutrition logic that already powered the detail page, lifted into `searchRecipes` via a shared `summariseRow` helper.
- Added a `Featured this week` strip above the search filter, showing the top 3 recipes by average rating (ties broken by review count). Hidden when the user has applied a search or difficulty filter so it doesn't fight the results.

---

## [v0.6.5] - 2026-05-02 — Final polish: macro typography, date prominence, filter row, sidebar tone (closes #52)

### Fixed
- Macro rows now split current value (body-size, strong) from goal (`/ 80 g` muted) instead of one grey lump.
- Page-header date bumped from small / soft to base size with a sage-soft left accent bar so the date reads as page context rather than fine print.
- Recipes filter card collapsed from a 3-row stacked form into a single inline strip (search input + difficulty select + Apply); roughly 60px less vertical air on `/recipes`.
- Sidebar bg softened from `#1f2a23` to `#2d3d34` (warmer, less editorial); active-link tint lifted to `rgba(184,209,168,0.26)` so the highlight reads on the new bg.

---

## [v0.6.4] - 2026-05-02 — Dashboard polish: ring, thicker bars, warmer empty states (closes #50)

### Fixed
- Calories progress was a thin 6px line buried among three other macros — promoted to a conic-gradient ring centrepiece with the absolute number and percent inside.
- Macro progress bars bumped from 6px to 10px with rounded ends and per-macro gradients (deep-sage → sage on calories, sage → sage-soft on protein, clay → clay-soft on fat).
- Empty-state copy (`No foods logged for this meal.`, `Nothing logged yet.`, `No data for this week yet.`) replaced with friendlier nudges; diary empty meals point at the **Add food** button.

---

## [v0.6.3] - 2026-05-01 — UI detail bug fixes (closes #46)

### Fixed
- Trailing `.00` on numeric values across Dashboard / Diary / Goals / professional client-detail.
- Raw ISO timestamps on Messages chat bubbles.
- Bare text-only Recipe cards.

---

## [v0.6.2] - 2026-05-01 — Persist accounts on Render via PostgreSQL (closes #44)

### Why
The H2 database file (`./data/goodfood.mv.db`) lives inside each container's local filesystem and is `.gitignore`d, so it never travels with the code. On Render's free tier the container is rebuilt on every deploy and on idle spin-up, which wipes `data/` and erases every account a user registered through the web UI. The seed accounts only appear permanent because `SeedData.insertIfEmpty()` re-runs against the fresh schema and re-creates them. The same dynamic explains why a Codespace registered account disappears the next time the codespace is recycled. For the assessment demo the marker needs to register an account and have it survive a redeploy, so production needs a database that lives outside the container.

### Changed
- **`Database.kt`** — new `resolveDbSettings()` helper. When the `DATABASE_URL` environment variable is present (Render injects this when a PostgreSQL service is linked to the web service) the libpq form `postgres://user:pass@host:port/db[?…]` is parsed into a JDBC URL plus separate credentials, and the connection goes through `org.postgresql.Driver`. When the variable is unset — Codespaces, local dev, CI — the existing H2 file path from `application.conf` is used unchanged. Any query string on the original URL (e.g. `sslmode=require` for an external Render endpoint) is preserved verbatim.
- **`build.gradle.kts`** — added `org.postgresql:postgresql:42.7.4` alongside the existing H2 driver. Both ship in the fat jar so the same artifact runs locally on H2 and on Render against PostgreSQL.

### Notes
- Existing Exposed queries are dialect-portable (`Recipes.title.lowerCase() like ?`, `FoodItems.name.lowerCase() like ?`); `lowerCase()` compiles to `LOWER(col)` which works on H2, MySQL, and PostgreSQL alike. Schema generation is delegated to `SchemaUtils.create(...)` and Exposed picks the right DDL per dialect.
- Render setup after merge: provision a PostgreSQL Free instance, attach it to the web service so `DATABASE_URL` is auto-injected, then redeploy. No code switch needed — the env var alone flips the runtime to PostgreSQL.

---

## [v0.6.1] - 2026-05-01 — Fix Render build (Gradle 8.5)

### Fixed
- **Render deploy** — bumped the Dockerfile build-stage image from `gradle:7.6-jdk17` to `gradle:8.5-jdk17`. The Shadow plugin in `build.gradle.kts` is at `com.gradleup.shadow:8.3.0`, which requires Gradle 8.3+; the old base image shipped Gradle 7.6, so `gradle shadowJar` failed with *"This version of Shadow supports Gradle 8.3+ only"* and Render builds exited with status 1. The new image matches the project's wrapper version (`gradle-8.5-bin.zip`). Local dev and GitHub Actions were unaffected because they use `./gradlew`, not the Docker base image's `gradle` binary.

---

## [v0.6.0] - 2026-05-01 — Warm wellness redesign + animated login background (closes #40)

The team sat with v0.5.0 for a couple of days and decided it was the wrong fit. The disciplined research-journal aesthetic looked sharp in isolation but read as clinical and intimidating for a consumer app whose entire purpose is to *encourage* people to log meals and try home cooking. The brief is about warmth and habit-building, not editorial gravity. v0.6.0 swaps the system out for one that fits the product.

### Changed
- **Palette** — cream / oat / sage / terracotta replaces ivory / slate / clay. Deep-forest text on warm cream surfaces. Sage as the calm primary; terracotta reserved as the single warm accent. Berry tone added for genuine alerts only.
- **Type** — consolidated to **Plus Jakarta Sans** at every level. The v0.5.0 sans + serif + mono trio was elegant but added formality the product doesn't need; one friendly geometric family across the system reads as "consumer wellness" rather than "magazine".
- **Geometry** — soft rounded corners throughout (8 / 14 / 16 / 20 px and pill). Buttons are now pills; modal corners 20 px; cards 16 px. Subtle layered shadows return as elevation cues — barely there but present, replacing the v0.5.0 zero-shadow rule.
- **Sidebar** — deep-forest band with cream text; active nav entry uses a soft sage-tinted background instead of the previous accent border.
- **Recipe cards** — inviting elevation on hover (translate-up + slightly heavier shadow). The home-cooking pillar of the brief is where the new system most obviously pays off.
- **Macro progress bars** — gradient fill from deep sage to sage on the calorie bar, solid sage on protein, muted sage on carbs, terracotta on fat.
- **Tabs in the auth card** — pill-tabs inside a warm tray with a soft shadow, replacing the underline-tab pattern.

### Added
- **Animated login background** — two large soft-edged colour fields (sage and terracotta tints) drift slowly behind the auth card. Pure CSS via `::before` and `::after` on `.auth-body`, `transform` and `opacity` only, GPU-composited, blurred for an organic feel. `prefers-reduced-motion` cuts the animation entirely. The auth card sits crisp on top — motion is wallpaper, not theatre.

### Notes
- Same constraint as v0.5.0: zero HTML class-name renames, JavaScript untouched, all 21 tests still green, dark mode keeps working (inverts into a deep-forest base with cream text).
- `DESIGN_DECISIONS.md` gains entry **D-12** explaining why v0.5.0 was retired.

---

## [v0.5.1] - 2026-05-01 — Food-search hint & threshold (closes #38)

### Changed
- The diary "Add food" search input now shows an inline hint underneath: *"Type at least 3 letters to search. Try ban, chi, or oat."* The placeholder also says *"Type at least 3 letters…"* instead of the vague *"Type to search…"*.
- The JS minimum-query-length constant is bumped from 2 → 3 so the actual behaviour matches the hint exactly (extracted as `MIN_QUERY_LEN` so the next change is a one-liner).
- `aria-describedby` wires the hint into the input's accessibility tree so screen-reader users hear the threshold too.

### Why
Testers (and one of our own teammates) kept typing 1–2 letters into the search and giving up when nothing appeared — the field looked broken on first interaction. The hint is the smallest possible fix.

---

## [v0.5.0] - 2026-05-01 — Visual identity refresh inspired by Anthropic

We've spent the last few weeks looking at how research-led companies communicate visually, and Anthropic's site (anthropic.com) kept coming up as a reference the team admired — the warm parchment ivory background instead of clinical white, the strict alternation of light and slate-dark surfaces, the serif-plus-grotesque type pairing that reads more "research journal" than "startup", and the discipline of holding the entire chromatic budget for a single terracotta accent. As a learning exercise we studied their public design tokens and component vocabulary, then translated the system onto the existing Good Food class names so we could see what our app looked like in that visual idiom without rewriting any templates.

The result is v0.5.0 — same product, redesigned skin.

### Changed
- **Palette** — replaced the v0.4.0 emerald token system with the Anthropic-inspired palette: ivory (`#faf9f5`) page base, near-black slate (`#141413`) primary text, ivory-medium / oat for surfaces, clay (`#d97757`) reserved as the one accent.
- **Surface alternation** — page base ivory; cards 8 px radius on ivory-medium / oat; sidebar a slate-dark band; "feature" cards 24 px radius on slate-dark with ivory text. Zero gradients, zero blur transitions.
- **Typography** — switched to `Inter` (sans), `Playfair Display` (serif), and `JetBrains Mono` (mono) via Google Fonts. Type scale aligned to the Anthropic spec: 12 → 15 → 18 → 20 → 24 → 61 → 91 px. Body uses `-0.002em` tracking; display sizes use `-0.02em`.
- **Geometry** — buttons radius `0`; the primary "Sign in" / "Add food" CTA gets the asymmetric `0 0 8px 8px` Anthropic signature; cards radius `8`; dark feature cards radius `24`. **All `box-shadow` rules deleted.** Depth is now conveyed by surface contrast and 1 px hairlines only.
- **Emphasis** — headline keywords use a thick `text-decoration: underline` (`.text-emphasis` utility) rather than colour, matching the Anthropic underline-as-accent convention.
- **Metadata labels** — `DATE`, `CATEGORY`, time-on-task and stat captions now use `JetBrains Mono` 12 px in uppercase with `0.06em` tracking, matching the Anthropic editorial/data-label pattern.
- **Forms** — inputs are flat 1 px hairlines, focus ring is `clay`-coloured.
- **Dark mode** — kept the toggle from v0.4.0 but inverted to a slate-dark base with ivory text (Anthropic's reference is light-only; we didn't want to drop a feature we'd already shipped).

### Notes
- **Zero template / class-name changes.** Every existing class (`.btn`, `.card`, `.sidebar`, `.macro-card`, `.recipe-card`, `.conv-list`, `.bubble`, `.modal`, `.toast`, `.skeleton`) is preserved with the same selector — only its styling has been redrawn. HTML templates were not touched.
- All 21 tests still pass. Mobile drawer + theme toggle JS still work.

### Inspiration
- anthropic.com — the design tokens and component vocabulary we studied for this refresh. Substitute fonts (Inter, Playfair Display, JetBrains Mono) are the ones the Anthropic style reference recommends for non-licensed deployments.

---

## [v0.4.7] - 2026-05-01 — UX-test report, integration tests, security tests, design-decisions doc

### Added
- **`UX_TESTING.md`** at the repo root (closes #31) — Round-1 moderated walkthrough: scenarios, time-on-task, 8 findings with severity, decisions on which to fix before the demo. Closes the rubric Pass-tier requirement *"At least one UX test performed with feedback used to improve"*.
- **`src/test/kotlin/com/goodfood/IntegrationTest.kt`** (closes #32) — first HTTP-level tests via Ktor `testApplication`. Each test boots the full `Application.module` against a fresh in-memory H2. Three scenarios covered: unauthenticated dashboard redirects to login, login page renders, food-search API rejects unauthenticated requests.
- **`src/test/kotlin/com/goodfood/SecurityTest.kt`** (closes #33) — five regression tests guarding the v0.4.4 security fixes: literal `%` and `_` in food/recipe search no longer dump the table, normal queries still work, cross-user diary delete is a no-op.
- **`DESIGN_DECISIONS.md`** at the repo root (closes #34) — chronological "what / why / alternatives considered" entries for ten significant decisions: feature-module layout, H2 + seed-on-empty, server-side rendering, dark-mode tokens, mobile drawer, IDOR helper, cookie hardening, LIKE escaping, AI transparency, non-blocking Detekt CI.
- **README "Beyond the basic spec" section** (closes #34) — surfaces the extras (dark mode, mobile drawer, IDOR pattern, cookie hardening, CI, devcontainer, AI transparency, evolved documentation).
- README links to `UX_TESTING.md` and `DESIGN_DECISIONS.md`.

### Notes
- 8 new tests bring total to **21**: 13 service-level units + 5 security-regression + 3 HTTP-integration.
- No production code behaviour changes — this release adds documentation and tests only.

---

## [v0.4.6] - 2026-05-01 — Detekt, class diagram, KDoc, user stories, accessibility audit

### Added
- **Detekt static analysis** (closes #25) — `io.gitlab.arturbosch.detekt` plugin in `build.gradle.kts`, repo-level `detekt.yml` tuned for the project (relaxed style rules, strict correctness rules), CI workflow runs `./gradlew detekt` after the unit tests and uploads the report as an artifact. Initial run is non-blocking (`continue-on-error: true`) so the team can tighten rules incrementally.
- **Class diagram** (closes #26) — `CLASS_diagram.md` at the repo root: Mermaid `classDiagram` covering routes, services, table objects, and their dependencies, grouped by feature module. README now points to it alongside the ER diagram.
- **KDoc on the five core service classes** (closes #27) — `UserService`, `DiaryService`, `GoalService`, `RecipeService`, `MessageService` now have class-level KDoc and method-level `@param` / `@return` notes on every public function. No behaviour changes.
- **User stories** (closes #28) — `USER_STORIES.md` at the repo root: 15 stories in `As a [role], I want [...]` form, MoSCoW priority + XS/S/M/L estimate per story, mapped back to wiki Job Stories and forward to AC IDs that the test suite already covers.
- **Accessibility self-audit** (closes #29) — `ACCESSIBILITY.md` at the repo root: per-page WCAG 2.1 AA checklist, list of structural patterns (`:focus-visible`, ARIA, prefers-reduced-motion, dark mode), honest list of known gaps. README now points to it.

### Changed
- README gains "Accessibility" and "Requirements" sections linking the new docs.
- README "Database & class design" section consolidates ER + class diagram links.

### Notes
- Documentation and tooling only. No runtime behaviour changes; the existing 13 unit tests still pass.

---

## [v0.4.5] - 2026-05-01 — CI workflow, test ↔ acceptance-criteria mapping, README fix

### Added
- **GitHub Actions** workflow at `.github/workflows/build.yml` (closes #21) — `build-and-test` job runs `./gradlew build test` on push to `main` and on every PR targeting `main`. JDK 17 (Temurin), Gradle cache, test report uploaded as an artifact on every run. Closes the rubric gap on *"Excellent use of Git with a clear strategy including Actions or other CICD tools"*.
- **Acceptance-criteria headers** on every `*ServiceTest.kt` (closes #22) — each file now opens with a KDoc block naming the wiki Job Story it covers and the explicit ACs (`AC-AUTH-1`, `AC-DIARY-1`, `AC-GOAL-1`, `AC-RECIPE-1`, `AC-MSG-1`, `AC-DB-1` …) that each test method exercises. No test logic changed.

### Changed
- **README** (closes #23) — the `## UI Prototype` section pointed to a `ui-prototype/` folder that was removed long ago; replaced with a `## UI design` section pointing to `UI_wireframes.md`.

### Notes
- Documentation / process only. No runtime behaviour change.
- The first run of the new workflow will be the merge commit of this PR.

---

## [v0.4.4] - 2026-04-30 — Critical security fixes

### Fixed
- **IDOR on professional client view** (#16) — `GET /pro/client/{id}` previously returned any subscriber's diary, goals, and nutrition summary as long as the requester's session role was `professional`. Now also requires an active row in `client_relationships` between the professional and the requested subscriber; otherwise the request is redirected to `/pro/dashboard`.
- **IDOR on professional advice endpoint** (#17) — `POST /pro/client/{id}/advice` previously created a message addressed to any user id. Same authorisation gate now applies; an extra role check was added on this route for parity with the GET. Submissions to non-client ids now redirect and create no message.
- **Session cookie missing `HttpOnly` / `SameSite`** (#18) — `config/Security.kt` now sets `cookie.httpOnly = true` and `cookie.extensions["SameSite"] = "Lax"`. JavaScript can no longer read the session cookie, and browsers will not send it on cross-site POST submissions, blocking the standard form-based CSRF path. `Secure` is intentionally left off so `./gradlew run` over plain HTTP in dev / Codespaces keeps working; flip it on for production HTTPS.
- **`LIKE` wildcard injection in food / recipe search** (#19) — `searchFood()` (DiaryService) and `searchRecipes()` (RecipeService) now escape the SQL wildcards `%` and `_` and the escape character `\` before substituting user input into the `LIKE` pattern. Submitting `%` no longer matches the entire table.

### Notes
- All four fixes are scoped to authorisation / cookie / search-input handling; no schema changes, no UI changes, no new dependencies.
- Existing unit tests still pass; manual regression covered the happy paths (Sarah → Alice diary visible, food search for `apple`, recipe search for `salmon`).

---

## [v0.4.3] - 2026-04-29 — Backfill Cursor acknowledgment in AI_USAGE.md

### Added
- `AI_USAGE.md` now lists the 5 pre-v0.4.0 commits assisted by Cursor (build-config / Dockerfile fixes by `chlwu0777`). The `Made-with: Cursor` git trailers on those commits remain as the contemporaneous acknowledgment; this update brings the project-level log to full coverage.

### Notes
- Documentation only. No code changes. The Cursor trailers were already valid amber-rating acknowledgments — this commit just consolidates them into the central log.

---

## [v0.4.2] - 2026-04-29 — AI acknowledgment & code comments

### Added
- `AI_USAGE.md` at the repo root — canonical log of every AI-assisted contribution (model, scope, what the human verified) for COMP2850 amber-rated AI use compliance.
- Header comments in `static/css/styles.css` and `static/js/app.js` naming the AI model (Claude Opus 4.6) and the lines/sections it drafted, per the assessment brief example format.
- README "Generative AI usage" section pointing to the log.

### Changed
- Going forward, no `Co-Authored-By: <AI>` git trailers and no `🤖 Generated with Claude Code` bot footers in PR descriptions — AI acknowledgment lives in code comments and `AI_USAGE.md` instead, treating AI as a supportive tool (not a contributor) in line with the amber rating.

### Notes
- No code behavior changes — comments and documentation only.
- Retroactively documents AI assistance for v0.4.0 (UI refresh) and v0.4.1 (devcontainer config).

---

## [v0.4.1] - 2026-04-29 — Codespace / dev container config

### Added
- `.devcontainer/devcontainer.json` — pins JDK 17 (matches `build.gradle.kts` `jvmToolchain(17)`) so GitHub Codespaces and VS Code Dev Containers boot ready to run `./gradlew run` with no manual `apt-get install` or JDK switching.
  - Base image: `mcr.microsoft.com/devcontainers/java:1-17-bookworm`
  - Port `8080` auto-forwarded with `openBrowser` on first start
  - `postCreateCommand` warms the Gradle daemon so the first build is faster
  - Pre-installs Kotlin, Gradle, and Thymeleaf VS Code extensions

### Notes
- No code changes — config-only.
- Existing local dev workflow (`./gradlew run` with a system JDK 17) is unaffected.

---

## [v0.4.0] - 2026-04-29 — UI Upgrade: Visual refresh, dark mode, component polish

### Added
- **Dark mode** with three modes — auto (follows `prefers-color-scheme`), manual light, manual dark
  - Theme toggle button in every sidebar (and floating button on the login page)
  - User selection persisted via `localStorage` (`gf-theme` key)
  - Inline pre-`DOMContentLoaded` script applies saved theme synchronously to prevent flash-of-wrong-theme
  - Full color-token reorganization (~30 semantic tokens: surface, surface-alt, text, text-strong, muted, border, primary, accent, danger, sidebar layers, chat layers, table layers, etc.)
- **Mobile drawer sidebar** below 840 px — fixed-position slide-in drawer with backdrop, replacing the previous squashed top bar
  - Hamburger toggle button (top-left), backdrop click, Escape key, and nav-link click all close the drawer
- **Component polish**
  - Card hover lift (recipe cards, conversations, table rows)
  - Animated progress bars (smooth width transition + subtle shimmer)
  - Modal scale-in + backdrop blur
  - Toast region (`.toast-region` / `.toast` / `.toast--warn` / `.toast--danger`)
  - Skeleton loader (`.skeleton`)
  - Chat bubble entrance animation
  - Star rating hover scale
- **Accessibility**
  - `:focus-visible` ring on all interactive elements (replaces the previous 2px outline)
  - `prefers-reduced-motion` reduces all animation durations to ~0
  - Print stylesheet hides chrome (sidebar, toggles, toasts) and flattens shadows

### Changed
- Color contrast bumped — `--color-text-strong` introduced for headings, primary button gains gradient + soft shadow, focus ring uses accent color
- Typography polish — added `letter-spacing` micro-adjustments to titles, font-smoothing on body, slightly larger page-title (1.65 → 1.75 rem)
- Auth background switched to dual radial-gradient
- Modal panel scales in instead of fading; backdrop has 4 px backdrop blur
- Sidebar active state uses inset accent border (3 px) in addition to bg color
- Tablet breakpoint added (1024 px) — recipe grid retightens, main padding compacts
- All transitions use a shared `cubic-bezier(0.4, 0, 0.2, 1)` ease and three duration tokens (`--dur-fast` / `--dur` / `--dur-slow`)

### Notes
- Backend, routes, services, models, and seed data are **untouched** — this release is CSS + JS + template chrome only.
- No new dependencies; uses native CSS features (`color-mix`, custom properties, `prefers-color-scheme`).

---

## [v0.3.0] - 2026-03-24 — Refactor: Restructure Kotlin by Feature Module

### Changed
- Project structure refactored from layer-based (models/, plugins/, routes/, services/) to feature-based modules
- Each module folder now contains its own table definitions, routes, and service logic together
- Updated all package declarations and import paths

### New Directory Layout
```
src/main/kotlin/com/goodfood/
├── Application.kt           # Entry point
├── config/                  # Database, Routing, Security, Templating
├── auth/                    # Users, AuthRoutes, UserService
├── diary/                   # FoodItems, DiaryEntries, NutritionalGoals, DiaryService, DashboardRoutes
├── goals/                   # GoalRoutes, GoalService
├── recipes/                 # Recipes, Ingredients, Steps, Ratings, Favourites, RecipeService
├── messages/                # AdviceMessages, MessageRoutes, MessageService
├── professional/            # Profiles, ClientRelationships, ProfessionalRoutes
├── profile/                 # ProfileRoutes
└── seed/                    # SeedData
```

---

## [v0.2.0] - 2026-03-24 — Add Ktor Full-Stack Web Application

### Added
- Complete Kotlin/Ktor backend with all web functionalities
- H2 embedded database with Exposed ORM (12 tables)
- Thymeleaf server-side rendering with 11 HTML templates
- User authentication with BCrypt password hashing
- Subscriber features: Dashboard, Food Diary, Recipes, Goals, Messages, Profile
- Professional features: Client management, messaging
- Seed data with test accounts (alice@email.com, bob@email.com, sarah@clinic.com / password: password)

---

## [v0.1.0] - 2026-03-24 — Initial Commit: Design Phase

### Added
- ER diagram design
- UI wireframes
- Interactive HTML/CSS prototype
