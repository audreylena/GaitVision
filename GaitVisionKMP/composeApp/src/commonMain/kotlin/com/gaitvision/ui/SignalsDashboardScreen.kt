package com.gaitvision.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SignalsDashboardScreen(
    scoreId: Long,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
    ) {
        CommonScreenHeader(title = "Signal Dashboard", onBack = onNavigateBack)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            backgroundColor = AppColors.CardInnerDark,
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Step Detection", fontSize = 10.sp, color = Color(0xFF999999))
                    Text(
                        "INTER_ANKLE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.IconPurple
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Valid Strides", fontSize = 10.sp, color = Color(0xFF999999))
                    Text(
                        "0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.IconGreen
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Frame Valid %", fontSize = 10.sp, color = Color(0xFF999999))
                    Text(
                        "0%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.RidgeBlue
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            backgroundColor = AppColors.CardSurfaceDark,
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = AppColors.IconPurple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Inter-Ankle Distance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextWhite
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            backgroundColor = AppColors.CardInnerDark,
            elevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val grid = AppColors.ChartGrid
                    val h = size.height
                    for (i in 0..4) {
                        val y = h * (i / 4f)
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }
                    val path = Path()
                    val pts = 80
                    val amp = size.height / 4f
                    val freq = 5f * PI.toFloat() / size.width
                    for (i in 0..pts) {
                        val x = size.width * (i / pts.toFloat())
                        val y = size.height / 2f + amp * sin(x * freq + scoreId.toFloat() * 0.01f)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path = path, color = AppColors.IconLightBlue, style = Stroke(width = 3f))
                }
            }
        }

        Text(
            text = "Blue = Left, Red = Right | Green bands = Valid strides",
            fontSize = 11.sp,
            color = AppColors.ChartNoDataText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}
