package com.gaitvision.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A technical, user-friendly debug overlay for monitoring KMP app state.
 * This overlay provides real-time telemetry metrics that can be enabled by developers
 * or clinicians to monitor system load during video processing.
 */
@Composable
fun DiagnosticOverlay(
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = remember { DiagnosticsViewModel() }
) {
    val telemetryState by viewModel.uiState.collectAsState()

    // Formats numbers safely without relying on Java's String.format in KMP
    val memText = "${(telemetryState.memoryUsageMb * 10).toInt() / 10f} MB"
    val cpuText = "${(telemetryState.estimatedCpuLoad * 10).toInt() / 10f} %"
    val queueText = telemetryState.analysisQueueSize.toString()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
                .width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "SYS TELEMETRY",
                color = Color.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(0.9f)
            )
            
            Divider()

            TelemetryRow(label = "MEM:", value = memText)
            TelemetryRow(label = "CPU:", value = cpuText)
            TelemetryRow(label = "QUEUE:", value = queueText)
            
            val camColor = if (telemetryState.isCameraActive) Color.Green else Color.Red
            TelemetryRow(
                label = "CAM:", 
                value = if (telemetryState.isCameraActive) "ACTIVE" else "IDLE",
                valueColor = camColor
            )
        }
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 11.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 4.dp)
            .background(Color.DarkGray)
    )
}
