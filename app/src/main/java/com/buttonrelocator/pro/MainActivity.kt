package com.buttonrelocator.pro

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.buttonrelocator.pro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedProfileId = 1
    private var selectedSecurityLevel = AntiDetectEngine.SecurityLevel.STEALTH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("RelocatorPrefs", Context.MODE_PRIVATE)
        selectedProfileId = prefs.getInt("active_profile_id", 1)
        updateProfileUi(selectedProfileId)

        val isAntiDetect = prefs.getBoolean("antidetections_enabled", true)
        binding.switchAntiDetect.isChecked = isAntiDetect

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

        binding.switchAntiDetect.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("antidetections_enabled", isChecked).apply()
        }

        binding.btnLevelStealth.setOnClickListener { selectSecurityLevel(AntiDetectEngine.SecurityLevel.STEALTH) }
        binding.btnLevelBalanced.setOnClickListener { selectSecurityLevel(AntiDetectEngine.SecurityLevel.BALANCED) }
        binding.btnLevelDirect.setOnClickListener { selectSecurityLevel(AntiDetectEngine.SecurityLevel.DIRECT) }

        binding.btnGrantAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.btnGrantOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        binding.btnToggleService.setOnClickListener {
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

        binding.btnProfile1.setOnClickListener { selectProfile(1) }
        binding.btnProfile2.setOnClickListener { selectProfile(2) }
        binding.btnProfile3.setOnClickListener { selectProfile(3) }
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

        binding.btnLevelStealth.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        binding.btnLevelStealth.setTextColor(inactiveTextColor)
        binding.btnLevelBalanced.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        binding.btnLevelBalanced.setTextColor(inactiveTextColor)
        binding.btnLevelDirect.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        binding.btnLevelDirect.setTextColor(inactiveTextColor)

        when (level) {
            AntiDetectEngine.SecurityLevel.STEALTH -> {
                binding.btnLevelStealth.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                binding.btnLevelStealth.setTextColor(activeTextColor)
            }
            AntiDetectEngine.SecurityLevel.BALANCED -> {
                binding.btnLevelBalanced.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                binding.btnLevelBalanced.setTextColor(activeTextColor)
            }
            AntiDetectEngine.SecurityLevel.DIRECT -> {
                binding.btnLevelDirect.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                binding.btnLevelDirect.setTextColor(activeTextColor)
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

        binding.btnProfile1.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        binding.btnProfile1.setTextColor(inactiveTextColor)
        binding.btnProfile2.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        binding.btnProfile2.setTextColor(inactiveTextColor)
        binding.btnProfile3.backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_border))
        binding.btnProfile3.setTextColor(inactiveTextColor)

        when (activeId) {
            1 -> {
                binding.btnProfile1.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                binding.btnProfile1.setTextColor(activeTextColor)
            }
            2 -> {
                binding.btnProfile2.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                binding.btnProfile2.setTextColor(activeTextColor)
            }
            3 -> {
                binding.btnProfile3.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                binding.btnProfile3.setTextColor(activeTextColor)
            }
        }
    }

    private fun checkPermissions() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        val isOverlayEnabled = Settings.canDrawOverlays(this)

        if (isAccessibilityEnabled) {
            binding.txtAccessibilityStatus.text = getString(R.string.btn_granted)
            binding.txtAccessibilityStatus.setTextColor(getColor(R.color.green_success))
            binding.btnGrantAccessibility.isEnabled = false
            binding.btnGrantAccessibility.text = getString(R.string.btn_granted)
            binding.btnGrantAccessibility.alpha = 0.6f
        } else {
            binding.txtAccessibilityStatus.text = "Inactivo"
            binding.txtAccessibilityStatus.setTextColor(getColor(R.color.red_error))
            binding.btnGrantAccessibility.isEnabled = true
            binding.btnGrantAccessibility.text = getString(R.string.btn_grant)
            binding.btnGrantAccessibility.alpha = 1.0f
        }

        if (isOverlayEnabled) {
            binding.txtOverlayStatus.text = getString(R.string.btn_granted)
            binding.txtOverlayStatus.setTextColor(getColor(R.color.green_success))
            binding.btnGrantOverlay.isEnabled = false
            binding.btnGrantOverlay.text = getString(R.string.btn_granted)
            binding.btnGrantOverlay.alpha = 0.6f
        } else {
            binding.txtOverlayStatus.text = "Inactivo"
            binding.txtOverlayStatus.setTextColor(getColor(R.color.red_error))
            binding.btnGrantOverlay.isEnabled = true
            binding.btnGrantOverlay.text = getString(R.string.btn_grant)
            binding.btnGrantOverlay.alpha = 1.0f
        }

        val bothPermissionsGranted = isAccessibilityEnabled && isOverlayEnabled
        binding.btnToggleService.isEnabled = bothPermissionsGranted

        if (bothPermissionsGranted) {
            val service = RelocatorService.instance
            val isOverlayShowing = service?.isOverlayShowing() ?: false
            updateServiceUi(true, isOverlayShowing)
        } else {
            updateServiceUi(false, false)
        }
    }

    private fun updateServiceUi(ready: Boolean, isOverlayShowing: Boolean) {
        if (!ready) {
            binding.txtServiceStatus.text = getString(R.string.service_status_pending)
            binding.txtServiceStatus.setTextColor(getColor(R.color.text_secondary))
            binding.btnToggleService.isEnabled = false
            binding.btnToggleService.text = getString(R.string.btn_start_service)
            binding.btnToggleService.backgroundTintList = ColorStateList.valueOf(getColor(R.color.text_secondary))
        } else {
            binding.txtServiceStatus.text = getString(R.string.service_status_ready)
            binding.txtServiceStatus.setTextColor(getColor(R.color.text_primary))
            binding.btnToggleService.isEnabled = true
            
            if (isOverlayShowing) {
                binding.btnToggleService.text = getString(R.string.btn_stop_service)
                binding.btnToggleService.backgroundTintList = ColorStateList.valueOf(getColor(R.color.red_error))
            } else {
                binding.btnToggleService.text = getString(R.string.btn_start_service)
                binding.btnToggleService.backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent))
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
