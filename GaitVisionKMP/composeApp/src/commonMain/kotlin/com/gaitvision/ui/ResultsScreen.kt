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
import com.gaitvision.data.AuditLogger
import com.gaitvision.data.ClinicianReviewEntity
import com.gaitvision.data.GaitScoreEntity
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

@Composable
fun ResultsScreen(
    scoreId: Long,
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onNavigateToSignals: (Long) -> Unit,
    onNavigateToFeatures: () -> Unit = {},
    onNavigateToCsv: (Long) -> Unit
) {
    var scoreEntity by remember { mutableStateOf<GaitScoreEntity?>(null) }
    var reviewEntity by remember { mutableStateOf<ClinicianReviewEntity?>(null) }
    val scope = rememberSafeCoroutineScope()
    LaunchedEffect(scoreId) {
        try {
            scoreEntity = database.gaitScoreDao().getScoreById(scoreId)
            reviewEntity = database.clinicianReviewDao().getReviewForScore(scoreId)
            AuditLogger.log(database.auditLogDao(), "VIEW_RESULTS", recordId = scoreId)
        } catch (e: Exception) {
            println("ResultsScreen: load failed: ${e.message}")
        }
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
            // ── Disclaimer Banner #1 (SB 1188 § 183.005) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color(0xFFFF8F00).copy(alpha = 0.15f),
                elevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8F00))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "These results were generated using artificial intelligence and must be reviewed by a licensed healthcare practitioner before clinical use.",
                        style = MaterialTheme.typography.caption,
                        color = Color(0xFFFFCC80)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Clinician Review Card (SB 1188 § 183.005(a)(3)) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = CardSurfaceDark,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Clinician Review",
                        style = MaterialTheme.typography.body2,
                        color = TextSlate,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val isReviewed = reviewEntity?.isReviewed == true
                    // Status badge
                    Surface(
                        color = if (isReviewed) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFE65100).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isReviewed) "✓  Reviewed" else "⏳  Pending Review",
                            color = if (isReviewed) Color(0xFF81C784) else Color(0xFFFFB74D),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    if (!isReviewed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val review = ClinicianReviewEntity(
                                            gaitScoreId = scoreId,
                                            isReviewed = true,
                                            reviewTimestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                        )
                                        database.clinicianReviewDao().insertReview(review)
                                        reviewEntity = review
                                    } catch (e: Exception) {
                                        println("ResultsScreen: review insert failed: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue)
                        ) {
                            Text("Mark as Reviewed", color = Color.White)
                        }
                    } else {
                        reviewEntity?.reviewTimestamp?.let { ts ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val fmt = try {
                                val inst = kotlinx.datetime.Instant.fromEpochMilliseconds(ts)
                                val ldt = inst.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                                "${ldt.month.name.take(3)} ${ldt.dayOfMonth}, ${ldt.year} ${ldt.hour.toString().padStart(2,'0')}:${ldt.minute.toString().padStart(2,'0')}"
                            } catch (e: Exception) { "" }
                            Text("Reviewed: $fmt", style = MaterialTheme.typography.caption, color = TextSlate)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                    // Patient biological sex metadata — SB 1188 § 183.007(a)(2)
                    if (score.biologicalSex.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Patient Sex: ${score.biologicalSex}",
                            style = MaterialTheme.typography.caption,
                            color = TextSlate
                        )
                    }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    // ── Disclaimer Banner #2 ──
                    Text(
                        "AI-generated scores are decision-support tools and do not constitute a medical diagnosis.",
                        style = MaterialTheme.typography.caption,
                        color = Color(0xFFFFCC80),
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
                        onClick = { onNavigateToCsv(scoreId) }
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
