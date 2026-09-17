package com.hkm.antigravity.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test

class EffortPickerBridgeTest {
    @Test
    fun scriptUsesComposedPathAndReactHandlersInsteadOfRadixOnlySelectors() {
        val script = EffortPickerBridge.script

        assertTrue(script.contains("composedPath"))
        assertTrue(script.contains("__reactProps"))
        assertTrue(script.contains("elementFromPoint"))
        assertTrue(script.contains("MutationObserver"))
        assertTrue(script.contains("pointerdown"))
    }

    @Test
    fun scriptKeepsEffortChoicesClickable() {
        val script = EffortPickerBridge.script

        assertTrue(script.contains("low|medium|high|auto"))
        assertTrue(script.contains("data-anti-effort-armed"))
        assertTrue(script.contains("stopImmediatePropagation"))
    }
}
