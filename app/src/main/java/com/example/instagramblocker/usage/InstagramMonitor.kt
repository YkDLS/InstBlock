package com.example.instagramblocker.usage

import android.app.KeyguardManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.os.PowerManager
import android.view.WindowManager
import android.graphics.PixelFormat
import com.example.instagramblocker.accessibility.InstagramTarget
import com.example.instagramblocker.data.BlockStore
import com.example.instagramblocker.data.SystemTimer
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockPhase
import com.example.instagramblocker.notifications.BlockNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class InstagramMonitor : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var store: BlockStore
    private lateinit var notifications: BlockNotificationManager
    private val foregroundTracker = ForegroundEntryTracker(InstagramTarget.PACKAGE_NAME)
    private var lastBlockedAt = 0L
    private var bootCount = 0
    private var previousPhase: BlockPhase? = null
    private var lastMessageIndex = -1
    private var blockOverlay: BlockOverlayView? = null
    private var overlayManager: WindowManager? = null

    override fun onCreate() {
        super.onCreate()
        store = BlockStore(this)
        notifications = BlockNotificationManager(this)
        bootCount = SystemTimer.bootCount(this)
        startForeground(BlockNotificationManager.SERVICE_NOTIFICATION_ID, notifications.monitoringNotification())
        scope.launch { monitorLoop() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private suspend fun monitorLoop() {
        val usage = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val power = getSystemService(POWER_SERVICE) as PowerManager
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        overlayManager = getSystemService(WINDOW_SERVICE) as WindowManager
        while (true) {
            if (UsageAccess.isGranted(this) && android.provider.Settings.canDrawOverlays(this)) {
                val now = System.currentTimeMillis()
                foregroundTracker.resetIfClockMovedBack(now)
                val events = usage.queryEvents((now - EVENT_LOOKBACK_MS).coerceAtLeast(0), now)
                val event = UsageEvents.Event()
                var newestTime = foregroundTracker.lastEventTime
                var newestPackage: String? = null
                var enteredInstagram = false
                while (events != null && events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType in FOREGROUND_EVENTS && event.timeStamp > newestTime) {
                        newestTime = event.timeStamp
                        newestPackage = event.packageName
                    }
                }
                if (newestPackage != null) {
                    enteredInstagram = foregroundTracker.observe(newestTime, newestPackage)
                }
                val elapsed = SystemClock.elapsedRealtime()
                val screenUsable = power.isInteractive && !keyguard.isKeyguardLocked
                var state = withContext(Dispatchers.IO) { store.checkpoint(elapsed, bootCount) }
                val shouldRecord = MonitorDecision.shouldRecordAttempt(
                    phase = state.phase,
                    instagramForeground = foregroundTracker.isTargetForeground,
                    enteredInstagram = enteredInstagram,
                    screenUsable = screenUsable,
                    debounceElapsed = elapsed - lastBlockedAt >= DEBOUNCE_MS,
                )
                if (shouldRecord) {
                    val saved = withContext(Dispatchers.IO) { store.recordAttempt(elapsed, bootCount) }
                    if (saved) {
                        lastBlockedAt = elapsed
                        state = withContext(Dispatchers.IO) { store.checkpoint(elapsed, bootCount) }
                    }
                }
                notifications.notifyTransition(previousPhase, state.phase)
                notifications.update(state, elapsed)
                previousPhase = state.phase
                if (state.phase == BlockPhase.BLOCKED && foregroundTracker.isTargetForeground && screenUsable) {
                    showBlockOverlay()
                    blockOverlay?.update(BlockPolicy.remainingMs(elapsed, state.deadlineMs), state.attempts)
                } else {
                    removeBlockOverlay()
                }
            } else {
                removeBlockOverlay()
                stopSelf()
                return
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        removeBlockOverlay()
        scope.cancel()
        super.onDestroy()
    }

    private fun showBlockOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this) || blockOverlay != null) return
        val view = BlockOverlayView(this, nextMotivation()) {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(home)
                foregroundTracker.clearForeground()
                removeBlockOverlay()
            } catch (_: Exception) { }
        }
        try {
            overlayManager?.addView(view, WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ))
            blockOverlay = view
        } catch (_: Exception) { }
    }

    private fun removeBlockOverlay() {
        blockOverlay?.let { try { overlayManager?.removeView(it) } catch (_: Exception) { } }
        blockOverlay = null
    }

    private fun nextMotivation(): String {
        val messages = resources.getStringArray(com.example.instagramblocker.R.array.block_motivations)
        val index = if (lastMessageIndex < 0 || messages.size < 2) {
            Random.nextInt(messages.size)
        } else {
            val next = Random.nextInt(messages.size - 1)
            if (next >= lastMessageIndex) next + 1 else next
        }
        lastMessageIndex = index
        return messages[index]
    }

    companion object {
        const val ACTION_START = "com.example.instagramblocker.START_MONITOR"
        private const val POLL_INTERVAL_MS = 1_000L
        private const val EVENT_LOOKBACK_MS = 15_000L
        private const val DEBOUNCE_MS = 1_000L
        private val FOREGROUND_EVENTS = setOf(UsageEvents.Event.MOVE_TO_FOREGROUND)
    }
}
