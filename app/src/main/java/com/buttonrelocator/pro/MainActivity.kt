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
