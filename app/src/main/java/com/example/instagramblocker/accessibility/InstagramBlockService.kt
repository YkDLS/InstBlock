package com.example.instagramblocker.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.instagramblocker.BlockedActivity
import com.example.instagramblocker.BlockStore
import com.example.instagramblocker.BlockControlStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Observa apenas mudanças de janela; não solicita acesso ao conteúdo da tela. */
class InstagramBlockService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var store: BlockStore
    private lateinit var controlStore: BlockControlStore
    private var instagramForeground = false
    private var pendingAttempt: Job? = null

    override fun onCreate() {
        super.onCreate()
        store = BlockStore(this)
        controlStore = BlockControlStore(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ServiceConnectionState.connected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val isInstagram = InstagramTarget.matches(event.packageName)
        if (!isInstagram) {
            instagramForeground = false
            return
        }
        if (!controlStore.isEnabled()) return
        if (instagramForeground || pendingAttempt != null) return
        instagramForeground = true

        pendingAttempt = serviceScope.launch {
            var saved = false
            try {
                // Once started, do not cancel the critical sequence between
                // persistence and the corresponding dismissal attempt.
                withContext(NonCancellable) {
                    saved = withContext(Dispatchers.IO) {
                        store.recordAttempt(System.currentTimeMillis())
                    }
                    if (saved) dismissInstagram()
                }
            } catch (error: Exception) {
                if (!saved) {
                    instagramForeground = false
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
        instagramForeground = false
    }

    override fun onDestroy() {
        ServiceConnectionState.connected.value = false
        pendingAttempt?.cancel()
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

    private companion object {
        const val TAG = "InstagramBlockService"
    }
}
