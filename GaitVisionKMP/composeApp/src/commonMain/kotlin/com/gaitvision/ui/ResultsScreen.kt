package com.gaitvision.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.GaitScoreEntity

@Composable
fun ResultsScreen(
    scoreId: Long,
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onNavigateToSignals: (Long) -> Unit
) {
    var scoreEntity by remember { mutableStateOf<GaitScoreEntity?>(null) }
    LaunchedEffect(scoreId) {
        scoreEntity = database.gaitScoreDao().getScoreById(scoreId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gait Analysis Results") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 4.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        if (scoreEntity == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val score = scoreEntity!!
        val overall = score.overallScore.toInt()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Overall Score Circle
            Surface(
                modifier = Modifier.size(140.dp),
                shape = RoundedCornerShape(70.dp),
                color = scoreToBackgroundColor(overall),
                elevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$overall",
                            style = MaterialTheme.typography.h2,
                            color = scoreToTextColor(overall),
                            fontWeight = FontWeight.Bold
                        )
                        Text("/ 100", style = MaterialTheme.typography.body2, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    overall >= 85 -> "Excellent Gait"
                    overall >= 70 -> "Good Gait"
                    overall >= 50 -> "Moderate Impairment"
                    else -> "Significant Impairment"
                },
                style = MaterialTheme.typography.h6,
                color = scoreToTextColor(overall),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Score Breakdown Card
            Card(elevation = 4.dp, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Score Breakdown",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ResultRow(
                        title = "Overall Gait Score",
                        value = "$overall",
                        subtitle = "Autoencoder Model (Primary)",
                        score = overall
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow(
                        title = "Left Knee ROM",
                        value = score.leftKneeScore?.let { "${it.toInt()}°" } ?: "N/A",
                        subtitle = "Range of Motion",
                        score = score.leftKneeScore?.toInt() ?: -1
                    )
                    ResultRow(
                        title = "Right Knee ROM",
                        value = score.rightKneeScore?.let { "${it.toInt()}°" } ?: "N/A",
                        subtitle = "Range of Motion",
                        score = score.rightKneeScore?.toInt() ?: -1
                    )
                    ResultRow(
                        title = "Hip Smoothness",
                        value = score.leftHipScore?.let { "${it.toInt()}" } ?: "N/A",
                        subtitle = "Log Dimensionless Jerk",
                        score = score.leftHipScore?.let { 
                            // LDJ: more negative = smoother; map to 0-100
                            val ldj = it.toFloat()
                            if (ldj.isNaN()) -1 else ((-ldj / 15f) * 100).toInt().coerceIn(0, 100)
                        } ?: -1
                    )
                    ResultRow(
                        title = "Ridge Prediction",
                        value = score.rightHipScore?.let { "${it.toInt()}" } ?: "N/A",
                        subtitle = "Severity Regression Model",
                        score = score.rightHipScore?.toInt() ?: -1
                    )
                    ResultRow(
                        title = "Trunk Lean Stability",
                        value = score.torsoScore?.let { "${(it * 10).toLong().toFloat() / 10f}°" } ?: "N/A",
                        subtitle = "Std. deviation (lower = better)",
                        score = score.torsoScore?.let {
                            val std = it.toFloat()
                            if (std.isNaN()) -1 else ((1f - std / 15f) * 100).toInt().coerceIn(0, 100)
                        } ?: -1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interpretation Card
            Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Clinical Interpretation",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            overall >= 85 -> "Gait pattern is within normal parameters. No significant asymmetry or temporal irregularity detected."
                            overall >= 70 -> "Mild gait deviations noted. Monitor for progression and consider targeted rehabilitation."
                            overall >= 50 -> "Moderate gait impairment. Clinical follow-up recommended. Physical therapy may be beneficial."
                            else -> "Significant gait impairment detected. Immediate clinical evaluation recommended."
                        },
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = { onNavigateToSignals(scoreId) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
            ) {
                Text("View Signals Dashboard", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
fun ResultRow(title: String, value: String, subtitle: String = "", score: Int = -1) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = when {
                score < 0 -> MaterialTheme.colors.onSurface
                score >= 80 -> Color(0xFF137333)
                score >= 50 -> Color(0xFFE37400)
                else -> Color(0xFFC5221F)
            }
        )
    }
}

private fun scoreToBackgroundColor(score: Int): Color = when {
    score >= 85 -> Color(0xFFE6F4EA)
    score >= 70 -> Color(0xFFFEF3CD)
    score >= 50 -> Color(0xFFFCE8E6)
    else -> Color(0xFFFFE0E0)
}

private fun scoreToTextColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF137333)
    score >= 70 -> Color(0xFFE37400)
    score >= 50 -> Color(0xFFC5221F)
    else -> Color(0xFF9B0000)
}
