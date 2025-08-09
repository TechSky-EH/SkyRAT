package com.techsky.skyrat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class KeypadListener : BroadcastReceiver() {

    companion object {
        private const val TAG = "KeypadListener"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Keypad event received")
        val controlPanelIntent = Intent(context, ControlPanel::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(controlPanelIntent)
    }
}