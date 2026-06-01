package com.autoclik.promax

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.autoclik.promax.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedProfileId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load previously selected profile
        val prefs = getSharedPreferences("AutoclikPrefs", Context.MODE_PRIVATE)
        selectedProfileId = prefs.getInt("active_profile_id", 1)
        updateProfileUi(selectedProfileId)

        val showAddBtn = prefs.getBoolean("show_add_button", true)
        binding.switchShowAddButton.isChecked = showAddBtn

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun setupListeners() {
        val prefs = getSharedPreferences("AutoclikPrefs", Context.MODE_PRIVATE)
        binding.switchShowAddButton.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_add_button", isChecked).apply()
            val service = AutoClickService.instance
            if (service != null && service.isOverlayShowing()) {
                service.updateAddButtonVisibility()
            }
        }

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
            val service = AutoClickService.instance
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

        // Profile Selection Listeners
        binding.btnProfile1.setOnClickListener { selectProfile(1) }
        binding.btnProfile2.setOnClickListener { selectProfile(2) }
        binding.btnProfile3.setOnClickListener { selectProfile(3) }
    }

    private fun selectProfile(profileId: Int) {
        selectedProfileId = profileId
        val prefs = getSharedPreferences("AutoclikPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("active_profile_id", profileId).apply()
        
        updateProfileUi(profileId)
        
        // If service is currently active, we tell it to reload the new configuration
        val service = AutoClickService.instance
        if (service != null && service.isOverlayShowing()) {
            service.hideOverlay()
            service.showOverlay()
            updateServiceUi(true, isOverlayShowing = true)
        }
    }

    private fun updateProfileUi(activeId: Int) {
        val activeColor = getColor(R.color.primary)
        val inactiveColor = getColor(R.color.surface_border)
        
        val activeTextColor = getColor(R.color.text_primary)
        val inactiveTextColor = getColor(R.color.text_secondary)

        // Reset all buttons
        binding.btnProfile1.backgroundTintList = getColorStateList(R.color.surface_border)
        binding.btnProfile1.setTextColor(inactiveTextColor)
        
        binding.btnProfile2.backgroundTintList = getColorStateList(R.color.surface_border)
        binding.btnProfile2.setTextColor(inactiveTextColor)
        
        binding.btnProfile3.backgroundTintList = getColorStateList(R.color.surface_border)
        binding.btnProfile3.setTextColor(inactiveTextColor)

        // Highlight selected
        when (activeId) {
            1 -> {
                binding.btnProfile1.backgroundTintList = getColorStateList(R.color.primary)
                binding.btnProfile1.setTextColor(activeTextColor)
            }
            2 -> {
                binding.btnProfile2.backgroundTintList = getColorStateList(R.color.primary)
                binding.btnProfile2.setTextColor(activeTextColor)
            }
            3 -> {
                binding.btnProfile3.backgroundTintList = getColorStateList(R.color.primary)
                binding.btnProfile3.setTextColor(activeTextColor)
            }
        }
    }

    private fun checkPermissions() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        val isOverlayEnabled = Settings.canDrawOverlays(this)

        // Update Accessibility Permission UI
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

        // Update Overlay Permission UI
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

        // Enable / Disable Start Button based on permissions
        val bothPermissionsGranted = isAccessibilityEnabled && isOverlayEnabled
        binding.btnToggleService.isEnabled = bothPermissionsGranted

        if (bothPermissionsGranted) {
            val service = AutoClickService.instance
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
            binding.btnToggleService.backgroundTintList = getColorStateList(R.color.text_secondary)
        } else {
            binding.txtServiceStatus.text = getString(R.string.service_status_ready)
            binding.txtServiceStatus.setTextColor(getColor(R.color.text_primary))
            binding.btnToggleService.isEnabled = true
            
            if (isOverlayShowing) {
                binding.btnToggleService.text = getString(R.string.btn_stop_service)
                binding.btnToggleService.backgroundTintList = getColorStateList(R.color.red_error)
            } else {
                binding.btnToggleService.text = getString(R.string.btn_start_service)
                binding.btnToggleService.backgroundTintList = getColorStateList(R.color.accent)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${AutoClickService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(service)
    }
}
