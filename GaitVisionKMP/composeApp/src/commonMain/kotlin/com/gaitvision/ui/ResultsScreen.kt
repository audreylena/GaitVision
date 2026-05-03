package com.gaitvision.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
    ) {
        CommonScreenHeader(title = "Analysis Results", onBack = onNavigateBack)

        when {
            scoreEntity == null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.PrimaryBlue)
                }
            }
            else -> {
                val score = scoreEntity!!
                val overall = score.overallScore.toInt()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
            AiDisclaimerBanner()
            Spacer(modifier = Modifier.height(16.dp))

            ClinicianReviewSection(
                scoreId = scoreId,
                database = database,
                reviewEntity = reviewEntity,
                onReviewUpdated = { reviewEntity = it },
                scope = scope
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AppColors.CardInnerDark,
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAIT SCORE",
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = Color(0xFF999999)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = overall.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = gaitScoreHue(overall)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = gaitScoreCaption(overall),
                        fontSize = 16.sp,
                        color = AppColors.TextWhite
                    )
                    if (score.biologicalSex.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Patient Sex: ${score.biologicalSex}",
                            style = MaterialTheme.typography.caption,
                            color = AppColors.ChartAxisText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = AppColors.CardInnerDark,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Model Scores",
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScoreMetricItem("Autoencoder", "--", AppColors.IconPurple)
                        ScoreMetricItem("Ridge", "--", AppColors.RidgeBlue)
                        ScoreMetricItem("PCA", "--", AppColors.IconGreen)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "100 = Healthy, 0 = Impaired",
                        fontSize = 10.sp,
                        color = AppColors.ChartNoDataText,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI-generated scores are decision-support tools and do not constitute a medical diagnosis.",
                        style = MaterialTheme.typography.caption,
                        color = Color(0xFFFFCC80),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AppColors.CardSurfaceDark,
                elevation = 3.dp
            ) {
                Column {
                    LegacyResultsActionRow(
                        title = "View Gait Features",
                        icon = Icons.Default.Description,
                        iconTint = Color(0xFFAAAAAA),
                        chevronTint = Color(0xFFAAAAAA),
                        showChevron = true,
                        onClick = onNavigateToFeatures
                    )
                    Divider(color = AppColors.DividerLightWhite, thickness = 1.dp)
                    LegacyResultsActionRow(
                        title = "View Signal Dashboard",
                        icon = Icons.Default.BarChart,
                        iconTint = AppColors.SignalsOrange,
                        chevronTint = AppColors.SignalsOrange,
                        showChevron = true,
                        onClick = { onNavigateToSignals(scoreId) }
                    )
                    Divider(color = AppColors.DividerLightWhite, thickness = 1.dp)
                    LegacyResultsActionRow(
                        title = "Export CSV Files",
                        icon = Icons.Default.Description,
                        iconTint = AppColors.RidgeBlue,
                        chevronTint = AppColors.RidgeBlue,
                        showChevron = false,
                        onClick = { onNavigateToCsv(scoreId) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun gaitScoreHue(score: Int): Color = when {
    score >= 75 -> AppColors.IconGreen
    score >= 45 -> AppColors.ScoreWarn
    else -> AppColors.ScorePoor
}

private fun gaitScoreCaption(score: Int): String = when {
    score >= 75 -> "Functional gait pattern"
    score >= 45 -> "Mixed findings — clinical correlation advised"
    else -> "Marked deviation — review recommended"
}

@Composable
private fun AiDisclaimerBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFFFF8F00).copy(alpha = 0.15f),
        elevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFFF8F00))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "These results were generated using artificial intelligence and must be reviewed by a licensed healthcare practitioner before clinical use.",
                style = MaterialTheme.typography.caption,
                color = Color(0xFFFFCC80)
            )
        }
    }
}

@Composable
private fun ClinicianReviewSection(
    scoreId: Long,
    database: AppDatabase,
    reviewEntity: ClinicianReviewEntity?,
    onReviewUpdated: (ClinicianReviewEntity?) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = AppColors.CardSurfaceDark,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Clinician Review",
                style = MaterialTheme.typography.body2,
                color = TextSlate,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val isReviewed = reviewEntity?.isReviewed == true
            Surface(
                color = if (isReviewed) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFE65100).copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isReviewed) "Reviewed" else "Pending Review",
                    color = if (isReviewed) Color(0xFF81C784) else Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
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
                                onReviewUpdated(review)
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
                        "${ldt.month.name.take(3)} ${ldt.dayOfMonth}, ${ldt.year} ${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"
                    } catch (_: Exception) {
                        ""
                    }
                    Text("Reviewed: $fmt", style = MaterialTheme.typography.caption, color = TextSlate)
                }
            }
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
fun LegacyResultsActionRow(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    chevronTint: Color,
    showChevron: Boolean,
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
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = chevronTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
