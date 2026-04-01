package com.gaitvision.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.GaitScoreEntity
import com.gaitvision.data.VideoEntity
import com.gaitvision.logic.GaitAnalyzer
import com.gaitvision.platform.FilePicker
import com.gaitvision.platform.VideoProcessor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun DashboardScreen(
    onNavigateToCamera: (Long) -> Unit,       // Takes patientId
    onNavigateToAnalysis: () -> Unit,
    onNavigateToPatientList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPatientProfile: (Long) -> Unit,
    onNavigateToResults: (Long) -> Unit,       // Takes scoreId
    database: AppDatabase,
    videoProcessor: VideoProcessor
) {
    val scope = rememberCoroutineScope()
    val analyzer = remember { GaitAnalyzer() }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var showPatientPicker by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "camera" or "upload"

    val recentPatients by database.patientDao().getAllPatientsFlow().collectAsState(initial = emptyList())

    // Patient picker dialog for choosing which patient to associate with new analysis
    if (showPatientPicker) {
        AlertDialog(
            onDismissRequest = { showPatientPicker = false },
            title = { Text("Select Patient") },
            text = {
                LazyColumn {
                    items(recentPatients) { patient ->
                        TextButton(
                            onClick = {
                                showPatientPicker = false
                                when (pendingAction) {
                                    "camera" -> onNavigateToCamera(patient.id)
                                    "upload" -> {
                                        // File picker will be launched by FilePicker composable
                                        // We store patientId for use in the callback
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${patient.firstName} ${patient.lastName}")
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showPatientPicker = false }) { Text("Cancel") }
            }
        )
    }

    // FilePicker with patientId passed at launch time via closure
    var activePatientId by remember { mutableLongStateOf(0L) }
    val filePicker = remember {
        FilePicker { path ->
            if (path != null) {
                isProcessing = true
                val patientId = activePatientId
                scope.launch {
                    try {
                        val outputPath = path.substringBeforeLast(".") + "_processed.mp4"

                        // Insert video record
                        val videoId = database.videoDao().insertVideo(
                            VideoEntity(
                                patientId = patientId,
                                originalVideoPath = path,
                                editedVideoPath = outputPath,
                                recordedAt = Clock.System.now().toEpochMilliseconds()
                            )
                        )

                        videoProcessor.processVideo(
                            inputPath = path,
                            outputPath = outputPath,
                            onProgress = { p -> progress = p },
                            onPoseDetected = { pose -> analyzer.addPose(pose) }
                        )

                        val scoreEntity = analyzer.analyze(patientId = patientId, videoId = videoId)
                        val scoreId = database.gaitScoreDao().insertScore(scoreEntity)
                        analyzer.clear()

                        isProcessing = false
                        onNavigateToResults(scoreId)
                    } catch (e: Exception) {
                        isProcessing = false
                        analyzer.clear()
                    }
                }
            }
        }
    }
    filePicker.register()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GaitVision", style = MaterialTheme.typography.h6) },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 4.dp,
                actions = {
                    androidx.compose.material.IconButton(onClick = onNavigateToPatientList) {
                        Icon(Icons.Default.Person, contentDescription = "Patients")
                    }
                    androidx.compose.material.IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Welcome Section
            Text(
                text = "Welcome, Doctor",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(vertical = 16.dp).align(Alignment.Start)
            )

            // Action Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = "New Analysis",
                    icon = Icons.Default.Add,
                    onClick = {
                        if (recentPatients.isEmpty()) {
                            // No patients — go to create patient first
                            onNavigateToPatientList()
                        } else if (recentPatients.size == 1) {
                            onNavigateToCamera(recentPatients.first().id)
                        } else {
                            pendingAction = "camera"
                            showPatientPicker = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    title = "Upload Video",
                    icon = Icons.Default.Share,
                    onClick = {
                        if (recentPatients.isEmpty()) {
                            onNavigateToPatientList()
                        } else if (recentPatients.size == 1) {
                            activePatientId = recentPatients.first().id
                            filePicker.launch()
                        } else {
                            pendingAction = "upload"
                            showPatientPicker = true
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colors.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Processing Video: $progress%", style = MaterialTheme.typography.body1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Patients Section
            Text(
                text = "Recent Patients",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            )

            if (recentPatients.isEmpty()) {
                Text(
                    text = "No patients yet. Tap 'New Analysis' to begin.",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentPatients.take(5)) { patient ->
                        PatientCard(
                            name = "${patient.firstName} ${patient.lastName}",
                            date = "Age: ${patient.age ?: "N/A"}",
                            score = 0, // Score will be loaded per patient in their profile
                            onClick = { onNavigateToPatientProfile(patient.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(enabled = enabled, onClick = onClick),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (enabled) MaterialTheme.colors.surface else Color.LightGray
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.button, color = MaterialTheme.colors.onSurface)
        }
    }
}

@Composable
fun PatientCard(name: String, date: String, score: Int, onClick: () -> Unit = {}) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                Text(date, style = MaterialTheme.typography.caption)
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
