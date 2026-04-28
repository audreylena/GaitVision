package com.gaitvision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.AuditLogger
import com.gaitvision.data.VideoEntity
import com.gaitvision.data.PatientEntity
import com.gaitvision.logic.GaitAnalyzer
import com.gaitvision.platform.FilePicker
import com.gaitvision.platform.PoseDetector
import com.gaitvision.platform.Pose
import com.gaitvision.platform.VideoProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class CameraScreenMode {
    SELECTION, RECORDING
}

@Composable
fun CameraScreen(
    patientId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (Long) -> Unit,
    poseDetector: PoseDetector,
    videoProcessor: VideoProcessor,
    database: AppDatabase
) {
    val scope = rememberSafeCoroutineScope()
    var mode by remember { mutableStateOf(CameraScreenMode.SELECTION) }
    
    var pose by remember { mutableStateOf<Pose?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var poseCount by remember { mutableIntStateOf(0) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    val analyzer = remember { GaitAnalyzer() }

    // Helper to get actual patientId (creating anonymous one if needed)
    var resolvedPatientId by remember { mutableLongStateOf(patientId) }
    var patientBiologicalSex by remember { mutableStateOf("") }
    
    LaunchedEffect(patientId) {
        try {
            if (patientId == 0L) {
                val qid = database.patientDao().insertPatient(
                    PatientEntity(
                        firstName = "Quick",
                        lastName = "Analysis",
                        participantId = "GUEST",
                        createdAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
                resolvedPatientId = qid
            } else {
                val p = database.patientDao().getPatientById(patientId)
                patientBiologicalSex = p?.biologicalSex ?: ""
            }
        } catch (e: Exception) {
            println("CameraScreen: patient setup failed: ${e.message}")
        }
    }

    // Countdown timer while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    val filePicker = remember {
        FilePicker { path ->
            if (path != null) {
                isProcessing = true
                scope.launch {
                    try {
                        val outputPath = path.substringBeforeLast(".") + "_processed.mp4"
                        val videoId = database.videoDao().insertVideo(
                            VideoEntity(
                                patientId = resolvedPatientId,
                                originalVideoPath = path,
                                editedVideoPath = outputPath,
                                recordedAt = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                        videoProcessor.processVideo(
                            inputPath = path,
                            outputPath = outputPath,
                            onProgress = { p -> progress = p },
                            onPoseDetected = { p -> analyzer.addPose(p) }
                        )

                        val scoreEntity = analyzer.analyze(
                            patientId = resolvedPatientId,
                            videoId = videoId,
                            biologicalSex = patientBiologicalSex
                        )
                        val scoreId = database.gaitScoreDao().insertScore(scoreEntity)
                        AuditLogger.log(database.auditLogDao(), "RUN_ANALYSIS", patientId = resolvedPatientId, recordId = scoreId)
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

    if (mode == CameraScreenMode.SELECTION) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "Select Video",
                                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colors.onBackground
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.colors.onBackground,
                    elevation = 0.dp,
                    actions = { Spacer(modifier = Modifier.width(48.dp)) } // balance navigationIcon
                )
            },
            backgroundColor = MaterialTheme.colors.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Video Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Video preview will appear here",
                        color = TextSlate,
                        style = MaterialTheme.typography.body1
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Failed to load video", color = Color.Gray, style = MaterialTheme.typography.caption)
                Spacer(modifier = Modifier.height(24.dp))

                // Gallery & Record Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { filePicker.launch() },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Gallery", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Gallery", color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { mode = CameraScreenMode.RECORDING },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Record", tint = AccentGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Record", color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recording Date Input
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { /* TODO: Date Picker */ },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Date", tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recording Date", style = MaterialTheme.typography.caption, color = TextSlate)
                            Text("Today", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colors.onSurface)
                        }
                        Icon(Icons.Default.Create, contentDescription = "Edit", tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Processing overlay for file picker progress
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = CardSurfaceDark
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Processing Video: $progress%", style = MaterialTheme.typography.h6, color = Color.White)
                    }
                }
            }
        }
    } else {
        // Record Mode (Live Camera)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Camera preview with pose detection
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                poseDetector = poseDetector,
                videoProcessor = videoProcessor,
                onPoseDetected = { newPose ->
                    pose = newPose
                    if (isRecording) {
                        analyzer.addPose(newPose)
                        poseCount++
                    }
                }
            )

            // Pose skeleton overlay
            PoseOverlay(
                pose = pose,
                modifier = Modifier.fillMaxSize()
            )

            // Top controls bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mode = CameraScreenMode.SELECTION }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Red.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "  REC ${recordingSeconds}s",
                                color = Color.White,
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Processing overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = CardSurfaceDark
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analyzing Gait...", style = MaterialTheme.typography.h6, color = Color.White)
                            Text(
                                "Processed $poseCount frames",
                                style = MaterialTheme.typography.body2,
                                color = TextSlate
                            )
                        }
                    }
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                if (!isRecording && !isProcessing) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = 0.dp,
                        backgroundColor = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = if (pose != null) "Pose detected — ready to record" else "Waiting for pose detection...",
                            color = if (pose != null) AccentGreen else Color.Yellow,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Record / Stop Button
                Button(
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                            isProcessing = true
                            scope.launch {
                                try {
                                    val videoId = database.videoDao().insertVideo(
                                        VideoEntity(
                                            patientId = resolvedPatientId,
                                            originalVideoPath = "live_camera",
                                            editedVideoPath = "",
                                            recordedAt = Clock.System.now().toEpochMilliseconds()
                                        )
                                    )
                                    val scoreEntity = analyzer.analyze(
                                        patientId = resolvedPatientId,
                                        videoId = videoId,
                                        biologicalSex = patientBiologicalSex
                                    )
                                    val scoreId = database.gaitScoreDao().insertScore(scoreEntity)
                                    AuditLogger.log(database.auditLogDao(), "RUN_ANALYSIS", patientId = resolvedPatientId, recordId = scoreId)
                                    analyzer.clear()
                                    isProcessing = false
                                    onNavigateToResults(scoreId)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    analyzer.clear()
                                    mode = CameraScreenMode.SELECTION
                                }
                            }
                        } else {
                            poseCount = 0
                            analyzer.clear()
                            isRecording = true
                        }
                    },
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isRecording) Color.Red else Color.White
                    ),
                    elevation = ButtonDefaults.elevation(0.dp),
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) Color.White else Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (isRecording) "Tap to stop and analyze" else "Tap to start recording",
                    color = Color.White,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
