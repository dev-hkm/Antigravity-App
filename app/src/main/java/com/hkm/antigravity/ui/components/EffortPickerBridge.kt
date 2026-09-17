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
        })();
    """.trimIndent()
}
