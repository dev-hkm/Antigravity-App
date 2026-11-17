(function () {
    if (window.__antiEffortPickerV139) return;
    window.__antiEffortPickerV139 = true;
    var touchTarget = null;
    function group(target) {
        if (!target || !target.closest) return null;
        var row = target.closest('[role="menuitem"][aria-haspopup="menu"]');
        return row && row.querySelector('[data-testid="model-selector-effort-group"], [data-testid="model-selector-context-window-submenu"]') ? row : null;
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
            if (menu.dataset.antiSized === '1') continue;
            menu.dataset.antiSized = '1';
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
    function hasPotentialMenu(mutations) {
        for (var i = 0; i < mutations.length; i++) {
            var added = mutations[i].addedNodes;
            for (var j = 0; j < added.length; j++) {
                var n = added[j];
                if (n.nodeType !== 1) continue;
                if ((n.matches && n.matches('[role="menu"], [data-radix-menu-content]')) ||
                    (n.querySelector && n.querySelector('[role="menu"], [data-radix-menu-content]'))) {
                    return true;
                }
            }
        }
        return false;
    }
    function installContextMenuSizing() {
        if (!document.getElementById('anti-context-menu-style')) {
            var style = document.createElement('style');
            style.id = 'anti-context-menu-style';
            style.textContent = [
                '.anti-context-menu-large { width: min(82vw, 440px) !important; max-width: min(82vw, 440px) !important; max-height: 70vh !important; overflow-y: auto !important; padding: 6px !important; }',
                '.anti-context-menu-large .anti-context-menu-item-large { min-width: 0 !important; min-height: 44px !important; padding: 8px 12px !important; gap: 10px !important; font-size: 14px !important; line-height: 1.25 !important; border-radius: 10px !important; }',
                '.anti-context-menu-large .anti-context-menu-item-large svg { width: 18px !important; height: 18px !important; flex: 0 0 18px !important; }',
                '.anti-model-menu-large { width: min(86vw, 480px) !important; max-width: min(86vw, 480px) !important; max-height: 72vh !important; overflow-y: auto !important; }'
            ].join('');
            (document.head || document.documentElement).appendChild(style);
        }
        sizeContextMenu();
        var rafScheduled = false;
        new MutationObserver(function (mutations) {
            if (rafScheduled || !hasPotentialMenu(mutations)) return;
            rafScheduled = true;
            requestAnimationFrame(function () {
                rafScheduled = false;
                sizeContextMenu();
            });
        }).observe(document.documentElement, { childList: true, subtree: true });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', installContextMenuSizing, { once: true });
    else installContextMenuSizing();
})();
