/*
 * Sage — front-end glue (auth tabs, food modal, food search, star rating,
 * theme toggle, mobile sidebar drawer).
 *
 * AI acknowledgment (COMP2850 amber-rated AI use):
 * The v0.4.0 additions — initTheme() and initSidebarDrawer(), plus the early
 * synchronous theme application near the bottom of this file (~lines 162–220) —
 * were drafted with Claude Opus 4.6 (Anthropic) acting as a front-end
 * pair-programmer. Charlie Wu reviewed every line, tested the toggle and
 * drawer end-to-end in Chrome DevTools, and approved before merge.
 * initAuthTabs(), initFoodModal(), initFoodSearch(), and initStarRating()
 * pre-date v0.4.0 and were authored by the team.
 * See AI_USAGE.md in the repo root for the full log.
 */
(function () {
    "use strict";

    /* ---- Auth tabs (login / register) ---- */
    function initAuthTabs() {
        var tabLogin = document.getElementById("tab-login");
        var tabRegister = document.getElementById("tab-register");
        var panelLogin = document.getElementById("panel-login");
        var panelRegister = document.getElementById("panel-register");
        if (!tabLogin || !tabRegister || !panelLogin || !panelRegister) return;

        function showTab(name) {
            var isReg = name === "register";
            panelLogin.classList.toggle("is-hidden", isReg);
            panelLogin.classList.toggle("tab-panel--active", !isReg);
            panelRegister.classList.toggle("is-hidden", !isReg);
            panelRegister.classList.toggle("tab-panel--active", isReg);
            tabLogin.setAttribute("aria-selected", (!isReg).toString());
            tabRegister.setAttribute("aria-selected", isReg.toString());
        }

        if (typeof window.__AUTH_TAB__ !== "undefined" && window.__AUTH_TAB__ === "register") {
            showTab("register");
        }

        tabLogin.addEventListener("click", function () {
            showTab("login");
        });
        tabRegister.addEventListener("click", function () {
            showTab("register");
        });
    }

    /* ---- Modal (food diary) ---- */
    function initFoodModal() {
        var modal = document.getElementById("add-food-modal");
        if (!modal) return;

        var mealInput = document.getElementById("modal-meal-type");
        var foodIdInput = document.getElementById("modal-food-item-id");
        var submitBtn = document.getElementById("modal-submit");

        function openModal(mealType) {
            if (mealInput && mealType) mealInput.value = mealType;
            if (foodIdInput) foodIdInput.value = "";
            if (submitBtn) submitBtn.disabled = true;
            modal.classList.add("is-open");
            modal.setAttribute("aria-hidden", "false");
        }

        function closeModal() {
            modal.classList.remove("is-open");
            modal.setAttribute("aria-hidden", "true");
        }

        document.querySelectorAll(".js-open-food-modal").forEach(function (btn) {
            btn.addEventListener("click", function () {
                openModal(btn.getAttribute("data-meal-type") || "breakfast");
            });
        });

        modal.querySelectorAll(".js-modal-close").forEach(function (el) {
            el.addEventListener("click", closeModal);
        });

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && modal.classList.contains("is-open")) closeModal();
        });
    }

    /* ---- Food picker (visual grid) ----
       Replaces the old type-3-letters search. Modal opens → grid loads with
       every food (server caps at 60). Search input filters live. Click a
       card → marks it selected, sets the hidden foodItemId, enables submit. */
    function initFoodPicker() {
        var input = document.getElementById("food-search-input");
        var grid = document.getElementById("food-search-results");
        var emptyEl = document.querySelector(".food-picker__empty");
        var foodIdInput = document.getElementById("modal-food-item-id");
        var submitBtn = document.getElementById("modal-submit");
        var modal = document.getElementById("add-food-modal");
        if (!input || !grid) return;

        var allFoods = [];
        var loaded = false;

        function renderGrid(items) {
            grid.innerHTML = "";
            if (!items || !items.length) {
                if (emptyEl) emptyEl.hidden = false;
                return;
            }
            if (emptyEl) emptyEl.hidden = true;
            items.forEach(function (item) {
                var card = document.createElement("button");
                card.type = "button";
                card.className = "food-card";
                card.setAttribute("role", "option");
                card.setAttribute("data-food-id", String(item.id));
                card.setAttribute("data-food-name", item.name.toLowerCase());
                card.innerHTML =
                    '<span class="food-card__cover food-card__cover--' + item.tone + '">' +
                        '<span class="food-card__emoji" aria-hidden="true">' + item.emoji + '</span>' +
                    '</span>' +
                    '<span class="food-card__name"></span>' +
                    '<span class="food-card__cal"></span>';
                card.querySelector(".food-card__name").textContent = item.name;
                card.querySelector(".food-card__cal").textContent =
                    Math.round(parseFloat(item.calories)) + " kcal/100g";
                card.addEventListener("click", function () {
                    grid.querySelectorAll(".food-card.is-selected")
                        .forEach(function (c) { c.classList.remove("is-selected"); });
                    card.classList.add("is-selected");
                    if (foodIdInput) foodIdInput.value = String(item.id);
                    if (submitBtn) submitBtn.disabled = false;
                });
                grid.appendChild(card);
            });
        }

        function loadAll() {
            if (loaded) return Promise.resolve();
            return fetch("/api/food-search?q=")
                .then(function (r) { return r.ok ? r.json() : []; })
                .then(function (items) {
                    allFoods = items || [];
                    loaded = true;
                    renderGrid(allFoods);
                })
                .catch(function () { allFoods = []; loaded = true; renderGrid([]); });
        }

        // Live filter — substring match on food name. No min query length;
        // empty input shows everything. Filtering is client-side once the
        // initial fetch lands.
        input.addEventListener("input", function () {
            var q = input.value.trim().toLowerCase();
            if (!q) { renderGrid(allFoods); return; }
            renderGrid(allFoods.filter(function (item) {
                return item.name.toLowerCase().indexOf(q) !== -1;
            }));
        });

        // Lazy-load on first modal open (so /diary doesn't pay for the food
        // list on every page render). Watch for the .is-open class flip.
        if (modal) {
            var observer = new MutationObserver(function () {
                if (modal.classList.contains("is-open") && !loaded) loadAll();
            });
            observer.observe(modal, { attributes: true, attributeFilter: ["class"] });
        }
    }

    /* ---- Star rating ---- */
    function initStarRating() {
        document.querySelectorAll(".js-star-rating").forEach(function (wrap) {
            var hidden = wrap.closest("form") && wrap.closest("form").querySelector("#rate-input");
            if (!hidden) hidden = document.getElementById("rate-input");
            if (!hidden) return;

            var stars = wrap.querySelectorAll(".star-rating__star");
            function setVisual(val) {
                stars.forEach(function (s) {
                    var v = parseInt(s.getAttribute("data-value"), 10);
                    s.classList.toggle("star-rating__star--active", v <= val);
                });
            }

            var initial = parseInt(hidden.value, 10) || 5;
            setVisual(initial);

            stars.forEach(function (s) {
                s.addEventListener("click", function () {
                    var val = parseInt(s.getAttribute("data-value"), 10);
                    hidden.value = String(val);
                    setVisual(val);
                });
            });
        });
    }

    /* ---- Theme toggle (light / dark / auto) ---- */
    function initTheme() {
        var STORAGE_KEY = "gf-theme";
        var root = document.documentElement;

        function apply(theme) {
            if (theme === "light" || theme === "dark") {
                root.setAttribute("data-theme", theme);
            } else {
                root.removeAttribute("data-theme");
            }
        }

        var saved = null;
        try { saved = localStorage.getItem(STORAGE_KEY); } catch (_) {}
        apply(saved);

        function currentEffective() {
            var explicit = root.getAttribute("data-theme");
            if (explicit === "light" || explicit === "dark") return explicit;
            return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
        }

        function updateLabel(btn) {
            var eff = currentEffective();
            btn.setAttribute("aria-label", "Switch to " + (eff === "dark" ? "light" : "dark") + " mode");
            btn.textContent = eff === "dark" ? "☀ Light" : "☾ Dark";
        }

        document.querySelectorAll(".js-theme-toggle").forEach(function (btn) {
            updateLabel(btn);
            btn.addEventListener("click", function () {
                var next = currentEffective() === "dark" ? "light" : "dark";
                apply(next);
                try { localStorage.setItem(STORAGE_KEY, next); } catch (_) {}
                document.querySelectorAll(".js-theme-toggle").forEach(updateLabel);
            });
        });
    }

    /* ---- Mobile sidebar drawer ---- */
    function initSidebarDrawer() {
        var sidebar = document.querySelector(".sidebar");
        var toggle = document.querySelector(".js-menu-toggle");
        var backdrop = document.querySelector(".js-sidebar-backdrop");
        if (!sidebar || !toggle) return;

        function open() {
            sidebar.classList.add("is-open");
            if (backdrop) backdrop.classList.add("is-open");
            toggle.setAttribute("aria-expanded", "true");
        }
        function close() {
            sidebar.classList.remove("is-open");
            if (backdrop) backdrop.classList.remove("is-open");
            toggle.setAttribute("aria-expanded", "false");
        }
        toggle.addEventListener("click", function () {
            sidebar.classList.contains("is-open") ? close() : open();
        });
        if (backdrop) backdrop.addEventListener("click", close);
        sidebar.querySelectorAll("a.nav-link").forEach(function (a) {
            a.addEventListener("click", close);
        });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && sidebar.classList.contains("is-open")) close();
        });
    }

    /* ---- Count-up: numbers tween from 0 to their final value on first paint ---- */
    function initCountUp() {
        // Respect the user's motion preference.
        if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
        var nodes = document.querySelectorAll("[data-count-up]");
        if (!nodes.length) return;
        var DURATION = 700;
        Array.prototype.forEach.call(nodes, function (el) {
            var attrVal = el.getAttribute("data-count-up");
            var raw = (attrVal && attrVal.length ? attrVal : (el.textContent || "")).trim();
            var hadComma = raw.indexOf(",") !== -1;
            var hasDecimal = raw.indexOf(".") !== -1;
            var target = parseFloat(raw.replace(/,/g, ""));
            if (isNaN(target)) return;
            var fmt = function (v) {
                if (hasDecimal) return v.toFixed(1);
                var n = Math.round(v);
                // Re-comma 4-digit+ numbers if the source had thousands separators.
                return hadComma ? n.toLocaleString("en-US") : n.toString();
            };
            el.textContent = fmt(0);
            var startedAt = performance.now();
            function frame(now) {
                var t = Math.min((now - startedAt) / DURATION, 1);
                // ease-out cubic
                var eased = 1 - Math.pow(1 - t, 3);
                el.textContent = fmt(target * eased);
                if (t < 1) requestAnimationFrame(frame);
                else el.textContent = fmt(target);
            }
            requestAnimationFrame(frame);
        });
    }

    /* ---- Chat: scroll-to-bottom on load, auto-resize composer, send-on-Enter,
           optimistic AJAX submit, polling for cross-device live updates,
           conversation list filter. Telegram-style. ---- */
    function initChatPage() {
        var scrollEl = document.querySelector(".js-chat-scroll");
        var compose  = document.querySelector(".js-chat-compose");
        var input    = document.querySelector(".js-chat-input");
        var sendBtn  = document.querySelector(".js-chat-send");
        var filterEl = document.querySelector(".js-conv-filter");

        // 1. Scroll the message log to the bottom on first paint so the latest
        //    message is visible without manual scrolling.
        if (scrollEl) {
            scrollEl.scrollTop = scrollEl.scrollHeight;
        }

        // 2. Composer: auto-grow textarea + enable send button when there's
        //    content + send-on-Enter (Shift+Enter newline).
        if (input && compose) {
            function syncSendDisabled() {
                if (sendBtn) sendBtn.disabled = input.value.trim().length === 0;
            }
            function autosize() {
                input.style.height = "auto";
                input.style.height = Math.min(input.scrollHeight, 140) + "px";
            }
            input.addEventListener("input", function () { autosize(); syncSendDisabled(); });
            input.addEventListener("keydown", function (e) {
                if (e.key === "Enter" && !e.shiftKey && !e.isComposing) {
                    e.preventDefault();
                    if (!sendBtn.disabled) compose.requestSubmit();
                }
            });
            input.focus();
            syncSendDisabled();

            // 3. Optimistic AJAX submit. v0.6.37 fix: Ktor's
            //    call.receiveParameters() only parses application/x-www-form-urlencoded.
            //    Sending FormData here would default to multipart/form-data and the
            //    server would silently see message="" — messages dropped, never saved.
            //    URLSearchParams(FormData) gives us urlencoded explicitly.
            compose.addEventListener("submit", function (e) {
                if (sendBtn.disabled) { e.preventDefault(); return; }
                var text = input.value.trim();
                if (!text) { e.preventDefault(); return; }
                e.preventDefault();
                appendOptimisticBubble(scrollEl, text);
                input.value = "";
                autosize();
                syncSendDisabled();

                // v0.6.39 — bulletproof AJAX send.
                //  • body: explicit URLSearchParams string (works around any
                //    browser quirks with FormData → urlencoded conversion).
                //  • Accept: application/json so the server returns
                //    {ok: true|false} instead of an opaque 302 redirect.
                //  • credentials: "same-origin" makes the session cookie
                //    explicit (defaults are usually fine; this is belt+braces).
                //  • response.ok check + console logging so failures are
                //    visible in DevTools instead of silently flipping the
                //    bubble to failed.
                var msgValue = (compose.querySelector("[name=message]") || {}).value || text;
                var bodyStr = "message=" + encodeURIComponent(msgValue);
                console.info("[chat] sending to", compose.action, "len=", msgValue.length);
                fetch(compose.action, {
                    method: "POST",
                    body: bodyStr,
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                        "Accept": "application/json"
                    },
                    credentials: "same-origin"
                })
                    .then(function (r) {
                        if (!r.ok) throw new Error("HTTP " + r.status);
                        return r.json().catch(function () { return { ok: true }; });
                    })
                    .then(function (result) {
                        var pending = scrollEl.querySelector(".bubble--pending");
                        if (result && result.ok === false) {
                            console.warn("[chat] server rejected message (empty/blocked)");
                            if (pending) {
                                pending.classList.remove("bubble--pending");
                                pending.classList.add("bubble--failed");
                            }
                        } else {
                            console.info("[chat] sent ok");
                            if (pending) pending.classList.remove("bubble--pending");
                        }
                    })
                    .catch(function (err) {
                        console.error("[chat] send failed:", err);
                        var pending = scrollEl.querySelector(".bubble--pending");
                        if (pending) {
                            pending.classList.remove("bubble--pending");
                            pending.classList.add("bubble--failed");
                        }
                    });
            });
        }

        // 4. Polling — cross-device live updates. Every 4s, ask the server for
        //    every message in this thread newer than the highest id we've
        //    rendered. Append new bubbles. Reconcile pending bubbles whose
        //    server-side counterpart has just landed.
        if (scrollEl && scrollEl.dataset.partnerId) {
            startChatPolling(scrollEl);
        }

        // 5. Conversation filter (substring match on data-name across both the
        //    existing-conversations list and the directory list).
        if (filterEl) {
            var rows = document.querySelectorAll(".conv-list__row");
            var emptyHint = document.querySelector(".chat-search__empty");
            filterEl.addEventListener("input", function () {
                var q = filterEl.value.trim().toLowerCase();
                var anyVisible = false;
                rows.forEach(function (row) {
                    var name = (row.getAttribute("data-name") || "").toLowerCase();
                    var hit = q === "" || name.indexOf(q) !== -1;
                    row.classList.toggle("is-hidden", !hit);
                    if (hit) anyVisible = true;
                });
                if (emptyHint) emptyHint.hidden = anyVisible || rows.length === 0;
            });
        }
    }

    /* Polling loop. ~4s interval. Stops when the tab is hidden, resumes on
       focus to avoid burning battery in background tabs. */
    function startChatPolling(scrollEl) {
        var partnerId = scrollEl.dataset.partnerId;
        if (!partnerId) return;

        function highestRenderedId() {
            var ids = Array.prototype.map.call(
                scrollEl.querySelectorAll(".bubble[data-message-id]"),
                function (b) { return parseInt(b.getAttribute("data-message-id"), 10) || 0; }
            );
            return ids.length ? Math.max.apply(null, ids) : 0;
        }

        function isAtBottom() {
            // Within 60px of the bottom = "user is reading the latest", auto-scroll on append.
            return (scrollEl.scrollHeight - scrollEl.scrollTop - scrollEl.clientHeight) < 60;
        }

        function poll() {
            if (document.hidden) return;
            var lastId = highestRenderedId();
            fetch("/api/messages/" + partnerId + "/since/" + lastId, { credentials: "same-origin" })
                .then(function (r) { return r.ok ? r.json() : []; })
                .then(function (messages) {
                    if (!Array.isArray(messages) || !messages.length) return;
                    var stickToBottom = isAtBottom();
                    messages.forEach(function (m) { reconcileOrAppend(scrollEl, m); });
                    if (stickToBottom) scrollEl.scrollTop = scrollEl.scrollHeight;
                })
                .catch(function () { /* swallow — next tick will retry */ });
        }

        var POLL_MS = 4000;
        var timer = setInterval(poll, POLL_MS);
        // First poll fires soon after page load so messages sent on the OTHER
        // device just before this one rendered get caught quickly.
        setTimeout(poll, 1500);

        // Pause polling while the tab is hidden; one immediate poll on focus.
        document.addEventListener("visibilitychange", function () {
            if (!document.hidden) poll();
        });

        // No explicit cleanup — page navigation discards the timer.
        return timer;
    }

    /* Either reconcile a pending optimistic bubble (mine, same text, no id yet)
       with the freshly-arrived server record, or append a new bubble. */
    function reconcileOrAppend(scrollEl, m) {
        // Already rendered — skip
        if (scrollEl.querySelector('[data-message-id="' + m.id + '"]')) return;

        if (m.isMine) {
            var pendingMatch = null;
            var candidates = scrollEl.querySelectorAll(".bubble--mine:not([data-message-id])");
            for (var i = 0; i < candidates.length; i++) {
                var t = candidates[i].querySelector(".bubble__text");
                if (t && t.textContent === m.message) { pendingMatch = candidates[i]; break; }
            }
            if (pendingMatch) {
                pendingMatch.setAttribute("data-message-id", String(m.id));
                pendingMatch.classList.remove("bubble--pending");
                return;
            }
        }

        // Fresh bubble — create + append
        var lastGroup = scrollEl.querySelector(".chat-day:last-of-type");
        if (!lastGroup) {
            var hint = scrollEl.querySelector(".empty-hint");
            if (hint) hint.remove();
            lastGroup = document.createElement("div");
            lastGroup.className = "chat-day";
            var sep = document.createElement("div");
            sep.className = "chat-day__sep";
            var sepInner = document.createElement("span");
            sepInner.textContent = "Today";
            sep.appendChild(sepInner);
            lastGroup.appendChild(sep);
            scrollEl.appendChild(lastGroup);
        }
        var bubble = document.createElement("div");
        bubble.className = m.isMine ? "bubble bubble--mine" : "bubble bubble--theirs";
        bubble.setAttribute("data-message-id", String(m.id));
        var p = document.createElement("p");
        p.className = "bubble__text";
        p.textContent = m.message;
        bubble.appendChild(p);
        var time = document.createElement("time");
        time.className = "bubble__time";
        time.textContent = m.sentTime || "";
        bubble.appendChild(time);
        lastGroup.appendChild(bubble);
    }

    function appendOptimisticBubble(scrollEl, text) {
        if (!scrollEl) return;
        // Build inside the latest .chat-day so the bubble lives under the right
        // date pill. If no group exists yet (first message of an empty thread),
        // create a "Today" group on the fly.
        var lastGroup = scrollEl.querySelector(".chat-day:last-of-type");
        if (!lastGroup) {
            // Remove any "No messages yet" empty hint
            var hint = scrollEl.querySelector(".empty-hint");
            if (hint) hint.remove();
            lastGroup = document.createElement("div");
            lastGroup.className = "chat-day";
            var sep = document.createElement("div");
            sep.className = "chat-day__sep";
            var sepInner = document.createElement("span");
            sepInner.textContent = "Today";
            sep.appendChild(sepInner);
            lastGroup.appendChild(sep);
            scrollEl.appendChild(lastGroup);
        }
        var bubble = document.createElement("div");
        bubble.className = "bubble bubble--mine bubble--pending";
        var p = document.createElement("p");
        p.className = "bubble__text";
        p.textContent = text;
        bubble.appendChild(p);
        var time = document.createElement("time");
        time.className = "bubble__time";
        var now = new Date();
        var hh = String(now.getHours()).padStart(2, "0");
        var mm = String(now.getMinutes()).padStart(2, "0");
        time.textContent = hh + ":" + mm;
        bubble.appendChild(time);
        lastGroup.appendChild(bubble);
        scrollEl.scrollTop = scrollEl.scrollHeight;
    }

    /* ---- Scroll reveals: landing-page IntersectionObserver. Adds .is-revealed
           when an element with [data-reveal] crosses ~15% of the viewport. ---- */
    function initScrollReveals() {
        var nodes = document.querySelectorAll("[data-reveal]");
        if (!nodes.length) return;
        // No-IO fallback or reduced-motion: just reveal everything immediately —
        // the CSS rule for [data-reveal] is no-op outside prefers-reduced-motion: no-preference.
        if (!("IntersectionObserver" in window) ||
            (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches)) {
            Array.prototype.forEach.call(nodes, function (el) { el.classList.add("is-revealed"); });
            return;
        }
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-revealed");
                    io.unobserve(entry.target);
                }
            });
        }, { threshold: 0.15, rootMargin: "0px 0px -10% 0px" });
        Array.prototype.forEach.call(nodes, function (el) { io.observe(el); });
    }

    /* Apply saved theme synchronously (before DOMContentLoaded) to avoid flash */
    try {
        var early = localStorage.getItem("gf-theme");
        if (early === "light" || early === "dark") {
            document.documentElement.setAttribute("data-theme", early);
        }
    } catch (_) {}

    document.addEventListener("DOMContentLoaded", function () {
        initAuthTabs();
        initFoodModal();
        initFoodPicker();
        initStarRating();
        initTheme();
        initSidebarDrawer();
        initCountUp();
        initScrollReveals();
        initChatPage();
    });
})();
