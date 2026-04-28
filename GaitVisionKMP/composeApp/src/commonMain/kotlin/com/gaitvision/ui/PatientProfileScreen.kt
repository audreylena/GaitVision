package com.gaitvision.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.AuditLogger
import com.gaitvision.data.ClinicianReviewEntity
import com.gaitvision.data.GaitScoreEntity
import com.gaitvision.data.PatientEntity
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun PatientProfileScreen(
    patientId: Long,
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (Long) -> Unit,
    onNavigateToCamera: () -> Unit = {}
) {
    var patient by remember { mutableStateOf<PatientEntity?>(null) }
    val scoresWithReviews by database.gaitScoreDao().getScoresWithReviewsForPatientFlow(patientId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(patientId) {
        try {
            patient = database.patientDao().getPatientById(patientId)
            AuditLogger.log(database.auditLogDao(), "VIEW_PATIENT", patientId = patientId)
        } catch (e: Exception) {
            println("PatientProfileScreen: load failed: ${e.message}")
        }
    }

    if (patient == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 4.dp,
                actions = {
                    IconButton(onClick = onNavigateToCamera) {
                        Icon(Icons.Default.Add, contentDescription = "New Analysis")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Header Card
            item {
                Card(elevation = 4.dp, shape = RoundedCornerShape(12.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(40.dp),
                            color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${patient?.firstName} ${patient?.lastName}",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold
                        )
                        patient?.participantId?.let { pid ->
                            Text(
                                text = "ID: $pid",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${patient?.age ?: "—"}",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Age", style = MaterialTheme.typography.caption, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = patient?.biologicalSex.takeIf { !it.isNullOrBlank() } ?: "—",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Biological Sex", style = MaterialTheme.typography.caption, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (patient?.height != null && patient?.height != 0) "${patient?.height}\"" else "—",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Height", style = MaterialTheme.typography.caption, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Latest Score Summary
            if (scoresWithReviews.isNotEmpty()) {
                item {
                    val latestScore = scoresWithReviews.last().score
                    val scoreVal = latestScore.overallScore.toInt()
                    Card(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (scoreVal > 80) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Latest Gait Score",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray
                            )
                            Text(
                                text = "$scoreVal",
                                style = MaterialTheme.typography.h3,
                                fontWeight = FontWeight.Bold,
                                color = if (scoreVal > 80) Color(0xFF137333) else Color(0xFFC5221F)
                            )
                            Text(
                                text = "out of 100",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Assessment History Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Assessments (${scoresWithReviews.size})", style = MaterialTheme.typography.h6)
                    TextButton(onClick = onNavigateToCamera) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }

            if (scoresWithReviews.isEmpty()) {
                item {
                    Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No assessments yet", style = MaterialTheme.typography.body1)
                                Text(
                                    "Tap '+' to start a new gait analysis",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            } else {
                items(scoresWithReviews.reversed()) { item ->
                    AssessmentCard(
                        score = item.score,
                        isReviewed = item.review?.isReviewed == true,
                        onClick = { onNavigateToResults(item.score.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssessmentCard(score: GaitScoreEntity, isReviewed: Boolean = false, onClick: () -> Unit) {
    val overall = score.overallScore.toInt()
    val dateStr = try {
        val instant = Instant.fromEpochMilliseconds(score.recordedAt)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.month.name.take(3)} ${local.dayOfMonth}, ${local.year}"
    } catch (e: Exception) {
        "Unknown date"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gait Analysis", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
                Text(dateStr, style = MaterialTheme.typography.caption, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = if (isReviewed) Color(0xFFE6F4EA) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isReviewed) "✓ Reviewed" else "⏳ Pending Review",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.caption,
                        color = if (isReviewed) Color(0xFF137333) else Color(0xFFE65100)
                    )
                }
            }
            Surface(
                color = if (overall > 80) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Score: $overall",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = if (overall > 80) Color(0xFF137333) else Color(0xFFC5221F)
                )
            }
        }
    }
}
