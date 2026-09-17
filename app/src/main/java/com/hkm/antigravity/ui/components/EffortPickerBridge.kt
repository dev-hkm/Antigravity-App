package com.hkm.antigravity.ui.components

internal object EffortPickerBridge {
    val script = """
        (function () {
            if (window.__antiEffortPickerV138) return;
            window.__antiEffortPickerV138 = true;

            var armedItem = null;
            var armedAt = 0;
            var swallowUntil = 0;
            var passThroughItem = null;
            var observer = null;

            function normalizedText(el) {
                return ((el && (el.innerText || el.textContent)) || '')
                    .replace(/\s+/g, ' ').trim().toLowerCase();
            }

            function isEffortChoice(el) {
                var text = normalizedText(el);
                return /^(low|medium|high|auto)(\s|$)/i.test(text) && text.length < 80;
            }

            function isVisible(el) {
                if (!el || !el.getBoundingClientRect) return false;
                var r = el.getBoundingClientRect();
                var s = window.getComputedStyle ? getComputedStyle(el) : null;
                return r.width > 1 && r.height > 1 && (!s || (s.display !== 'none' && s.visibility !== 'hidden'));
            }

            function eventPath(e) {
                if (e && typeof e.composedPath === 'function') return e.composedPath();
                var result = [];
                var n = e && e.target;
                while (n) { result.push(n); n = n.parentElement; }
                return result;
            }

            function popupAncestor(el) {
                var n = el;
                while (n && n !== document.body && n !== document.documentElement) {
                    if (n.nodeType === 1) {
                        var role = (n.getAttribute('role') || '').toLowerCase();
                        if (role === 'menu' || role === 'listbox' || role === 'dialog' ||
                            n.hasAttribute('data-radix-popper-content-wrapper') ||
                            n.hasAttribute('data-radix-menu-content') ||
                            n.hasAttribute('data-side')) return n;
                        var style = window.getComputedStyle ? getComputedStyle(n) : null;
                        if (style && (style.position === 'fixed' || style.position === 'absolute') && isVisible(n)) {
                            var rect = n.getBoundingClientRect();
                            if (rect.width < innerWidth * 0.98 && rect.height < innerHeight * 0.95) return n;
                        }
                    }
                    n = n.parentElement;
                }
                return null;
            }

            function interactiveCandidate(e) {
                var path = eventPath(e);
                for (var i = 0; i < path.length; i++) {
                    var el = path[i];
                    if (!el || el.nodeType !== 1 || !popupAncestor(el)) continue;
                    var role = (el.getAttribute('role') || '').toLowerCase();
                    if (role === 'menuitem' || role === 'menuitemradio' || role === 'option' ||
                        el.matches('button, [tabindex], li, [data-value], [data-state]')) return el;
                }
                var point = document.elementFromPoint(e.clientX || 0, e.clientY || 0);
                while (point && point !== document.body) {
                    if (popupAncestor(point) && normalizedText(point)) return point;
                    point = point.parentElement;
                }
                return null;
            }

            function reactProps(el) {
                if (!el) return null;
                var keys;
                try { keys = Object.getOwnPropertyNames(el); } catch (_) { return null; }
                for (var i = 0; i < keys.length; i++) {
                    if (keys[i].indexOf('__reactProps') === 0) return el[keys[i]];
                }
                return null;
            }

            function invokeReact(el, name, nativeEvent) {
                var n = el;
                while (n && n !== document.body) {
                    var props = reactProps(n);
                    var handler = props && props[name];
                    if (typeof handler === 'function') {
                        try {
                            handler({
                                type: nativeEvent.type,
                                target: el,
                                currentTarget: n,
                                nativeEvent: nativeEvent,
                                pointerType: 'mouse',
                                key: name === 'onKeyDown' ? 'ArrowRight' : undefined,
                                preventDefault: function () {},
                                stopPropagation: function () {},
                                persist: function () {}
                            });
                        } catch (_) {}
                    }
                    n = n.parentElement;
                }
            }

            function effortVisible() {
                var all = document.querySelectorAll('[role="menuitem"], [role="menuitemradio"], [role="option"], button, [tabindex]');
                for (var i = 0; i < all.length; i++) {
                    if (isVisible(all[i]) && isEffortChoice(all[i]) && popupAncestor(all[i])) return true;
                }
                return false;
            }

            function clearArmed() {
                if (armedItem && armedItem.removeAttribute) armedItem.removeAttribute('data-anti-effort-armed');
                armedItem = null;
                armedAt = 0;
            }

            function openEffort(item, originalEvent) {
                clearArmed();
                armedItem = item;
                armedAt = Date.now();
                swallowUntil = armedAt + 850;
                item.setAttribute('data-anti-effort-armed', 'true');
                try { item.focus({ preventScroll: true }); } catch (_) {}

                ['onPointerEnter', 'onPointerMove', 'onMouseEnter', 'onMouseMove', 'onFocus'].forEach(function (name) {
                    invokeReact(item, name, originalEvent);
                });
                invokeReact(item, 'onKeyDown', originalEvent);

                ['pointerover', 'pointerenter', 'pointermove', 'mouseover', 'mouseenter', 'mousemove'].forEach(function (name) {
                    try {
                        item.dispatchEvent(new PointerEvent(name, {
                            bubbles: true, cancelable: true, pointerType: 'mouse',
                            clientX: originalEvent.clientX || 0, clientY: originalEvent.clientY || 0
                        }));
                    } catch (_) {
                        try { item.dispatchEvent(new MouseEvent(name, { bubbles: true, cancelable: true })); } catch (_) {}
                    }
                });
                try { item.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', code: 'ArrowRight', bubbles: true })); } catch (_) {}
            }

            function block(e) {
                try { e.preventDefault(); } catch (_) {}
                try { e.stopPropagation(); } catch (_) {}
                try { e.stopImmediatePropagation(); } catch (_) {}
            }

            document.addEventListener('pointerdown', function (e) {
                if (e.pointerType && e.pointerType !== 'touch' && e.pointerType !== 'pen') return;
                var item = interactiveCandidate(e);
                if (!item || isEffortChoice(item)) { clearArmed(); return; }

                if (armedItem === item && Date.now() - armedAt < 4000) {
                    passThroughItem = item;
                    swallowUntil = 0;
                    clearArmed();
                    return;
                }

                passThroughItem = null;
                block(e);
                openEffort(item, e);
            }, { capture: true, passive: false });

            ['touchend', 'click'].forEach(function (name) {
                document.addEventListener(name, function (e) {
                    var item = interactiveCandidate(e);
                    if (passThroughItem && item === passThroughItem) {
                        if (name === 'click') passThroughItem = null;
                        return;
                    }
                    if (Date.now() < swallowUntil) block(e);
                }, { capture: true, passive: false });
            });

            observer = new MutationObserver(function () {
                if (armedItem && effortVisible()) {
                    swallowUntil = 0;
                    clearArmed();
                }
            });
            observer.observe(document.documentElement, { childList: true, subtree: true, attributes: true });

            var style = document.createElement('style');
            style.textContent = '[data-anti-effort-armed="true"]{outline:2px solid #3b82f6!important;outline-offset:-2px!important;background:rgba(59,130,246,.12)!important}';
            (document.head || document.documentElement).appendChild(style);
        })();
    """.trimIndent()
}
