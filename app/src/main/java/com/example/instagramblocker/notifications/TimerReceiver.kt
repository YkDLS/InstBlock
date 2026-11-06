package com.example.instagramblocker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.instagramblocker.data.BlockStore
import com.example.instagramblocker.data.SystemTimer

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = BlockStore(context)
        val nowMs = SystemTimer.elapsedRealtimeMs()
        val bootCount = SystemTimer.bootCount(context)
        when (intent.action) {
            Intent.ACTION_SHUTDOWN -> store.checkpoint(nowMs, bootCount)
            Intent.ACTION_BOOT_COMPLETED -> {
                val state = store.current(nowMs, bootCount)
                BlockNotificationManager(context).update(state, nowMs)
                if (com.example.instagramblocker.usage.UsageAccess.isGranted(context) && android.provider.Settings.canDrawOverlays(context)) {
                    context.startForegroundService(Intent(context, com.example.instagramblocker.usage.InstagramMonitor::class.java))
                }
            }
        }
    }
}
