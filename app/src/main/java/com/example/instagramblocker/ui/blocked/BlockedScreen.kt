package com.example.instagramblocker.ui.blocked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun BlockedScreen(remainingMs: Long, message: String, onGoHome: () -> Unit) {
    val totalSeconds = (remainingMs + 999L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val timer = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Opa! O feed ficou para depois.", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text("Cada tentativa acrescenta mais 5 minutos ao bloqueio.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        Text("Tempo restante", style = MaterialTheme.typography.titleMedium)
        Text(timer, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar para a tela inicial")
        }
    }
}
