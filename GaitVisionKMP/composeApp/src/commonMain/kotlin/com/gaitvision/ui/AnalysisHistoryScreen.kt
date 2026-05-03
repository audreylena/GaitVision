package com.gaitvision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.GaitScoreWithReview
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun AnalysisHistoryScreen(
    patientId: Long,
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (Long) -> Unit
) {
    val scoresWithReviews by database.gaitScoreDao().getScoresWithReviewsForPatientFlow(patientId)
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
    ) {
        CommonScreenHeader(title = "Analysis History", onBack = onNavigateBack)

        if (scoresWithReviews.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No analyses recorded yet.", color = AppColors.ChartAxisText, fontSize = 16.sp)
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(scoresWithReviews.reversed(), key = { it.score.id }) { item ->
                HistoryScoreRow(item = item, onClick = { onNavigateToResults(item.score.id) })
            }
        }
    }
}

@Composable
private fun HistoryScoreRow(item: GaitScoreWithReview, onClick: () -> Unit) {
    val score = item.score
    val reviewed = item.review?.isReviewed == true
    val dateStr = formatHistoryDate(score.recordedAt)
    val overall = score.overallScore.toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AppColors.CardSurfaceDark,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateStr, color = AppColors.TableHeaderText, fontSize = 12.sp)
                Text(
                    "Overall score $overall",
                    color = AppColors.TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    if (reviewed) "Reviewed" else "Pending review",
                    color = if (reviewed) AppColors.IconGreen else AppColors.ScoreWarn,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Text(">", color = AppColors.TextTertiary, fontSize = 18.sp)
        }
    }
}

private fun formatHistoryDate(epochMs: Long): String = try {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    "${ldt.month.name.take(3)} ${ldt.dayOfMonth}, ${ldt.year}  ${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"
} catch (_: Exception) {
    "—"
}
