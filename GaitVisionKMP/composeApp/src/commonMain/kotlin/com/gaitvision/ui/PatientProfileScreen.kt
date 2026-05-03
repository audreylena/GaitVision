package com.gaitvision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.AuditLogger
import com.gaitvision.data.GaitScoreEntity
import com.gaitvision.data.PatientEntity
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
    var showHistoryDialog by remember { mutableStateOf(false) }

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
            CircularProgressIndicator(color = AppColors.PrimaryBlue)
        }
        return
    }

    val p = patient!!
    val subtitle = "${p.firstName} ${p.lastName}".trim()
    val analysesCount = scoresWithReviews.size
    val latestOverall = scoresWithReviews.lastOrNull()?.score?.overallScore?.toInt()
    val latestScoreColor = latestOverall?.let(::scoreTintForValue) ?: AppColors.ChartAxisText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        CommonScreenHeader(
            title = "Patient Profile",
            subtitle = subtitle,
            onBack = onNavigateBack
        )

        HeroPatientCard(
            patient = p,
            analysesCount = analysesCount,
            latestScoreText = latestOverall?.toString() ?: "—",
            latestScoreColor = latestScoreColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        ProgressChartPlaceholderCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

        ProfileActionsCard(
            onNewAnalysis = onNavigateToCamera,
            onBatchAnalysis = { },
            onAnalysisHistory = { showHistoryDialog = true },
            onProgressOverTime = { },
            onEditPatient = { },
            onDeletePatient = { },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Analysis History", color = AppColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (scoresWithReviews.isEmpty()) {
                        Text("No analyses recorded yet.", color = AppColors.TextSecondary)
                    } else {
                        scoresWithReviews.reversed().forEach { item ->
                            val dateStr = formatScoreDate(item.score)
                            TextButton(onClick = {
                                showHistoryDialog = false
                                onNavigateToResults(item.score.id)
                            }) {
                                Text("$dateStr — Score ${item.score.overallScore.toInt()}", color = AppColors.PrimaryBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = AppColors.PrimaryBlue)
                }
            },
            backgroundColor = AppColors.BackgroundWhite
        )
    }
}

private fun scoreTintForValue(score: Int): Color = when {
    score >= 75 -> AppColors.ScoreGood
    score >= 45 -> AppColors.ScoreWarn
    else -> AppColors.ScorePoor
}

private fun formatScoreDate(score: GaitScoreEntity): String = try {
    val instant = Instant.fromEpochMilliseconds(score.recordedAt)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    "${local.month.name.take(3)} ${local.dayOfMonth}, ${local.year}"
} catch (_: Exception) {
    "—"
}

private fun formatPatientAdded(patient: PatientEntity): String = try {
    val instant = Instant.fromEpochMilliseconds(patient.createdAt)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    "${local.month.name.take(3)} ${local.dayOfMonth}, ${local.year}"
} catch (_: Exception) {
    "—"
}

@Composable
private fun HeroPatientCard(
    patient: PatientEntity,
    analysesCount: Int,
    latestScoreText: String,
    latestScoreColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = AppColors.CardSurfaceDark,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "${patient.firstName} ${patient.lastName}".trim(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x33252542)
            ) {
                Text(
                    text = patient.participantId ?: patient.id.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TableHeaderText
                )
            }
            Divider(
                color = AppColors.DividerMediumWhite,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 14.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                DemoStatCell("AGE", "${patient.age ?: "—"} yr", Modifier.weight(1f))
                DemoStatCell("GENDER", patient.biologicalSex.takeIf { it.isNotBlank() } ?: "—", Modifier.weight(1f))
                DemoStatCell(
                    "HEIGHT",
                    if (patient.height != 0) "${patient.height}\"" else "—",
                    Modifier.weight(1f)
                )
                DemoStatCell("ADDED", formatPatientAdded(patient), Modifier.weight(1.2f), valueSize = 12.sp)
            }
            Divider(
                color = AppColors.DividerMediumWhite,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 14.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                DemoStatCell(
                    label = "ANALYSES",
                    value = analysesCount.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = AppColors.TableHeaderText,
                    valueSize = 18.sp
                )
                DemoStatCell(
                    label = "LATEST SCORE",
                    value = latestScoreText,
                    modifier = Modifier.weight(1f),
                    valueColor = latestScoreColor,
                    valueSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun DemoStatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppColors.TextWhite,
    valueSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            letterSpacing = 0.1.sp,
            color = AppColors.ChartAxisText,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = valueSize,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun ProgressChartPlaceholderCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AppColors.CardSurfaceDark,
        elevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Progress chart",
                color = AppColors.ChartAxisText,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ProfileActionsCard(
    onNewAnalysis: () -> Unit,
    onBatchAnalysis: () -> Unit,
    onAnalysisHistory: () -> Unit,
    onProgressOverTime: () -> Unit,
    onEditPatient: () -> Unit,
    onDeletePatient: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = AppColors.CardSurfaceDark,
        elevation = 3.dp
    ) {
        Column {
            ProfileActionRow(
                icon = Icons.Default.Movie,
                iconTint = AppColors.IconGreen,
                title = "New Gait Analysis",
                chevronTint = AppColors.IconGreen,
                onClick = onNewAnalysis
            )
            Divider(color = AppColors.DividerLightWhite, thickness = 1.dp)
            ProfileActionRow(
                icon = Icons.Default.Folder,
                iconTint = AppColors.IconPurple,
                title = "Batch Analysis",
                chevronTint = AppColors.IconPurple,
                onClick = onBatchAnalysis
            )
            Divider(color = AppColors.DividerLightWhite, thickness = 1.dp)
            ProfileActionRow(
                icon = Icons.Default.History,
                iconTint = AppColors.IconLightBlue,
                title = "Analysis History",
                chevronTint = AppColors.IconLightBlue,
                onClick = onAnalysisHistory
            )
            Divider(color = AppColors.DividerLightWhite, thickness = 1.dp)
            ProfileActionRow(
                icon = Icons.Default.ShowChart,
                iconTint = AppColors.IconLightBlue,
                title = "Progress Over Time",
                chevronTint = AppColors.IconLightBlue,
                onClick = onProgressOverTime
            )
            Divider(color = AppColors.DividerLightWhite, thickness = 1.dp)
            ProfileActionRow(
                icon = Icons.Default.Edit,
                iconTint = AppColors.ChartAxisText,
                title = "Edit Patient Record",
                titleColor = Color(0xFFCCCCCC),
                chevronTint = AppColors.ChartAxisText,
                onClick = onEditPatient
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = AppColors.DividerMediumWhite, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileActionRow(
                icon = Icons.Default.Delete,
                iconTint = AppColors.ScorePoor,
                title = "Delete Patient Record",
                titleColor = AppColors.ScorePoor,
                chevronTint = AppColors.ScorePoor,
                onClick = onDeletePatient
            )
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleColor: Color = AppColors.TextWhite,
    chevronTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.size(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = titleColor)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = chevronTint,
            modifier = Modifier.size(18.dp)
        )
    }
}
