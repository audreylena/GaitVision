package com.gaitvision.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gaitvision.data.GaitScoreEntity

@Composable
fun ResultsScreen(
    scoreId: Long,
    database: com.gaitvision.data.AppDatabase,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val overall = scoreEntity?.overallScore?.toInt() ?: 0
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(60.dp),
                color = if (overall > 80) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                elevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$overall",
                            style = MaterialTheme.typography.h3,
                            color = if (overall > 80) Color(0xFF137333) else Color(0xFFC5221F),
                            fontWeight = FontWeight.Bold
                        )
                        Text("Overall Score", style = MaterialTheme.typography.caption)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Score Breakdown", style = MaterialTheme.typography.h6, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            
            ResultRow(title = "Left Knee", score = "${scoreEntity?.leftKneeScore?.toInt() ?: 0}/100")
            ResultRow(title = "Right Knee", score = "${scoreEntity?.rightKneeScore?.toInt() ?: 0}/100")
            ResultRow(title = "Left Hip", score = "${scoreEntity?.leftHipScore?.toInt() ?: 0}/100")
            ResultRow(title = "Right Hip", score = "${scoreEntity?.rightHipScore?.toInt() ?: 0}/100")
            ResultRow(title = "Torso Alignment", score = "${scoreEntity?.torsoScore?.toInt() ?: 0}/100")
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { onNavigateToSignals(scoreId) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
            ) {
                Text("View Signals Dashboard", color = Color.White)
            }
        }
    }
}

@Composable
fun ResultRow(title: String, score: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.body1)
        }
        Text(score, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
    }
    Divider(color = Color.LightGray, thickness = 0.5.dp)
}
