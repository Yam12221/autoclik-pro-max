package com.buttonrelocator.pro

import android.view.View

/**
 * Represents a remapping pair between a Floating Trigger Button (Fake button touched by user)
 * and a Floating Target Pointer (Location on the real game button).
 */
data class ButtonPair(
    val id: Int,
    var triggerX: Int,
    var triggerY: Int,
    var targetX: Int,
    var targetY: Int,
    var triggerView: View? = null,
    var targetView: View? = null,
    var label: String = "Gatillo $id"
)
