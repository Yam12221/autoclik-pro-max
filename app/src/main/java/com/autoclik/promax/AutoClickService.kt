package com.autoclik.promax

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import kotlinx.coroutines.*

class AutoClickService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    
    private var controlPanelView: View? = null
    private val targets = mutableListOf<ClickTarget>()
    private var targetIdCounter = 1

    private var isPlaying = false
    private var isLocked = false
    private var isMinimized = false
    private val isAntiDetectEnabled = true

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        var instance: AutoClickService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        stopClicking()
        hideOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        stopClicking()
        hideOverlay()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // No-op
    }

    override fun onInterrupt() {
        // No-op
    }

    fun isOverlayShowing(): Boolean {
        return controlPanelView != null
    }

    fun showOverlay() {
        if (controlPanelView != null) return

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        val themedContext = ContextThemeWrapper(this, R.style.Theme_AutoclikProMax)
        val inflater = LayoutInflater.from(themedContext)
        controlPanelView = inflater.inflate(R.layout.layout_control_panel, null)

        setupControlPanelListeners(controlPanelView!!)

        windowManager.addView(controlPanelView, params)
        loadConfiguration()
        updateAddButtonVisibility()
    }

    fun hideOverlay() {
        // Clear click target overlays
        val targetsCopy = ArrayList(targets)
        for (target in targetsCopy) {
            removeTarget(target)
        }
        targets.clear()
        targetIdCounter = 1

        // Clear control panel overlay
        controlPanelView?.let {
            windowManager.removeView(it)
            controlPanelView = null
        }
        
        isPlaying = false
        isLocked = false
        isMinimized = false
    }

    private fun setupControlPanelListeners(view: View) {
        val imgDrag = view.findViewById<ImageView>(R.id.img_drag)
        val btnPlay = view.findViewById<ImageView>(R.id.btn_play)
        val btnAdd = view.findViewById<ImageView>(R.id.btn_add)
        val btnLock = view.findViewById<ImageView>(R.id.btn_lock)
        val btnMinimize = view.findViewById<ImageView>(R.id.btn_minimize)
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)

        // Control Panel Drag Listener
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        imgDrag.setOnTouchListener { _, event ->
            if (isLocked) return@setOnTouchListener false // Block panel dragging if locked

            val params = view.layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (initialX + (event.rawX - initialTouchX)).toInt()
                    params.y = (initialY + (event.rawY - initialTouchY)).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        btnPlay.setOnClickListener {
            if (isPlaying) {
                stopClicking()
            } else {
                startClicking()
            }
        }

        btnAdd.setOnClickListener {
            addNewTarget()
        }

        btnLock.setOnClickListener {
            isLocked = !isLocked
            if (isLocked) {
                btnLock.setImageResource(R.drawable.ic_lock_closed)
                btnLock.setColorFilter(getColor(R.color.accent))
                btnClose.isEnabled = false
                btnClose.alpha = 0.4f
                // Apply touchable flags to targets
                updateTargetFlags(interactive = false)
            } else {
                btnLock.setImageResource(R.drawable.ic_lock_open)
                btnLock.clearColorFilter()
                
                // Restore controls if not clicking
                if (!isPlaying) {
                    btnClose.isEnabled = true
                    btnClose.alpha = 1.0f
                    updateTargetFlags(interactive = true)
                }
            }
            updateAddButtonVisibility()
        }

        btnMinimize.setOnClickListener {
            isMinimized = !isMinimized
            if (isMinimized) {
                btnPlay.visibility = View.GONE
                btnLock.visibility = View.GONE
                btnClose.visibility = View.GONE
                btnMinimize.setColorFilter(getColor(R.color.accent))

                // Fade targets to avoid blocking sight
                targets.forEach { it.view?.alpha = 0.2f }
            } else {
                btnPlay.visibility = View.VISIBLE
                btnLock.visibility = View.VISIBLE
                btnClose.visibility = View.VISIBLE
                btnMinimize.clearColorFilter()

                // Restore targets opacity
                targets.forEach { it.view?.alpha = 1.0f }
            }
            updateAddButtonVisibility()
        }

        btnClose.setOnClickListener {
            hideOverlay()
        }
    }

    private fun addNewTarget() {
        val id = targetIdCounter++
        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 250
        params.y = 400

        val themedContext = ContextThemeWrapper(this, R.style.Theme_AutoclikProMax)
        val inflater = LayoutInflater.from(themedContext)
        val targetView = inflater.inflate(R.layout.layout_click_target, null)
        val txtNumber = targetView.findViewById<TextView>(R.id.txt_target_number)
        txtNumber.text = id.toString()

        val target = ClickTarget(
            id = id,
            x = params.x,
            y = params.y,
            view = targetView
        )

        targetView.setOnTouchListener(createTargetTouchListener(target, params))
        targets.add(target)

        windowManager.addView(targetView, params)
        saveConfiguration()
    }

    private fun removeTarget(target: ClickTarget) {
        target.job?.cancel()
        target.view?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View might already be detached
            }
        }
        targets.remove(target)
    }

    private fun createTargetTouchListener(target: ClickTarget, params: WindowManager.LayoutParams): View.OnTouchListener {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        return View.OnTouchListener { view, event ->
            // Prevent interaction if currently running clicks or locked
            if (isPlaying || isLocked) return@OnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }

                    params.x = (initialX + dx).toInt()
                    params.y = (initialY + dy).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        showTargetSettingsDialog(target)
                    } else {
                        target.x = params.x
                        target.y = params.y
                        saveConfiguration()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showTargetSettingsDialog(target: ClickTarget) {
        if (isPlaying || isLocked) return

        val dialogContext = ContextThemeWrapper(this, R.style.Theme_AutoclikProMax)
        val builder = AlertDialog.Builder(dialogContext)
        val inflater = LayoutInflater.from(dialogContext)
        val dialogView = inflater.inflate(R.layout.dialog_target_settings, null)

        val txtTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val editMinutes = dialogView.findViewById<EditText>(R.id.edit_minutes)
        val editSeconds = dialogView.findViewById<EditText>(R.id.edit_seconds)
        val editMilliseconds = dialogView.findViewById<EditText>(R.id.edit_milliseconds)
        
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_delete_target)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_target)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save_target)

        txtTitle.text = getString(R.string.dialog_settings_title, target.id)
        
        // Calculate minutes, seconds and milliseconds from total target interval
        val totalMs = target.intervalMs
        val mins = totalMs / 60000L
        val remaining = totalMs % 60000L
        val secs = remaining / 1000L
        val ms = remaining % 1000L

        editMinutes.setText(mins.toString())
        editSeconds.setText(secs.toString())
        editMilliseconds.setText(ms.toString())

        val dialog = builder.setView(dialogView).create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            removeTarget(target)
            saveConfiguration()
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val minsVal = editMinutes.text.toString().toLongOrNull() ?: 0L
            val secsVal = editSeconds.text.toString().toLongOrNull() ?: 0L
            val msVal = editMilliseconds.text.toString().toLongOrNull() ?: 0L

            val calculatedTotalMs = (minsVal * 60000L) + (secsVal * 1000L) + msVal
            if (calculatedTotalMs >= 10L) {
                target.intervalMs = calculatedTotalMs
                saveConfiguration()
                dialog.dismiss()
            } else {
                editMilliseconds.error = getString(R.string.invalid_interval)
            }
        }

        dialog.show()
    }

    private fun startClicking() {
        if (targets.isEmpty()) return

        isPlaying = true
        controlPanelView?.findViewById<ImageView>(R.id.btn_play)?.setImageResource(R.drawable.ic_pause)

        // Lock interface actions to protect overlays
        controlPanelView?.findViewById<ImageView>(R.id.btn_close)?.let {
            it.isEnabled = false
            it.alpha = 0.4f
        }
        
        updateAddButtonVisibility()

        // Set layout flags on targets so they pass-through touches
        updateTargetFlags(interactive = false)

        // Start independent coroutine for each target
        for (target in targets) {
            target.job = serviceScope.launch {
                // Dispatch click at target center relative to screen
                while (isActive && isPlaying) {
                    val view = target.view
                    if (view != null) {
                        val location = IntArray(2)
                        view.getLocationOnScreen(location)
                        var clickX = location[0] + view.width / 2
                        var clickY = location[1] + view.height / 2
                        
                        if (isAntiDetectEnabled) {
                            // Add small random tremor coordinates offset (-4 to +4 pixels)
                            clickX += (-4..4).random()
                            clickY += (-4..4).random()
                        }
                        
                        dispatchClickAt(clickX, clickY)
                    }
                    
                    val baseDelay = target.intervalMs
                    val delayMs = if (isAntiDetectEnabled && baseDelay > 200L) {
                        // Add random time jitter (-10% to +10% of base interval)
                        val maxJitter = (baseDelay * 0.1).toLong()
                        baseDelay + (-maxJitter..maxJitter).random()
                    } else {
                        baseDelay
                    }
                    
                    delay(maxOf(10L, delayMs))
                }
            }
        }
    }

    private fun stopClicking() {
        isPlaying = false
        controlPanelView?.findViewById<ImageView>(R.id.btn_play)?.setImageResource(R.drawable.ic_play)

        // Restore interface options
        if (!isLocked) {
            controlPanelView?.findViewById<ImageView>(R.id.btn_close)?.let {
                it.isEnabled = true
                it.alpha = 1.0f
            }
            updateTargetFlags(interactive = true)
        }
        
        updateAddButtonVisibility()

        // Stop all clicking coroutines
        for (target in targets) {
            target.job?.cancel()
            target.job = null
        }
    }

    private fun updateTargetFlags(interactive: Boolean) {
        for (target in targets) {
            val view = target.view ?: continue
            val params = view.layoutParams as WindowManager.LayoutParams
            if (interactive) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            } else {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            }
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                // In case view is detached
            }
        }
    }

    private fun dispatchClickAt(x: Int, y: Int) {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        
        val gestureBuilder = GestureDescription.Builder()
        // Clicks need a brief stroke duration to register on Android correctly. 50ms is ideal.
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun updateAddButtonVisibility() {
        val view = controlPanelView ?: return
        val btnAdd = view.findViewById<ImageView>(R.id.btn_add) ?: return
        val prefs = getSharedPreferences("AutoclikPrefs", MODE_PRIVATE)
        val showAddSetting = prefs.getBoolean("show_add_button", true)

        if (isMinimized || isPlaying || isLocked || !showAddSetting) {
            btnAdd.visibility = View.GONE
        } else {
            btnAdd.visibility = View.VISIBLE
            btnAdd.isEnabled = true
            btnAdd.alpha = 1.0f
        }
    }

    private fun saveConfiguration() {
        val prefs = getSharedPreferences("AutoclikPrefs", MODE_PRIVATE)
        val activeProfileId = prefs.getInt("active_profile_id", 1)
        val serialized = targets.joinToString("|") { "${it.id};${it.x};${it.y};${it.intervalMs}" }
        prefs.edit().putString("saved_targets_$activeProfileId", serialized).apply()
    }

    private fun loadConfiguration() {
        val prefs = getSharedPreferences("AutoclikPrefs", MODE_PRIVATE)
        val activeProfileId = prefs.getInt("active_profile_id", 1)
        val serialized = prefs.getString("saved_targets_$activeProfileId", "") ?: ""
        if (serialized.isEmpty()) return

        // Clear current screen targets if any (to avoid duplicates)
        val targetsCopy = ArrayList(targets)
        for (t in targetsCopy) {
            removeTarget(t)
        }
        targets.clear()

        var maxId = 0
        val parts = serialized.split("|")
        for (part in parts) {
            val fields = part.split(";")
            if (fields.size == 4) {
                val id = fields[0].toIntOrNull() ?: continue
                val x = fields[1].toIntOrNull() ?: continue
                val y = fields[2].toIntOrNull() ?: continue
                val intervalMs = fields[3].toLongOrNull() ?: continue

                if (id > maxId) maxId = id

                restoreTarget(id, x, y, intervalMs)
            }
        }
        targetIdCounter = maxId + 1
    }

    private fun restoreTarget(id: Int, x: Int, y: Int, intervalMs: Long) {
        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y

        val themedContext = ContextThemeWrapper(this, R.style.Theme_AutoclikProMax)
        val inflater = LayoutInflater.from(themedContext)
        val targetView = inflater.inflate(R.layout.layout_click_target, null)
        val txtNumber = targetView.findViewById<TextView>(R.id.txt_target_number)
        txtNumber.text = id.toString()

        val target = ClickTarget(
            id = id,
            x = x,
            y = y,
            intervalMs = intervalMs,
            view = targetView
        )

        targetView.setOnTouchListener(createTargetTouchListener(target, params))
        targets.add(target)

        windowManager.addView(targetView, params)
    }
}
