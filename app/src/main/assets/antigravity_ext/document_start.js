(function () {
    if (window.__antiDocStartV1110) return;
    window.__antiDocStartV1110 = true;

    var config = {
        isWebDark: null,
        isDesktopMode: false,
        autoConnect: false
    };
    var autoConnected = false;

    function applyTheme(dark) {
        if (typeof dark !== 'boolean') return;
        var scheme = dark ? 'dark' : 'light';
        try {
            localStorage.setItem('bcsp', scheme);
            localStorage.setItem('theme', scheme);
            localStorage.setItem('themeMode', scheme);
        } catch (e) {}

        document.documentElement.style.colorScheme = scheme;
        document.documentElement.setAttribute('data-theme', scheme);
        if (document.body) {
            document.body.classList.toggle('dark-theme', dark);
            if (document.body.classList.contains('theme-standalone') ||
                document.querySelector('.theme-standalone')) {
                document.body.classList.toggle('dark', dark);
                document.body.classList.toggle('theme-dark', dark);
                document.body.classList.toggle('theme-light', !dark);
            }
        }
    }

    var MOBILE_VIEWPORT = 'width=device-width, initial-scale=1.0, viewport-fit=cover, interactive-widget=resizes-content';
    function applyViewport() {
        var meta = document.querySelector('meta[name="viewport"]');
        if (!meta) return false;
        var target = config.isDesktopMode ? 'width=1100' : MOBILE_VIEWPORT;
        if (config.isDesktopMode && meta.getAttribute('content') !== 'width=1100') {
            meta.setAttribute('content', 'width=1100');
        } else if (!config.isDesktopMode && meta.getAttribute('content') === 'width=1100') {
            meta.setAttribute('content', target);
        }
        return true;
    }

    function runAutoConnect() {
        if (!config.autoConnect || autoConnected || window !== window.top) return;
        function tryConnect() {
            if (!config.autoConnect || autoConnected) return false;
            var els = document.querySelectorAll('button, a, [role="button"]');
            for (var i = 0; i < els.length; i++) {
                var el = els[i];
                var t = (el.textContent || '').trim().toLowerCase();
                var a = (el.getAttribute('aria-label') || '').toLowerCase();
                var textIsConnect = /(^|\s)connect(\s|$)/.test(t);
                var ariaIsConnect = /(^|\s)connect(\s|$)/.test(a);
                if ((textIsConnect || ariaIsConnect) &&
                    el.offsetParent !== null && !el.disabled &&
                    el.getAttribute('aria-disabled') !== 'true') {
                    autoConnected = true;
                    el.click();
                    return true;
                }
            }
            return false;
        }
        if (tryConnect()) return;
        var pendingTimer = null;
        var obs = new MutationObserver(function () {
            if (!config.autoConnect || autoConnected) {
                obs.disconnect();
                return;
            }
            if (pendingTimer) return;
            pendingTimer = setTimeout(function () {
                pendingTimer = null;
                if (tryConnect()) obs.disconnect();
            }, 250);
        });
        if (document.body) {
            obs.observe(document.body, { childList: true, subtree: true });
        }
        setTimeout(function () {
            obs.disconnect();
            if (pendingTimer) clearTimeout(pendingTimer);
        }, 10000);
    }

    function applyConfig(next) {
        if (!next || typeof next !== 'object') return;
        if (typeof next.isWebDark === 'boolean') {
            config.isWebDark = next.isWebDark;
            applyTheme(next.isWebDark);
        }
        if (typeof next.isDesktopMode === 'boolean') {
            config.isDesktopMode = next.isDesktopMode;
            applyViewport();
        }
        if (typeof next.autoConnect === 'boolean') {
            config.autoConnect = next.autoConnect;
            if (next.autoConnect && document.readyState !== 'loading') {
                runAutoConnect();
            }
        }
    }

    // Watch <head> for viewport meta when desktop mode is toggled early.
    var headObs = new MutationObserver(function () {
        if (config.isDesktopMode && applyViewport()) {
            headObs.disconnect();
        }
    });
    headObs.observe(document.documentElement, { childList: true, subtree: true });
    setTimeout(function () { headObs.disconnect(); }, 5000);

    // Allow tapping outside the slide-over sidebar drawer on narrow viewports to dismiss it.
    if (window === window.top) {
        document.addEventListener('click', function (event) {
            if (window.innerWidth >= 768) return;
            var split = document.querySelector('[data-testid="sidebar-split"]');
            if (!split) return;
            var drawer = split.firstElementChild;
            if (!drawer) return;
            var cs = getComputedStyle(drawer);
            if (cs.position !== 'fixed' || cs.visibility === 'hidden' || cs.pointerEvents === 'none') return;
            var rect = drawer.getBoundingClientRect();
            if (rect.width < 120 || event.clientX <= rect.right) return;
            var target = event.target instanceof Element ? event.target : null;
            if (target && target.closest('[role="menu"], [role="dialog"], [data-radix-menu-content]')) return;
            var toggleBtn = document.querySelector('button[data-testid="sidebar-toggle"][aria-expanded="true"], button[aria-label="Toggle Sidebar"][aria-expanded="true"]');
            if (toggleBtn && (!target || !toggleBtn.contains(target))) {
                toggleBtn.click();
            }
        }, true);
    }

    function onDomReady() {
        applyTheme(config.isWebDark);
        applyViewport();
        runAutoConnect();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', onDomReady, { once: true });
    } else {
        onDomReady();
    }

    try {
        var port = browser.runtime.connectNative('antigravity');
        port.onMessage.addListener(function (msg) {
            applyConfig(msg);
        });
        port.postMessage({ type: 'getConfig' });
    } catch (e) {
        try {
            browser.runtime.sendNativeMessage('antigravity', { type: 'getConfig' })
                .then(applyConfig)
                .catch(function () {});
        } catch (ignored) {}
    }
})();
