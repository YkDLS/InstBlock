package com.example.instagramblocker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}
