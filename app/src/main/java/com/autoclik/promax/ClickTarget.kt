package com.autoclik.promax

import android.view.View
import kotlinx.coroutines.Job

data class ClickTarget(
    val id: Int,
    var x: Int,
    var y: Int,
    var intervalMs: Long = 1000L,
    var view: View? = null,
    var job: Job? = null
)
