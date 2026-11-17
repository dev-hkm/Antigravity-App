package com.hkm.antigravity.ui.components

internal object EffortPickerBridge {
    val allowedOrigins = setOf(
        "https://antigravity.static.usercontent.goog",
        "https://antigravity.google.com"
    )

    // Installed at document start in the remote-control frame/host, not via evaluateJavascript.
    // Use Base UI's public keyboard behavior; never invoke private React handlers.
    val script = """
        (function () {
            if (window.__antiEffortPickerV139) return;
            window.__antiEffortPickerV139 = true;
            var touchTarget = null;
            function group(target) {
                if (!target || !target.closest) return null;
                var row = target.closest('[role="menuitem"][aria-haspopup="menu"]');
                return row && row.querySelector('[data-testid="model-selector-effort-group"]') ? row : null;
            }
            document.addEventListener('pointerdown', function (event) {
                touchTarget = event.pointerType === 'touch' || event.pointerType === 'pen'
                    ? group(event.target) : null;
            }, true);
            document.addEventListener('pointercancel', function () { touchTarget = null; }, true);
            document.addEventListener('click', function (event) {
                var row = group(event.target);
                var touch = event.pointerType === 'touch' || event.pointerType === 'pen' ||
                    (!event.pointerType && event.detail > 0 && row === touchTarget);
                touchTarget = null;
                if (!row || !touch || row.getAttribute('aria-disabled') === 'true' || row.hasAttribute('disabled')) return;
                event.preventDefault();
                event.stopImmediatePropagation();
                row.focus({ preventScroll: true });
                var key = getComputedStyle(row).direction === 'rtl' ? 'ArrowLeft' : 'ArrowRight';
                row.dispatchEvent(new KeyboardEvent('keydown', {
                    key: key, code: key, bubbles: true, cancelable: true,
                    keyCode: key === 'ArrowRight' ? 39 : 37,
                    which: key === 'ArrowRight' ? 39 : 37
                }));
            }, true);

            function sizeContextMenu() {
                var menus = document.querySelectorAll('[role="menu"], [data-radix-menu-content]');
                for (var i = 0; i < menus.length; i++) {
                    var menu = menus[i];
                    if (menu.querySelector('[data-testid="model-selector-panel"]') ||
                        menu.matches('[data-testid="model-selector-panel"]')) {
                        menu.classList.add('anti-model-menu-large');
                    }
                    if (!/add\s+context/i.test(menu.textContent || '')) continue;
                    menu.classList.add('anti-context-menu-large');
                    var items = menu.querySelectorAll('[role="menuitem"], button');
                    for (var j = 0; j < items.length; j++) items[j].classList.add('anti-context-menu-item-large');
                }
            }
            var menuResizeRaf = 0;
            function scheduleSizeContextMenu() {
                if (menuResizeRaf) return;
                menuResizeRaf = requestAnimationFrame(function () {
                    menuResizeRaf = 0;
                    sizeContextMenu();
                });
            }
            function installContextMenuSizing() {
                if (document.getElementById('anti-context-menu-style')) return;
                var style = document.createElement('style');
                style.id = 'anti-context-menu-style';
                style.textContent = [
                    '.anti-context-menu-large { width: min(82vw, 440px) !important; max-width: min(82vw, 440px) !important; max-height: 70vh !important; overflow-y: auto !important; padding: 6px !important; }',
                    '.anti-context-menu-large .anti-context-menu-item-large { min-width: 0 !important; min-height: 48px !important; padding: 10px 12px !important; gap: 10px !important; font-size: 14px !important; line-height: 1.25 !important; border-radius: 10px !important; }',
                    '.anti-context-menu-large .anti-context-menu-item-large svg { width: 18px !important; height: 18px !important; flex: 0 0 18px !important; }',
                    '.anti-model-menu-large { width: min(86vw, 500px) !important; max-width: min(86vw, 500px) !important; max-height: 72vh !important; overflow-y: auto !important; }'
                ].join('');
                (document.head || document.documentElement).appendChild(style);
                sizeContextMenu();
                new MutationObserver(function (mutations) {
                    for (var i = 0; i < mutations.length; i++) {
                        if (mutations[i].addedNodes && mutations[i].addedNodes.length > 0) {
                            scheduleSizeContextMenu();
                            return;
                        }
                    }
                }).observe(document.documentElement, { childList: true, subtree: true });
            }
            if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', installContextMenuSizing, { once: true });
            else installContextMenuSizing();
        })();
    """.trimIndent()
}
