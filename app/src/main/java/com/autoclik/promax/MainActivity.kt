package com.autoclik.promax

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.autoclik.promax.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun setupListeners() {
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
