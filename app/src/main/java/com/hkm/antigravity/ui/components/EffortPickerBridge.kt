package com.hkm.antigravity.ui.components

internal object EffortPickerBridge {
    val allowedOrigins = setOf("https://antigravity.static.usercontent.goog")

    // Installed at document start in the remote-control iframe, not the Angular host.
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
                        var modelItems = menu.querySelectorAll('[role="menuitem"], [data-testid="model-selector-item"], [data-testid="model-selector-effort-group"]');
                        for (var m = 0; m < modelItems.length; m++) modelItems[m].classList.add('anti-model-item-large');
                        installModelSearch(menu);
                    }
                    if (!/add\s+context/i.test(menu.textContent || '')) continue;
                    menu.classList.add('anti-context-menu-large');
                    var items = menu.querySelectorAll('[role="menuitem"], button');
                    for (var j = 0; j < items.length; j++) items[j].classList.add('anti-context-menu-item-large');
                }
            }
            function installModelSearch(menu) {
                if (menu.querySelector('.anti-model-search')) return;
                var input = document.createElement('input');
                input.className = 'anti-model-search';
                input.type = 'search';
                input.placeholder = 'Search models';
                input.setAttribute('aria-label', 'Search models');
                input.autocomplete = 'off';
                input.addEventListener('click', function (event) { event.stopPropagation(); });
                input.addEventListener('keydown', function (event) { event.stopPropagation(); });
                input.addEventListener('input', function () {
                    var query = input.value.trim().toLowerCase();
                    var rows = menu.querySelectorAll('[role="menuitem"], [data-testid="model-selector-item"], [data-testid="model-selector-effort-group"]');
                    for (var i = 0; i < rows.length; i++) {
                        var row = rows[i];
                        var label = (row.textContent || '').replace(/\s+/g, ' ').toLowerCase();
                        row.style.display = !query || label.indexOf(query) !== -1 ? '' : 'none';
                    }
                });
                menu.insertBefore(input, menu.firstChild);
            }
            function installContextMenuSizing() {
                if (document.getElementById('anti-context-menu-style')) return;
                var style = document.createElement('style');
                style.id = 'anti-context-menu-style';
                style.textContent = [
                    '.anti-context-menu-large { width: min(88vw, 560px) !important; max-width: min(88vw, 560px) !important; max-height: 76vh !important; overflow-y: auto !important; padding: 10px !important; }',
                    '.anti-context-menu-large .anti-context-menu-item-large { min-width: 0 !important; min-height: 56px !important; padding: 14px 16px !important; gap: 14px !important; font-size: 15px !important; line-height: 1.35 !important; border-radius: 12px !important; }',
                    '.anti-context-menu-large .anti-context-menu-item-large svg { width: 20px !important; height: 20px !important; flex: 0 0 20px !important; }'
                    ,'.anti-model-menu-large { width: min(92vw, 620px) !important; max-width: min(92vw, 620px) !important; max-height: 78vh !important; overflow-y: auto !important; padding: 10px !important; }',
                    '.anti-model-menu-large .anti-model-item-large { min-width: 0 !important; min-height: 56px !important; padding: 14px 16px !important; gap: 14px !important; border-radius: 12px !important; }',
                    '.anti-model-search { display: block !important; box-sizing: border-box !important; width: 100% !important; height: 48px !important; margin: 0 0 8px !important; padding: 0 14px !important; border: 1px solid color-mix(in srgb, currentColor 22%, transparent) !important; border-radius: 12px !important; background: transparent !important; color: inherit !important; font: inherit !important; font-size: 15px !important; outline: none !important; }',
                    '.anti-model-search:focus { border-color: currentColor !important; box-shadow: 0 0 0 2px color-mix(in srgb, currentColor 18%, transparent) !important; }'
                ].join('');
                (document.head || document.documentElement).appendChild(style);
                sizeContextMenu();
                new MutationObserver(sizeContextMenu).observe(document.documentElement, { childList: true, subtree: true });
            }
            if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', installContextMenuSizing, { once: true });
            else installContextMenuSizing();
        })();
    """.trimIndent()
}
