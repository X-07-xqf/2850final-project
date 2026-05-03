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

    /* ---- Food search (/api/food-search?q=) ---- */
    function initFoodSearch() {
        var input = document.getElementById("food-search-input");
        var results = document.getElementById("food-search-results");
        var foodIdInput = document.getElementById("modal-food-item-id");
        var submitBtn = document.getElementById("modal-submit");
        if (!input || !results) return;

        // Minimum query length: 3 — matches the inline hint shown next to the
        // search input in subscriber/diary.html. Bumping this here without also
        // updating that hint would re-introduce the original UX problem
        // (testers typing 1-2 chars and seeing nothing happen — issue #38).
        var MIN_QUERY_LEN = 3;
        var t = null;
        input.addEventListener("input", function () {
            clearTimeout(t);
            var q = input.value.trim();
            if (q.length < MIN_QUERY_LEN) {
                results.innerHTML = "";
                results.classList.remove("is-visible");
                return;
            }
            t = setTimeout(function () {
                fetch("/api/food-search?q=" + encodeURIComponent(q))
                    .then(function (r) {
                        if (!r.ok) throw new Error("search failed");
                        return r.json();
                    })
                    .then(function (items) {
                        results.innerHTML = "";
                        if (!items || !items.length) {
                            results.classList.remove("is-visible");
                            return;
                        }
                        items.forEach(function (item) {
                            var b = document.createElement("button");
                            b.type = "button";
                            b.setAttribute("role", "option");
                            b.textContent = item.name + " — " + item.calories + " kcal/100g";
                            b.addEventListener("click", function () {
                                if (foodIdInput) foodIdInput.value = String(item.id);
                                if (submitBtn) submitBtn.disabled = false;
                                input.value = item.name;
                                results.innerHTML = "";
                                results.classList.remove("is-visible");
                            });
                            results.appendChild(b);
                        });
                        results.classList.add("is-visible");
                    })
                    .catch(function () {
                        results.innerHTML = "";
                        results.classList.remove("is-visible");
                    });
            }, 250);
        });
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
        initFoodSearch();
        initStarRating();
        initTheme();
        initSidebarDrawer();
        initCountUp();
        initScrollReveals();
    });
})();
