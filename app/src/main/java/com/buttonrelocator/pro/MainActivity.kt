package com.buttonrelocator.pro

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private var selectedProfileId = 1
    private var selectedSecurityLevel = AntiDetectEngine.SecurityLevel.STEALTH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)
        selectedProfileId = prefs.getInt("active_profile_id", 1)
        updateProfileUi(selectedProfileId)

        val switchAntiDetect = findViewById<SwitchCompat>(R.id.switchAntiDetect)
        val isAntiDetect = prefs.getBoolean("antidetections_enabled", true)
        switchAntiDetect?.isChecked = isAntiDetect

        val securityLevelStr = prefs.getString("security_level", "STEALTH") ?: "STEALTH"
        selectedSecurityLevel = try {
            AntiDetectEngine.SecurityLevel.valueOf(securityLevelStr)
        } catch (e: Exception) {
            AntiDetectEngine.SecurityLevel.STEALTH
        }
        updateSecurityLevelUi(selectedSecurityLevel)

        // Customization UI Initial States
        val sizeDp = prefs.getInt("trigger_size_dp", 56)
        updateSizeUi(sizeDp)

        val shapeStr = prefs.getString("trigger_shape", "circle") ?: "circle"
        updateShapeUi(shapeStr)

        val opacityPct = (prefs.getFloat("trigger_opacity", 1.0f) * 100).toInt()
        updateOpacityUi(opacityPct)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun setupListeners() {
        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)

        findViewById<SwitchCompat>(R.id.switchAntiDetect)?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("antidetections_enabled", isChecked).apply()
        }

        findViewById<Button>(R.id.btnLevelStealth)?.setOnClickListener { selectSecurityLevel(AntiDetectEngine.SecurityLevel.STEALTH) }
        findViewById<Button>(R.id.btnLevelBalanced)?.setOnClickListener { selectSecurityLevel(AntiDetectEngine.SecurityLevel.BALANCED) }
        findViewById<Button>(R.id.btnLevelDirect)?.setOnClickListener { selectSecurityLevel(AntiDetectEngine.SecurityLevel.DIRECT) }

        findViewById<Button>(R.id.btnGrantAccessibility)?.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnGrantOverlay)?.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnToggleService)?.setOnClickListener {
            val service = RelocatorService.instance
            if (service != null) {
                if (service.isOverlayShowing()) {
                    service.hideOverlay()
                    updateServiceUi(true, isOverlayShowing = false)
                } else {
                    service.showOverlay()
                    updateServiceUi(true, isOverlayShowing = true)
                }
            }
        }

        findViewById<Button>(R.id.btnProfile1)?.setOnClickListener { selectProfile(1) }
        findViewById<Button>(R.id.btnProfile2)?.setOnClickListener { selectProfile(2) }
        findViewById<Button>(R.id.btnProfile3)?.setOnClickListener { selectProfile(3) }

        // Size Customization Listeners
        findViewById<Button>(R.id.btnSizeSmall)?.setOnClickListener { selectSize(40) }
        findViewById<Button>(R.id.btnSizeMedium)?.setOnClickListener { selectSize(56) }
        findViewById<Button>(R.id.btnSizeLarge)?.setOnClickListener { selectSize(72) }

        // Shape Customization Listeners
        findViewById<Button>(R.id.btnShapeCircle)?.setOnClickListener { selectShape("circle") }
        findViewById<Button>(R.id.btnShapeSquare)?.setOnClickListener { selectShape("square") }

        // Opacity Customization Listeners
        findViewById<Button>(R.id.btnOpacityLow)?.setOnClickListener { selectOpacity(0.4f) }
        findViewById<Button>(R.id.btnOpacityMedium)?.setOnClickListener { selectOpacity(0.7f) }
        findViewById<Button>(R.id.btnOpacityHigh)?.setOnClickListener { selectOpacity(1.0f) }
    }

    private fun selectSize(sizeDp: Int) {
        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("trigger_size_dp", sizeDp).apply()
        updateSizeUi(sizeDp)
        notifyServiceUiChange()
    }

    private fun selectShape(shape: String) {
        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("trigger_shape", shape).apply()
        updateShapeUi(shape)
        notifyServiceUiChange()
    }

    private fun selectOpacity(opacity: Float) {
        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("trigger_opacity", opacity).apply()
        updateOpacityUi((opacity * 100).toInt())
        notifyServiceUiChange()
    }

    private fun notifyServiceUiChange() {
        val service = RelocatorService.instance
        if (service != null && service.isOverlayShowing()) {
            service.refreshTriggerDesigns()
        }
    }

    private fun updateSizeUi(sizeDp: Int) {
        val activeColor = ColorStateList.valueOf(getColor(R.color.primary))
        val inactiveColor = ColorStateList.valueOf(getColor(R.color.surface_border))
        val activeText = getColor(R.color.text_primary)
        val inactiveText = getColor(R.color.text_secondary)

        val btnSmall = findViewById<Button>(R.id.btnSizeSmall)
        val btnMedium = findViewById<Button>(R.id.btnSizeMedium)
        val btnLarge = findViewById<Button>(R.id.btnSizeLarge)

        btnSmall?.backgroundTintList = inactiveColor
        btnSmall?.setTextColor(inactiveText)
        btnMedium?.backgroundTintList = inactiveColor
        btnMedium?.setTextColor(inactiveText)
        btnLarge?.backgroundTintList = inactiveColor
        btnLarge?.setTextColor(inactiveText)

        when (sizeDp) {
            40 -> {
                btnSmall?.backgroundTintList = activeColor
                btnSmall?.setTextColor(activeText)
            }
            56 -> {
                btnMedium?.backgroundTintList = activeColor
                btnMedium?.setTextColor(activeText)
            }
            72 -> {
                btnLarge?.backgroundTintList = activeColor
                btnLarge?.setTextColor(activeText)
            }
        }
    }

    private fun updateShapeUi(shape: String) {
        val activeColor = ColorStateList.valueOf(getColor(R.color.primary))
        val inactiveColor = ColorStateList.valueOf(getColor(R.color.surface_border))
        val activeText = getColor(R.color.text_primary)
        val inactiveText = getColor(R.color.text_secondary)

        val btnCircle = findViewById<Button>(R.id.btnShapeCircle)
        val btnSquare = findViewById<Button>(R.id.btnShapeSquare)

        btnCircle?.backgroundTintList = inactiveColor
        btnCircle?.setTextColor(inactiveText)
        btnSquare?.backgroundTintList = inactiveColor
        btnSquare?.setTextColor(inactiveText)

        if (shape == "square") {
            btnSquare?.backgroundTintList = activeColor
            btnSquare?.setTextColor(activeText)
        } else {
            btnCircle?.backgroundTintList = activeColor
            btnCircle?.setTextColor(activeText)
        }
    }

    private fun updateOpacityUi(opacityPct: Int) {
        val activeColor = ColorStateList.valueOf(getColor(R.color.primary))
        val inactiveColor = ColorStateList.valueOf(getColor(R.color.surface_border))
        val activeText = getColor(R.color.text_primary)
        val inactiveText = getColor(R.color.text_secondary)

        val btnLow = findViewById<Button>(R.id.btnOpacityLow)
        val btnMid = findViewById<Button>(R.id.btnOpacityMedium)
        val btnHigh = findViewById<Button>(R.id.btnOpacityHigh)

        btnLow?.backgroundTintList = inactiveColor
        btnLow?.setTextColor(inactiveText)
        btnMid?.backgroundTintList = inactiveColor
        btnMid?.setTextColor(inactiveText)
        btnHigh?.backgroundTintList = inactiveColor
        btnHigh?.setTextColor(inactiveText)

        when (opacityPct) {
            40 -> {
                btnLow?.backgroundTintList = activeColor
                btnLow?.setTextColor(activeText)
            }
            70 -> {
                btnMid?.backgroundTintList = activeColor
                btnMid?.setTextColor(activeText)
            }
            100 -> {
                btnHigh?.backgroundTintList = activeColor
                btnHigh?.setTextColor(activeText)
            }
        }
    }

    private fun selectSecurityLevel(level: AntiDetectEngine.SecurityLevel) {
        selectedSecurityLevel = level
        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("security_level", level.name).apply()
        updateSecurityLevelUi(level)
    }

    private fun updateSecurityLevelUi(level: AntiDetectEngine.SecurityLevel) {
        val activeTextColor = getColor(R.color.text_primary)
        val inactiveTextColor = getColor(R.color.text_secondary)

        val btnStealth = findViewById<Button>(R.id.btnLevelStealth)
        val btnBalanced = findViewById<Button>(R.id.btnLevelBalanced)
        val btnDirect = findViewById<Button>(R.id.btnLevelDirect)

        btnStealth?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        btnStealth?.setTextColor(inactiveTextColor)
        btnBalanced?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        btnBalanced?.setTextColor(inactiveTextColor)
        btnDirect?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        btnDirect?.setTextColor(inactiveTextColor)

        when (level) {
            AntiDetectEngine.SecurityLevel.STEALTH -> {
                btnStealth?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnStealth?.setTextColor(activeTextColor)
            }
            AntiDetectEngine.SecurityLevel.BALANCED -> {
                btnBalanced?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnBalanced?.setTextColor(activeTextColor)
            }
            AntiDetectEngine.SecurityLevel.DIRECT -> {
                btnDirect?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnDirect?.setTextColor(activeTextColor)
            }
        }
    }

    private fun selectProfile(profileId: Int) {
        selectedProfileId = profileId
        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("active_profile_id", profileId).apply()
        
        updateProfileUi(profileId)
        
        val service = RelocatorService.instance
        if (service != null && service.isOverlayShowing()) {
            service.hideOverlay()
            service.showOverlay()
            updateServiceUi(true, isOverlayShowing = true)
        }
    }

    private fun updateProfileUi(activeId: Int) {
        val activeTextColor = getColor(R.color.text_primary)
        val inactiveTextColor = getColor(R.color.text_secondary)

        val btnP1 = findViewById<Button>(R.id.btnProfile1)
        val btnP2 = findViewById<Button>(R.id.btnProfile2)
        val btnP3 = findViewById<Button>(R.id.btnProfile3)

        btnP1?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        btnP1?.setTextColor(inactiveTextColor)
        btnP2?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        btnP2?.setTextColor(inactiveTextColor)
        btnP3?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        btnP3?.setTextColor(inactiveTextColor)

        when (activeId) {
            1 -> {
                btnP1?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnP1?.setTextColor(activeTextColor)
            }
            2 -> {
                btnP2?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnP2?.setTextColor(activeTextColor)
            }
            3 -> {
                btnP3?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnP3?.setTextColor(activeTextColor)
            }
        }
    }

    private fun checkPermissions() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        val isOverlayEnabled = Settings.canDrawOverlays(this)

        val txtAccStatus = findViewById<TextView>(R.id.txtAccessibilityStatus)
        val btnGrantAcc = findViewById<Button>(R.id.btnGrantAccessibility)
        val txtOverlayStatus = findViewById<TextView>(R.id.txtOverlayStatus)
        val btnGrantOverlay = findViewById<Button>(R.id.btnGrantOverlay)
        val btnToggleService = findViewById<Button>(R.id.btnToggleService)

        if (isAccessibilityEnabled) {
            txtAccStatus?.text = getString(R.string.btn_granted)
            txtAccStatus?.setTextColor(getColor(R.color.green_success))
            btnGrantAcc?.isEnabled = false
            btnGrantAcc?.text = getString(R.string.btn_granted)
            btnGrantAcc?.alpha = 0.6f
        } else {
            txtAccStatus?.text = "Inactivo"
            txtAccStatus?.setTextColor(getColor(R.color.red_error))
            btnGrantAcc?.isEnabled = true
            btnGrantAcc?.text = getString(R.string.btn_grant)
            btnGrantAcc?.alpha = 1.0f
        }

        if (isOverlayEnabled) {
            txtOverlayStatus?.text = getString(R.string.btn_granted)
            txtOverlayStatus?.setTextColor(getColor(R.color.green_success))
            btnGrantOverlay?.isEnabled = false
            btnGrantOverlay?.text = getString(R.string.btn_granted)
            btnGrantOverlay?.alpha = 0.6f
        } else {
            txtOverlayStatus?.text = "Inactivo"
            txtOverlayStatus?.setTextColor(getColor(R.color.red_error))
            btnGrantOverlay?.isEnabled = true
            btnGrantOverlay?.text = getString(R.string.btn_grant)
            btnGrantOverlay?.alpha = 1.0f
        }

        val bothPermissionsGranted = isAccessibilityEnabled && isOverlayEnabled
        btnToggleService?.isEnabled = bothPermissionsGranted

        if (bothPermissionsGranted) {
            val service = RelocatorService.instance
            val isOverlayShowing = service?.isOverlayShowing() ?: false
            updateServiceUi(true, isOverlayShowing)
        } else {
            updateServiceUi(false, false)
        }
    }

    private fun updateServiceUi(ready: Boolean, isOverlayShowing: Boolean) {
        val txtStatus = findViewById<TextView>(R.id.txtServiceStatus)
        val btnToggle = findViewById<Button>(R.id.btnToggleService)

        if (!ready) {
            txtStatus?.text = getString(R.string.service_status_pending)
            txtStatus?.setTextColor(getColor(R.color.text_secondary))
            btnToggle?.isEnabled = false
            btnToggle?.text = getString(R.string.btn_start_service)
            btnToggle?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.text_secondary))
        } else {
            txtStatus?.text = getString(R.string.service_status_ready)
            txtStatus?.setTextColor(getColor(R.color.text_primary))
            btnToggle?.isEnabled = true
            
            if (isOverlayShowing) {
                btnToggle?.text = getString(R.string.btn_stop_service)
                btnToggle?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.red_error))
            } else {
                btnToggle?.text = getString(R.string.btn_start_service)
                btnToggle?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent))
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${RelocatorService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(service)
    }
}
