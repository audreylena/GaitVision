package com.gaitvision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.GaitScoreEntity

@Composable
fun ResultsScreen(
    scoreId: Long,
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onNavigateToSignals: (Long) -> Unit,
    onNavigateToFeatures: () -> Unit = {},
    onNavigateToCsv: () -> Unit = {}
) {
    var scoreEntity by remember { mutableStateOf<GaitScoreEntity?>(null) }
    LaunchedEffect(scoreId) {
        scoreEntity = database.gaitScoreDao().getScoreById(scoreId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Analysis Results",
                            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                actions = { Spacer(modifier = Modifier.width(48.dp)) }
            )
        },
        backgroundColor = BgUltraDarkBlue
    ) { paddingValues ->
        if (scoreEntity == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
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
            // Main Score Card
            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = CardSurfaceDark,
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "GAIT SCORE",
                        style = MaterialTheme.typography.caption.copy(letterSpacing = 2.sp),
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Simple Score Bar / Segment mockup
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(width = 40.dp, height = 8.dp).background(ButtonDanger, RoundedCornerShape(4.dp)))
                        Box(modifier = Modifier.size(width = 40.dp, height = 8.dp).background(ButtonDanger, RoundedCornerShape(4.dp)))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Feature extraction not run",
                        style = MaterialTheme.typography.body1,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Model Scores Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = CardSurfaceDark,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Model Scores",
                        style = MaterialTheme.typography.body2,
                        color = TextSlate,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ScoreMetricItem("Autoencoder", "--", PrimaryPurple)
                        ScoreMetricItem("Ridge", "--", PrimaryBlue)
                        ScoreMetricItem("PCA", "--", AccentGreen)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "100 = Healthy, 0 = Impaired",
                        style = MaterialTheme.typography.caption,
                        color = TextSlate,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = CardSurfaceDark,
                elevation = 0.dp
            ) {
                Column {
                    ResultActionRow(
                        title = "View Gait Features",
                        icon = Icons.Default.List,
                        onClick = onNavigateToFeatures
                    )
                    Divider(color = BgUltraDarkBlue, thickness = 1.dp)
                    ResultActionRow(
                        title = "View Signal Dashboard",
                        icon = Icons.Default.Search,
                        onClick = { onNavigateToSignals(scoreId) }
                    )
                    Divider(color = BgUltraDarkBlue, thickness = 1.dp)
                    ResultActionRow(
                        title = "Export CSV Files",
                        icon = Icons.Default.Share,
                        onClick = onNavigateToCsv
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ScoreMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.caption, color = color)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun ResultActionRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSlate)
    }
}
