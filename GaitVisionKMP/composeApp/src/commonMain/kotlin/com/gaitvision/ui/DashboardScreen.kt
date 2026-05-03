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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.platform.VideoProcessor
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DashboardScreen(
    onNavigateToCamera: (Long) -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToPatientList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPatientProfile: (Long) -> Unit,
    onNavigateToResults: (Long) -> Unit,
    database: AppDatabase,
    videoProcessor: VideoProcessor,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToInfo: () -> Unit = {},
    onNavigateToCreatePatient: () -> Unit = {}
) {
    val recentPatients by database.patientDao().getAllPatientsFlow().collectAsState(initial = emptyList())

    Scaffold(
        backgroundColor = Color.Transparent,
        bottomBar = {
            LegacyDashboardBottomNav(
                selectedTab = DashboardNavTab.Home,
                onHome = { },
                onHelp = onNavigateToHelp,
                onInfo = onNavigateToInfo,
                onSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.DashboardGradientTop,
                            AppColors.DashboardGradientBottom
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Icon(
                    Icons.Default.Person,
                    contentDescription = "GaitVision logo",
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.PrimaryBlue
                )

                Text(
                    text = "GaitVision",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextWhite,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = dashboardGreeting(),
                    fontSize = 14.sp,
                    color = AppColors.TextMutedGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardActionCard(
                        title = "Search",
                        subtitle = "Find Patient",
                        icon = Icons.Default.Search,
                        iconTint = AppColors.PrimaryBlue,
                        onClick = onNavigateToPatientList,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardActionCard(
                        title = "New",
                        subtitle = "Add Patient",
                        icon = Icons.Default.PersonAdd,
                        iconTint = AppColors.AccentEmerald,
                        onClick = onNavigateToCreatePatient,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCamera(0L) },
                    backgroundColor = AppColors.CardSurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    elevation = 0.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = AppColors.SecondaryTeal,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Quick Analysis",
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextWhite
                            )
                            Text(
                                "Analyze gait without creating a patient profile",
                                style = MaterialTheme.typography.caption,
                                color = AppColors.TextMutedGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AppColors.TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recent Patients",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 16.dp)
                )

                if (recentPatients.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent patients",
                            style = MaterialTheme.typography.body1,
                            color = AppColors.TableHeaderGray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(recentPatients.take(5)) { patient ->
                            val idLine = patient.participantId?.let { "ID: $it" } ?: "ID: ${patient.id}"
                            val agePart = patient.age?.let { "$it yrs" } ?: "—"
                            RecentPatientRowCard(
                                patientName = "${patient.firstName} ${patient.lastName}".trim(),
                                detailLine = "$idLine · $agePart",
                                onClick = { onNavigateToPatientProfile(patient.id) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dashboardGreeting(): String {
    val hour = try {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    } catch (_: Exception) {
        12
    }
    return when {
        hour < 12 -> "Good Morning!"
        hour < 17 -> "Good Afternoon!"
        else -> "Good Evening!"
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AppColors.CardSurfaceDark
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextWhite
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = AppColors.TextMutedGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
