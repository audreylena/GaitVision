package com.gaitvision.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.GaitScoreEntity
import kotlin.math.abs
import kotlin.math.round

private data class ProgressMetric(
    val label: String,
    val unit: String,
    val extractor: (GaitScoreEntity) -> Float?
)

private fun formatOneDecimal(value: Float): String {
    val scaled = round(value * 10f).toLong()
    val whole = scaled / 10
    val frac = abs(scaled % 10)
    return "$whole.$frac"
}

private val PROGRESS_METRICS = listOf(
    ProgressMetric("Overall Score", "/100") { it.overallScore.toFloat() },
    ProgressMetric("L Knee", "score") { it.leftKneeScore?.toFloat() },
    ProgressMetric("R Knee", "score") { it.rightKneeScore?.toFloat() },
    ProgressMetric("L Hip", "score") { it.leftHipScore?.toFloat() },
    ProgressMetric("R Hip", "score") { it.rightHipScore?.toFloat() },
    ProgressMetric("Torso", "score") { it.torsoScore?.toFloat() }
)

@Composable
fun ProgressOverTimeScreen(
    patientId: Long,
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    val scoresByPatient by database.gaitScoreDao().getScoresForPatientFlow(patientId)
        .collectAsState(initial = emptyList())

    val patientFlow by database.patientDao().getAllPatientsFlow().collectAsState(initial = emptyList())
    val patientName = remember(patientFlow, patientId) {
        patientFlow.find { it.id == patientId }?.let { "${it.firstName} ${it.lastName}".trim() }
            ?.takeIf { it.isNotBlank() } ?: "Patient"
    }

    val sortedAsc = remember(scoresByPatient) {
        scoresByPatient.sortedBy { it.recordedAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
    ) {
        CommonScreenHeader(
            title = "Progress",
            subtitle = patientName,
            onBack = onNavigateBack
        )

        if (sortedAsc.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No gait scores yet — run an analysis first.",
                    color = AppColors.ChartAxisText,
                    fontSize = 16.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PROGRESS_METRICS.forEachIndexed { colorIdx, metric ->
                    ProgressMetricChartCard(
                        metric = metric,
                        scores = sortedAsc,
                        colorIndex = colorIdx
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressMetricChartCard(
    metric: ProgressMetric,
    scores: List<GaitScoreEntity>,
    colorIndex: Int
) {
    val palette = listOf(
        AppColors.IconLightBlue,
        AppColors.IconGreen,
        AppColors.SignalsOrange,
        AppColors.AccentPurple,
        AppColors.AccentRose,
        AppColors.SecondaryTealLight
    )
    val lineColor = palette[colorIndex % palette.size]

    val points = scores.mapNotNull { s ->
        metric.extractor(s)?.let { v -> Pair(s.recordedAt.toFloat(), v) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AppColors.TableRowOdd,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${metric.label} (${metric.unit})",
                color = AppColors.TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (points.size < 2) {
                    return@Canvas
                }
                val xs = points.map { it.first }
                val ys = points.map { it.second }
                val minX = xs.minOrNull() ?: 0f
                val maxX = xs.maxOrNull() ?: 1f
                val minY = ys.minOrNull() ?: 0f
                val maxY = ys.maxOrNull() ?: 1f
                val dx = (maxX - minX).takeIf { it > 1f } ?: 1f
                val dy = (maxY - minY).takeIf { it > 1e-3f } ?: 1f

                fun px(x: Float, y: Float): Offset {
                    val nx = (x - minX) / dx
                    val ny = (y - minY) / dy
                    return Offset(nx * size.width, size.height - ny * size.height * 0.85f - size.height * 0.05f)
                }

                val path = Path()
                points.forEachIndexed { i, p ->
                    val o = px(p.first, p.second)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 3f))
                points.forEach { p ->
                    val o = px(p.first, p.second)
                    drawCircle(color = lineColor, radius = 5f, center = o)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = points.takeLast(3).joinToString(" · ") { (_, v) ->
                    formatOneDecimal(v)
                }.ifBlank { "—" },
                color = AppColors.ChartAxisText,
                fontSize = 11.sp
            )
        }
    }
}
