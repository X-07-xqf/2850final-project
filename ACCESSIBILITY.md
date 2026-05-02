# Accessibility — WCAG 2.1 AA self-audit

This is the team's self-audit of accessibility against WCAG 2.1 Level AA. Where a criterion is met, the cell shows ✅ and the file/pattern that delivers it. Where a criterion is partial or missing, the cell shows ⚠️ / ❌ with what would be needed to fix.

## Structural patterns in place

| Pattern | Where | Notes |
|---|---|---|
| Semantic HTML (`<nav>`, `<main>`, `<header>`, `<aside>`, `<h1>`–`<h2>`) | All Thymeleaf templates under `src/main/resources/templates/` | One `<h1>` per page, descending heading order. |
| `:focus-visible` ring on all interactive elements | `static/css/styles.css` `--ring` token | Replaces the prior 2 px outline; visible on Tab navigation. |
| `aria-label` on icon-only buttons | Theme toggle, mobile menu toggle, modal close, diary "Remove" button | |
| `role="progressbar"` + `aria-valuenow` / `aria-valuemin` / `aria-valuemax` | Macro progress bars on dashboard | |
| `role="tablist"` / `role="tab"` / `role="tabpanel"` + `aria-selected` | Login page Login / Register tabs | |
| `aria-hidden` on the modal when closed; `aria-labelledby` on the dialog | "Add food" modal | |
| `prefers-reduced-motion: reduce` cuts every animation duration to ~0 ms | Top of `styles.css` | |
| `prefers-color-scheme: dark` honoured plus a manual override | Dark-mode tokens (v0.4.0) | |
| `<label>` properly associated with every form `<input>` (wrapping label form) | All forms | No floating labels; labels are read by every screen reader. |
| `autocomplete` attributes on email, current-password, new-password fields | Login & register forms | |
| `.sr-only` utility class for visually-hidden helper text | Several templates | |
| Skip-to-main-content pattern | Each page has a single `<main>` with focusable content; Tab from the top reaches the first nav link in 1 hop. | (No explicit "Skip to main" link — see ⚠️ below.) |
| Keyboard support — Escape closes the modal and the mobile drawer | `static/js/app.js` | |

## Per-page WCAG 2.1 AA self-audit

Legend: ✅ pass · ⚠️ partial · ❌ fail · n/a not applicable to this page.

### `/login` (auth/login.html)

| Criterion | Status | Note |
|---|---|---|
| 1.1.1 Non-text content | ✅ | Logo is decorative emoji; nothing else relies on imagery. |
| 1.3.1 Info and relationships | ✅ | Tabs use `role="tablist"`; form fields use `<label>`. |
| 1.4.3 Contrast (minimum) | ⚠️ | `--color-primary #2d6a4f` on `--color-bg #f4f8f5` measures ~4.8:1 (passes AA for normal text by a small margin). Not formally measured across every state. |
| 1.4.10 Reflow | ✅ | Layout reflows down to 320 px. |
| 2.1.1 Keyboard | ✅ | All controls reachable by Tab. |
| 2.4.7 Focus visible | ✅ | `:focus-visible` ring. |
| 3.2.2 On input | ✅ | Submitting the form is the only state-changing action. |
| 3.3.1 Error identification | ⚠️ | Error message rendered when login fails, but is not associated with the inputs via `aria-describedby`. |
| 3.3.2 Labels or instructions | ✅ | Every field labelled. |

### `/dashboard` (subscriber/dashboard.html)

| Criterion | Status | Note |
|---|---|---|
| 1.3.1 Info and relationships | ✅ | Headings, regions, progress bars all use semantic markup. |
| 1.4.1 Use of colour | ✅ | Status (on-track / over) is reinforced by the progress-bar fill width and the numeric label, not colour alone. |
| 1.4.3 Contrast | ✅ | Body text on surface measures 12.6:1. Badge contrast verified for warn states. |
| 2.4.4 Link purpose | ✅ | Sidebar links are descriptive ("Diary", not "click here"). |
| 4.1.2 Name, role, value | ✅ | Progress bars expose `aria-valuenow`. |
| 1.4.11 Non-text contrast | ⚠️ | Border colour `#d4e5db` on white is below 3:1; OK because borders are decorative, but worth flagging. |

### `/diary` (subscriber/diary.html)

| Criterion | Status | Note |
|---|---|---|
| 1.3.1 Info and relationships | ✅ | Each meal section is its own `<section>` with an `<h2>`. |
| 2.4.3 Focus order | ✅ | Date nav → meals → "Add food" modal trigger; logical. |
| 4.1.2 Name, role, value (modal) | ✅ | `role="dialog"`, `aria-labelledby`, `aria-hidden` toggles. |
| 2.1.2 No keyboard trap | ✅ | Escape closes the modal. |
| 3.3.1 Error identification | ⚠️ | The "no food selected" state disables the submit button but doesn't announce why to a screen reader. Could add `aria-describedby` pointing at a status node. |

### `/recipes` and `/recipes/{id}` (subscriber/recipes.html, recipe-detail.html)

| Criterion | Status | Note |
|---|---|---|
| 1.4.3 Contrast | ✅ | Recipe card text and meta on surface tested. |
| 2.4.4 Link purpose | ✅ | Cards link to recipe titles, not "View". |
| 1.3.1 Info and relationships | ✅ | Steps use `<ol>`, ingredients use `<ul>`. |
| 4.1.2 Star rating | ⚠️ | Star buttons are real `<button>`s but their pressed state is conveyed only by colour change. Should also flip `aria-pressed`. |

### `/goals` (subscriber/goals.html)

| Criterion | Status | Note |
|---|---|---|
| 1.3.1 / 4.1.2 | ✅ | Form labelled, submit clearly named. |
| 1.4.3 Contrast | ✅ | |

### `/messages` and `/pro/messages` (subscriber/messages.html, professional/messages.html)

| Criterion | Status | Note |
|---|---|---|
| 1.3.1 / 4.1.2 | ✅ | Conversation list uses `<a>` links; bubble layout uses `<article>`-equivalent semantics. |
| 1.4.3 Contrast | ✅ | "Theirs" bubble text 6.7:1; "Mine" bubble white-on-primary 5.2:1. |
| 2.4.7 Focus visible | ✅ | |
| 3.3.2 Labels | ✅ | Compose textarea labelled. |

### `/profile` (subscriber/profile.html)

| Criterion | Status | Note |
|---|---|---|
| All applicable criteria | ✅ | Read-only profile data + simple form. No identified issues. |

### `/pro/dashboard` and `/pro/client/{id}` (professional/...)

| Criterion | Status | Note |
|---|---|---|
| 1.3.1 Info and relationships (table) | ✅ | `<th>` headers in the client list. |
| 4.1.2 Status pills | ✅ | "On Track" / "Needs Attention" pill carries the text, not colour alone. |
| 1.4.3 Contrast | ✅ | Status-pill warn variant 4.6:1. |

## Possible gaps

1. No automated axe-core or Lighthouse run yet. We plan to include axe-core in the GitHub Actions workflow as a follow-up.
2. Colour contrast not measured at every interactive state, such as hover/active/disabled.
3. No "Skip to main content" link, as tab order is currently short enough to not necessarily require this.
4. Star-rating buttons do not flip `aria-pressed`. Only colour communicates the rating value to assistive tech.
5. Form errors are not associated with their inputs via `aria-describedby`. Login error, modal "select a food first", and goals validation messages all render correctly visually but are not linked into the form-control accessibility tree.

## How to re-run this audit

When the app is running locally (`./gradlew run`):

1. Open the page in Chrome.
2. DevTools → Lighthouse → check Accessibility → run.
3. DevTools → Issues tab → look for any Accessibility category items.
4. Re-tab through the page from the URL bar to confirm the focus order matches the table above.

For automated coverage, install [`axe-core` CLI](https://github.com/dequelabs/axe-core-npm) and run:

```bash
npx @axe-core/cli http://localhost:8080/dashboard --tags wcag2a,wcag2aa --exit
```

Findings should land in this file (or, ideally, in a CI step that fails on new violations).
