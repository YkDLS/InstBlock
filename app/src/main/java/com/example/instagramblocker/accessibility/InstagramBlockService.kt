package com.example.instagramblocker.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.instagramblocker.BlockedActivity
import com.example.instagramblocker.data.BlockStore
import com.example.instagramblocker.data.SystemTimer
import com.example.instagramblocker.domain.BlockPhase
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.notifications.BlockNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Observa apenas mudanças de janela; não solicita acesso ao conteúdo da tela. */
class InstagramBlockService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var store: BlockStore
    private lateinit var notifications: BlockNotificationManager
    private var pendingAttempt: Job? = null
    private var timerJob: Job? = null
    private var lastBlockedAtMs = 0L
    private var bootCount = 0

    override fun onCreate() {
        super.onCreate()
        store = BlockStore(this)
        notifications = BlockNotificationManager(this)
        bootCount = SystemTimer.bootCount(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            packageNames = arrayOf(InstagramTarget.PACKAGE_NAME)
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        ServiceConnectionState.connected.value = true
        startTimer()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!InstagramTarget.matches(event.packageName)) return

        val nowMs = SystemTimer.elapsedRealtimeMs()
        val state = store.current(nowMs, bootCount)
        if (state.phase == BlockPhase.RELEASED) {
            return
        }
        if (pendingAttempt != null || nowMs - lastBlockedAtMs < EVENT_DEBOUNCE_MS) return
        lastBlockedAtMs = nowMs

        pendingAttempt = serviceScope.launch {
            var saved = false
            try {
                // Once started, do not cancel the critical sequence between
                // persistence and the corresponding dismissal attempt.
                withContext(NonCancellable) {
                    saved = withContext(Dispatchers.IO) {
                        store.recordAttempt(nowMs, bootCount)
                    }
                    if (saved) dismissInstagram()
                }
            } catch (error: Exception) {
                if (!saved) {
                    lastBlockedAtMs = 0L
                    Log.w(TAG, "Could not persist Instagram attempt", error)
                } else {
                    // The attempt is already persisted; preserve deduplication
                    // if Android rejects both dismissal mechanisms.
                    Log.w(TAG, "Could not dismiss Instagram", error)
                }
            } finally {
                pendingAttempt = null
            }
        }
    }

    override fun onInterrupt() {
        ServiceConnectionState.connected.value = false
        // Keep the Job reference until its critical section finishes.
        pendingAttempt?.cancel()
        timerJob?.cancel()
        checkpoint()
    }

    override fun onDestroy() {
        ServiceConnectionState.connected.value = false
        pendingAttempt?.cancel()
        timerJob?.cancel()
        checkpoint()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun dismissInstagram() {
        try {
            startActivity(
                Intent(this, BlockedActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            return
        } catch (error: Exception) {
            Log.w(TAG, "Could not open block screen", error)
        }

        try {
            if (performGlobalAction(GLOBAL_ACTION_HOME)) return
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(home)
        } catch (error: Exception) {
            // Persistence already happened; log the fallback failure without
            // crashing the accessibility service.
            Log.w(TAG, "Fallback HOME intent failed", error)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var previousPhase: BlockPhase? = null
            var ticks = 0
            while (true) {
                val nowMs = SystemTimer.elapsedRealtimeMs()
                val state = withContext(Dispatchers.IO) {
                    if (ticks % CHECKPOINT_INTERVAL_SECONDS == 0) {
                        store.checkpoint(nowMs, bootCount)
                    } else {
                        store.current(nowMs, bootCount)
                    }
                }
                notifications.notifyTransition(previousPhase, state.phase)
                notifications.update(state, nowMs)
                previousPhase = state.phase
                ticks++
                delay(TIMER_TICK_MS)
            }
        }
    }

    private fun checkpoint() {
        if (!::store.isInitialized) return
        store.checkpoint(SystemTimer.elapsedRealtimeMs(), bootCount)
    }

    private companion object {
        const val TAG = "InstagramBlockService"
        const val EVENT_DEBOUNCE_MS = 1_000L
        const val TIMER_TICK_MS = 1_000L
        const val CHECKPOINT_INTERVAL_SECONDS = 5
    }
}
