package com.example.instagramblocker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.instagramblocker.R
import com.example.instagramblocker.accessibility.ServiceConnectionState
import com.example.instagramblocker.domain.BlockState
import com.example.instagramblocker.ui.components.StatCard
import com.example.instagramblocker.ui.theme.AppColors
import com.example.instagramblocker.updates.UpdateInfo
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    state: BlockState,
    remainingMs: Long,
    serviceEnabled: Boolean,
    blockingEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onToggleBlocking: () -> Unit,
    updateState: UpdateUiState,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: (UpdateInfo) -> Unit,
) {
    val connected by ServiceConnectionState.connected.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                onRefresh()
                delay(1_000L)
            }
        }
    }

    val totalSeconds = (remainingMs + 999L) / 1_000L
    val timer = String.format(Locale.getDefault(), "%02d:%02d:%02d", totalSeconds / 3_600L, (totalSeconds % 3_600L) / 60L, totalSeconds % 60L)
    val status = when {
        !serviceEnabled -> R.string.service_inactive
        connected -> R.string.service_active
        else -> R.string.service_waiting
    }
    val todayKey = LocalDate.now().toString()
    val dailyEntries = state.attemptsByDay.entries.sortedByDescending { it.key }.take(7)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Text("Foco leve, feed longe", style = MaterialTheme.typography.labelLarge, color = AppColors.primary)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.intro), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(
                modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF6555D9), Color(0xFF9A7BFF))), RoundedCornerShape(28.dp)).padding(24.dp),
            ) {
                Column {
                    Text(if (blockingEnabled) "Bloqueio ativo" else "Pausa para trabalho", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(timer, color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(if (blockingEnabled) "Cada tentativa soma 5 minutos" else "O tempo está congelado", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Tentativas", state.attempts.toString(), AppColors.tertiary, Modifier.weight(1f))
            StatCard("Hoje", (state.attemptsByDay[todayKey] ?: 0).toString(), AppColors.secondary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Tentativas por dia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                if (dailyEntries.isEmpty()) {
                    Text("Ainda não há tentativas registradas.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    dailyEntries.forEach { (day, count) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (day == todayKey) "Hoje" else day, style = MaterialTheme.typography.bodyLarge)
                            Text("$count tentativas", fontWeight = FontWeight.SemiBold, color = AppColors.primary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = AppColors.primary.copy(alpha = 0.08f)), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Atualizações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(updateState.message ?: "Mantenha o bloqueador atualizado pelo GitHub.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                if (updateState.available != null && !updateState.checking) {
                    Button(onClick = { onInstallUpdate(updateState.available) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("Baixar versão ${updateState.available.version}")
                    }
                } else {
                    Button(onClick = onCheckUpdates, enabled = !updateState.checking, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text(if (updateState.checking) "Verificando…" else "Verificar atualizações")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(status), style = MaterialTheme.typography.titleMedium, color = AppColors.secondary)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(if (blockingEnabled) R.string.blocking_enabled else R.string.blocking_paused), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onToggleBlocking,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (blockingEnabled) AppColors.tertiary else AppColors.secondary),
        ) {
            Text(stringResource(if (blockingEnabled) R.string.pause_blocking else R.string.resume_blocking))
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.enable_explanation), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text(stringResource(R.string.open_settings))
        }
    }
}
