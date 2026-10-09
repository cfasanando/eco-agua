(function () {
    'use strict';

    var STORAGE_KEY = 'matrix26-theme';

    function resolveInitialTheme() {
        try {
            var stored = localStorage.getItem(STORAGE_KEY);
            if (stored === 'dark' || stored === 'light') {
                return stored;
            }
            if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
                return 'dark';
            }
        } catch (ignored) {
            return 'light';
        }
        return 'light';
    }

    function persistTheme(theme) {
        try {
            localStorage.setItem(STORAGE_KEY, theme);
        } catch (ignored) {
            // Storage may be blocked in private mode. The UI can still update for the current page.
        }
    }

    function updateToggle(theme) {
        var toggle = document.querySelector('[data-matrix26-theme-toggle]');
        if (!toggle) {
            return;
        }
        var icon = toggle.querySelector('[data-matrix26-theme-icon]');
        var label = toggle.querySelector('[data-matrix26-theme-label]');
        var isDark = theme === 'dark';
        toggle.setAttribute('aria-pressed', String(isDark));
        toggle.setAttribute('title', isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
        if (icon) {
            icon.className = isDark ? 'bi bi-sun' : 'bi bi-moon-stars';
        }
        if (label) {
            label.textContent = isDark ? 'Modo claro' : 'Modo oscuro';
        }
    }

    function applyTheme(theme) {
        var normalizedTheme = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-matrix26-theme', normalizedTheme);
        document.documentElement.setAttribute('data-bs-theme', normalizedTheme);
        updateToggle(normalizedTheme);
        persistTheme(normalizedTheme);
    }

    function init() {
        applyTheme(resolveInitialTheme());
        var toggle = document.querySelector('[data-matrix26-theme-toggle]');
        if (!toggle) {
            return;
        }
        toggle.addEventListener('click', function () {
            var current = document.documentElement.getAttribute('data-matrix26-theme') || 'light';
            applyTheme(current === 'dark' ? 'light' : 'dark');
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
