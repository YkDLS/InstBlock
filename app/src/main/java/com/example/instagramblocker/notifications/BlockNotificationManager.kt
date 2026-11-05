package com.example.instagramblocker.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.instagramblocker.MainActivity
import com.example.instagramblocker.R
import com.example.instagramblocker.domain.BlockPhase
import com.example.instagramblocker.domain.BlockPolicy
import com.example.instagramblocker.domain.BlockState
import java.util.Locale

class BlockNotificationManager(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Tempo de foco",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Avisa o tempo restante e as mudanças do ciclo de bloqueio."
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    fun update(state: BlockState, nowElapsedMs: Long) {
        val snapshot = BlockPolicy.snapshot(nowElapsedMs, state.phase, state.deadlineMs)
        when (snapshot.phase) {
            BlockPhase.BLOCKED -> showStatus(
                title = "Instagram bloqueado",
                text = "Tempo restante: ${formatDuration(snapshot.remainingMs)}",
                ongoing = true,
            )
            BlockPhase.RELEASED -> showStatus(
                title = "Instagram liberado",
                text = "Acesso disponível por ${formatDuration(snapshot.remainingMs)}",
                ongoing = true,
            )
            BlockPhase.READY -> manager.cancel(STATUS_NOTIFICATION_ID)
        }
    }

    fun notifyTransition(previous: BlockPhase?, current: BlockPhase) {
        if (previous == null || previous == current) return
        val (title, text) = when (current) {
            BlockPhase.RELEASED -> "Instagram liberado" to "Você tem 10 minutos corridos de acesso."
            BlockPhase.READY -> "A proteção voltou sozinha" to "A próxima tentativa será bloqueada novamente."
            BlockPhase.BLOCKED -> "Bloqueio iniciado" to "A espera usa o tempo interno do sistema."
        }
        manager.notify(
            EVENT_NOTIFICATION_ID,
            builder(title, text).setAutoCancel(true).build(),
        )
    }

    private fun showStatus(title: String, text: String, ongoing: Boolean) {
        manager.notify(
            STATUS_NOTIFICATION_ID,
            builder(title, text)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true)
                .build(),
        )
    }

    private fun builder(title: String, text: String): NotificationCompat.Builder {
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(intent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs + 999L) / 1_000L
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            totalSeconds / 3_600L,
            (totalSeconds % 3_600L) / 60L,
            totalSeconds % 60L,
        )
    }

    private companion object {
        const val CHANNEL_ID = "block_timer"
        const val STATUS_NOTIFICATION_ID = 1001
        const val EVENT_NOTIFICATION_ID = 1002
    }
}
