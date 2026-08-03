package com.buttonrelocator.pro

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
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import kotlinx.coroutines.*

class RelocatorService : AccessibilityService() {

    private lateinit var windowManager: WindowManager

    private var controlPanelView: View? = null
    private val buttonPairs = mutableListOf<ButtonPair>()
    private var pairIdCounter = 1

    private var isLocked = false
    private var isMinimized = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        var instance: RelocatorService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        hideOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
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

        val themedContext = ContextThemeWrapper(this, R.style.Theme_ButtonRelocatorPro)
        val inflater = LayoutInflater.from(themedContext)
        controlPanelView = inflater.inflate(R.layout.layout_control_panel, null)

        setupControlPanelListeners(controlPanelView!!)

        windowManager.addView(controlPanelView, params)
        loadConfiguration()
    }

    fun hideOverlay() {
        val pairsCopy = ArrayList(buttonPairs)
        for (pair in pairsCopy) {
            removeRemapperPair(pair)
        }
        buttonPairs.clear()
        pairIdCounter = 1

        controlPanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            controlPanelView = null
        }

        isLocked = false
        isMinimized = false
    }

    private fun setupControlPanelListeners(view: View) {
        val imgDrag = view.findViewById<ImageView>(R.id.img_drag)
        val btnAddPair = view.findViewById<ImageView>(R.id.btn_add_pair)
        val btnLock = view.findViewById<ImageView>(R.id.btn_lock)
        val btnMinimize = view.findViewById<ImageView>(R.id.btn_minimize)
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val btnProfileBadge = view.findViewById<TextView>(R.id.btn_profile_badge)

        btnProfileBadge?.setOnClickListener {
            val prefs = getSharedPreferences("RelocatorPrefs", MODE_PRIVATE)
            val currentProfile = prefs.getInt("active_profile_id", 1)
            val nextProfile = if (currentProfile >= 3) 1 else currentProfile + 1
            prefs.edit().putInt("active_profile_id", nextProfile).apply()

            btnProfileBadge.text = "P$nextProfile"
            loadConfiguration()
            android.widget.Toast.makeText(this, "Perfil $nextProfile cargado", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Panel Drag Listener
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        imgDrag.setOnTouchListener { _, event ->
            if (isLocked) return@setOnTouchListener false

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

        btnAddPair.setOnClickListener {
            addNewRemapperPair()
        }

        btnLock.setOnClickListener {
            isLocked = !isLocked
            if (isLocked) {
                btnLock.setImageResource(R.drawable.ic_lock_closed)
                btnLock.setColorFilter(getColor(R.color.accent))
                btnClose.isEnabled = false
                btnClose.alpha = 0.4f
            } else {
                btnLock.setImageResource(R.drawable.ic_lock_open)
                btnLock.clearColorFilter()
                btnClose.isEnabled = true
                btnClose.alpha = 1.0f
            }
        }

        btnMinimize.setOnClickListener {
            isMinimized = !isMinimized
            if (isMinimized) {
                btnLock.visibility = View.GONE
                btnClose.visibility = View.GONE
                btnMinimize.setColorFilter(getColor(R.color.accent))

                buttonPairs.forEach { 
                    it.targetView?.alpha = 0.2f
                    it.triggerView?.alpha = 0.4f
                }
            } else {
                btnLock.visibility = View.VISIBLE
                btnClose.visibility = View.VISIBLE
                btnMinimize.clearColorFilter()

                buttonPairs.forEach { 
                    it.targetView?.alpha = 1.0f
                    it.triggerView?.alpha = 1.0f
                }
            }
        }

        btnClose.setOnClickListener {
            hideOverlay()
        }
    }

    fun addNewRemapperPair() {
        val id = pairIdCounter++
        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_ButtonRelocatorPro)
        val inflater = LayoutInflater.from(themedContext)

        // 1. Target Pointer View (On the game button)
        val targetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        targetParams.gravity = Gravity.TOP or Gravity.START
        targetParams.x = 400
        targetParams.y = 500

        val targetView = inflater.inflate(R.layout.layout_target_pointer, null)
        val txtTargetNum = targetView.findViewById<TextView>(R.id.txt_target_number)
        txtTargetNum.text = "R$id"

        // 2. Trigger View (Fake Floating Button touched by user thumb)
        val triggerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        triggerParams.gravity = Gravity.TOP or Gravity.START
        triggerParams.x = 150
        triggerParams.y = 700

        val triggerView = inflater.inflate(R.layout.layout_trigger_button, null)
        val txtTriggerLabel = triggerView.findViewById<TextView>(R.id.txt_trigger_label)
        txtTriggerLabel.text = "T$id"

        val pair = ButtonPair(
            id = id,
            triggerX = triggerParams.x,
            triggerY = triggerParams.y,
            targetX = targetParams.x,
            targetY = targetParams.y,
            triggerView = triggerView,
            targetView = targetView,
            label = "Gatillo $id"
        )

        targetView.setOnTouchListener(createPairTargetTouchListener(pair, targetParams))
        triggerView.setOnTouchListener(createTriggerTouchListener(pair, triggerParams))

        buttonPairs.add(pair)

        windowManager.addView(targetView, targetParams)
        windowManager.addView(triggerView, triggerParams)
        saveConfiguration()
    }

    private fun removeRemapperPair(pair: ButtonPair) {
        pair.targetView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        pair.triggerView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        buttonPairs.remove(pair)
    }

    private fun createTriggerTouchListener(pair: ButtonPair, params: WindowManager.LayoutParams): View.OnTouchListener {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        return View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true

                    // Trigger instant touch remap signal with anti-detection!
                    triggerRemapSignal(pair)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }

                    if (!isLocked) {
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isClick && !isLocked) {
                        pair.triggerX = params.x
                        pair.triggerY = params.y
                        saveConfiguration()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun createPairTargetTouchListener(pair: ButtonPair, params: WindowManager.LayoutParams): View.OnTouchListener {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        return View.OnTouchListener { view, event ->
            if (isLocked) return@OnTouchListener false

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
                        showPairDeleteDialog(pair)
                    } else {
                        pair.targetX = params.x
                        pair.targetY = params.y
                        saveConfiguration()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showPairDeleteDialog(pair: ButtonPair) {
        if (isLocked) return
        val dialogContext = ContextThemeWrapper(this, R.style.Theme_ButtonRelocatorPro)
        val builder = AlertDialog.Builder(dialogContext)
        builder.setTitle("Eliminar Reubicador T$id")
        builder.setMessage("¿Deseas eliminar la pareja del Botón Gatillo Flotante T${pair.id} y Puntero Real R${pair.id}?")
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            removeRemapperPair(pair)
            saveConfiguration()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.show()
    }

    private fun triggerRemapSignal(pair: ButtonPair) {
        val targetView = pair.targetView ?: return
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val targetCenterX = location[0] + targetView.width / 2
        val targetCenterY = location[1] + targetView.height / 2

        val prefs = getSharedPreferences("RelocatorPrefs", MODE_PRIVATE)
        val isAntiDetect = prefs.getBoolean("antidetections_enabled", true)
        val levelStr = prefs.getString("security_level", "STEALTH") ?: "STEALTH"
        val securityLevel = try {
            AntiDetectEngine.SecurityLevel.valueOf(levelStr)
        } catch (e: Exception) {
            AntiDetectEngine.SecurityLevel.STEALTH
        }
        val jitterRadius = prefs.getInt("jitter_radius", 4)

        val finalClick = if (isAntiDetect) {
            AntiDetectEngine.calculateClick(targetCenterX, targetCenterY, securityLevel, jitterRadius)
        } else {
            AntiDetectEngine.calculateClick(targetCenterX, targetCenterY, AntiDetectEngine.SecurityLevel.DIRECT, 0)
        }

        serviceScope.launch {
            if (finalClick.preDelayMs > 0) {
                delay(finalClick.preDelayMs)
            }
            dispatchHumanizedClickAt(finalClick.x, finalClick.y, finalClick.durationMs)
        }
    }

    private fun dispatchHumanizedClickAt(x: Int, y: Int, durationMs: Long = 1L) {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, maxOf(1L, durationMs)))
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun saveConfiguration() {
        val prefs = getSharedPreferences("RelocatorPrefs", MODE_PRIVATE)
        val activeProfileId = prefs.getInt("active_profile_id", 1)
        val pairsSerialized = buttonPairs.joinToString("|") { "${it.id};${it.triggerX};${it.triggerY};${it.targetX};${it.targetY}" }
        prefs.edit().putString("saved_pairs_$activeProfileId", pairsSerialized).apply()
    }

    private fun loadConfiguration() {
        val prefs = getSharedPreferences("RelocatorPrefs", MODE_PRIVATE)
        val activeProfileId = prefs.getInt("active_profile_id", 1)

        val pairsCopy = ArrayList(buttonPairs)
        for (p in pairsCopy) removeRemapperPair(p)
        buttonPairs.clear()

        val pairsSerialized = prefs.getString("saved_pairs_$activeProfileId", "") ?: ""
        var maxPairId = 0
        if (pairsSerialized.isNotEmpty()) {
            val parts = pairsSerialized.split("|")
            for (part in parts) {
                val fields = part.split(";")
                if (fields.size == 5) {
                    val id = fields[0].toIntOrNull() ?: continue
                    val trigX = fields[1].toIntOrNull() ?: continue
                    val trigY = fields[2].toIntOrNull() ?: continue
                    val targX = fields[3].toIntOrNull() ?: continue
                    val targY = fields[4].toIntOrNull() ?: continue

                    if (id > maxPairId) maxPairId = id
                    restoreRemapperPair(id, trigX, trigY, targX, targY)
                }
            }
        }
        pairIdCounter = maxPairId + 1

        if (buttonPairs.isEmpty()) {
            addNewRemapperPair()
        }
    }

    private fun restoreRemapperPair(id: Int, trigX: Int, trigY: Int, targX: Int, targY: Int) {
        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_ButtonRelocatorPro)
        val inflater = LayoutInflater.from(themedContext)

        val targetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        targetParams.gravity = Gravity.TOP or Gravity.START
        targetParams.x = targX
        targetParams.y = targY

        val targetView = inflater.inflate(R.layout.layout_target_pointer, null)
        val txtTargetNum = targetView.findViewById<TextView>(R.id.txt_target_number)
        txtTargetNum.text = "R$id"

        val triggerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        triggerParams.gravity = Gravity.TOP or Gravity.START
        triggerParams.x = trigX
        triggerParams.y = trigY

        val triggerView = inflater.inflate(R.layout.layout_trigger_button, null)
        val txtTriggerLabel = triggerView.findViewById<TextView>(R.id.txt_trigger_label)
        txtTriggerLabel.text = "T$id"

        val pair = ButtonPair(
            id = id,
            triggerX = trigX,
            triggerY = trigY,
            targetX = targX,
            targetY = targY,
            triggerView = triggerView,
            targetView = targetView,
            label = "Gatillo $id"
        )

        targetView.setOnTouchListener(createPairTargetTouchListener(pair, targetParams))
        triggerView.setOnTouchListener(createTriggerTouchListener(pair, triggerParams))

        buttonPairs.add(pair)

        windowManager.addView(targetView, targetParams)
        windowManager.addView(triggerView, triggerParams)
    }
}
