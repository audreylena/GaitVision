package com.gaitvision.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun AnalysisScreen(
    onNavigateBack: () -> Unit,
    database: AppDatabase,
    onNavigateToPatientList: () -> Unit,
    onNavigateToLatestResults: (Long) -> Unit
) {
    val scope = rememberSafeCoroutineScope()
    var viewResultsHint by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
            .verticalScroll(rememberScrollState())
    ) {
        CommonScreenHeader(title = "Gait Analysis", onBack = onNavigateBack)

        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = AppColors.CardInnerDark,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Participant: —\nHeight: —",
                        fontSize = 16.sp,
                        color = AppColors.TextWhite,
                        lineHeight = 22.sp
                    )
                    Divider(
                        color = Color(0xFF333333),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Text(
                        text = "Video ready for analysis",
                        fontSize = 14.sp,
                        color = AppColors.IconGreen
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToPatientList)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        tint = AppColors.IconPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Run Analysis",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Extracting Frames...",
                fontSize = 16.sp,
                color = AppColors.TextWhite,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LinearProgressIndicator(
                progress = 0.35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = AppColors.IconGreen,
                backgroundColor = Color(0xFF333333)
            )
            Text(
                text = "35%",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Text(
                text = "Processing Poses...",
                fontSize = 16.sp,
                color = AppColors.TextWhite,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LinearProgressIndicator(
                progress = 0.12f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = AppColors.RidgeBlue,
                backgroundColor = Color(0xFF333333)
            )
            Text(
                text = "12%",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "This may take a few minutes...",
                fontSize = 12.sp,
                color = AppColors.ChartNoDataText,
                modifier = Modifier.padding(top = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Stride Signal",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = AppColors.CardInnerDark,
                elevation = 0.dp
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val w = size.width
                    val h = size.height
                    val pts = listOf(0.2f, 0.45f, 0.72f, 0.55f, 0.88f, 0.4f, 0.62f)
                    val path = Path()
                    pts.forEachIndexed { i, y ->
                        val x = (i.toFloat() / (pts.size - 1)) * w
                        val yPos = h - y * h
                        if (i == 0) path.moveTo(x, yPos) else path.lineTo(x, yPos)
                    }
                    drawPath(path = path, color = AppColors.SignalsOrange, style = Stroke(width = 4f))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "L Ankle: —°\nR Ankle: —°",
                    fontSize = 12.sp,
                    color = AppColors.SignalsOrange,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "L Knee: —°\nR Knee: —°",
                    fontSize = 12.sp,
                    color = AppColors.IconGreen,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "L Hip: —°\nR Hip: —°",
                    fontSize = 12.sp,
                    color = AppColors.RidgeBlue,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Torso: —°",
                    fontSize = 12.sp,
                    color = AppColors.HipPink,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = AppColors.CardSurfaceDark,
                elevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                val latest = database.gaitScoreDao().getLatestScoreGlobally()
                                if (latest != null) {
                                    viewResultsHint = null
                                    onNavigateToLatestResults(latest.id)
                                } else {
                                    viewResultsHint = "No saved analyses yet — run one from a patient first."
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = AppColors.IconPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "View Results",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextWhite
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AppColors.IconPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            viewResultsHint?.let { hint ->
                Text(
                    text = hint,
                    color = AppColors.ChartAxisText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
        }
    }
}
