package com.gaitvision.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.VideoEntity
import com.gaitvision.logic.GaitAnalyzer
import com.gaitvision.platform.FilePicker
import com.gaitvision.platform.VideoProcessor
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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
    val scope = rememberCoroutineScope()
    val analyzer = remember { GaitAnalyzer() }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var showPatientPicker by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "camera" or "upload"

    val recentPatients by database.patientDao().getAllPatientsFlow().collectAsState(initial = emptyList())

    if (showPatientPicker) {
        AlertDialog(
            onDismissRequest = { showPatientPicker = false },
            title = { Text("Select Patient") },
            backgroundColor = MaterialTheme.colors.surface,
            text = {
                LazyColumn {
                    items(recentPatients) { patient ->
                        TextButton(
                            onClick = {
                                showPatientPicker = false
                                when (pendingAction) {
                                    "camera" -> onNavigateToCamera(patient.id)
                                    "upload" -> { }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${patient.firstName} ${patient.lastName}", color = MaterialTheme.colors.onSurface)
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showPatientPicker = false }) { Text("Cancel", color = PrimaryBlue) }
            }
        )
    }

    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        bottomBar = {
            BottomNavigation(backgroundColor = MaterialTheme.colors.surface, elevation = 8.dp) {
                BottomNavigationItem(
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    selectedContentColor = PrimaryBlue,
                    unselectedContentColor = TextSlate
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onNavigateToHelp,
                    icon = { Icon(Icons.Default.Info, contentDescription = "Help") },
                    label = { Text("Help", fontSize = 10.sp) },
                    selectedContentColor = PrimaryBlue,
                    unselectedContentColor = TextSlate
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onNavigateToInfo,
                    icon = { Icon(Icons.Default.Info, contentDescription = "Info") },
                    label = { Text("Info", fontSize = 10.sp) },
                    selectedContentColor = PrimaryBlue,
                    unselectedContentColor = TextSlate
                )
                BottomNavigationItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) },
                    selectedContentColor = PrimaryBlue,
                    unselectedContentColor = TextSlate
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Application Logo placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Logo", modifier = Modifier.size(50.dp), tint = PrimaryBlue)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Welcome Section
            Text(
                text = "GaitVision",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onBackground
            )
            Text(
                text = "Good evening!",
                style = MaterialTheme.typography.body1,
                color = TextSlate,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Action Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardActionCard(
                    title = "Search",
                    subtitle = "Find Patient",
                    icon = Icons.Default.Search,
                    iconColor = PrimaryBlue,
                    onClick = onNavigateToPatientList,
                    modifier = Modifier.weight(1f)
                )
                DashboardActionCard(
                    title = "New",
                    subtitle = "Add Patient",
                    icon = Icons.Default.Add,
                    iconColor = AccentGreen,
                    onClick = onNavigateToCreatePatient,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Analysis Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onNavigateToCamera(0L) }),
                backgroundColor = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quick Analysis", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colors.onSurface)
                        Text("Analyze gait without creating a patient profile", style = MaterialTheme.typography.caption, color = TextSlate)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextSlate)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent Patients Section
            Text(
                text = "Recent Patients",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            if (recentPatients.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No recent patients",
                        style = MaterialTheme.typography.body1,
                        color = TextSlate
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(recentPatients.take(5)) { patient ->
                        PatientCard(
                            name = "${patient.firstName} ${patient.lastName}",
                            date = "Age: ${patient.age ?: "N/A"}",
                            score = 0,
                            onClick = { onNavigateToPatientProfile(patient.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.caption, color = TextSlate)
        }
    }
}

@Composable
fun PatientCard(name: String, date: String, score: Int, onClick: () -> Unit = {}) {
    Card(
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colors.onSurface)
                Text(date, style = MaterialTheme.typography.caption, color = TextSlate)
            }
            if (score > 0) {
                Surface(
                    color = if (score > 80) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Score: $score",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = if (score > 80) Color(0xFF137333) else Color(0xFFC5221F)
                    )
                }
            }
        }
    }
}
