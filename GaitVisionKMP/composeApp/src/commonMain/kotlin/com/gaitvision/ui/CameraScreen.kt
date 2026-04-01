package com.gaitvision.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.VideoEntity
import com.gaitvision.logic.GaitAnalyzer
import com.gaitvision.platform.PoseDetector
import com.gaitvision.platform.Pose
import com.gaitvision.platform.VideoProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun CameraScreen(
    patientId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (Long) -> Unit,
    poseDetector: PoseDetector,
    videoProcessor: VideoProcessor,
    database: AppDatabase
) {
    val scope = rememberCoroutineScope()
    var pose by remember { mutableStateOf<Pose?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var poseCount by remember { mutableIntStateOf(0) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    val analyzer = remember { GaitAnalyzer() }

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
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing Gait...", style = MaterialTheme.typography.h6)
                        Text(
                            "Processed $poseCount frames",
                            style = MaterialTheme.typography.body2,
                            color = Color.Gray
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
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Card
            if (!isRecording && !isProcessing) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp,
                    backgroundColor = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = if (pose != null) "Pose detected — ready to record" else "Waiting for pose detection...",
                        color = if (pose != null) Color.Green else Color.Yellow,
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
                                // Save video metadata
                                val videoId = database.videoDao().insertVideo(
                                    VideoEntity(
                                        patientId = patientId,
                                        originalVideoPath = "live_camera",
                                        editedVideoPath = "",
                                        recordedAt = Clock.System.now().toEpochMilliseconds()
                                    )
                                )
                                val scoreEntity = analyzer.analyze(patientId = patientId, videoId = videoId)
                                val scoreId = database.gaitScoreDao().insertScore(scoreEntity)
                                analyzer.clear()
                                isProcessing = false
                                onNavigateToResults(scoreId)
                            } catch (e: Exception) {
                                isProcessing = false
                                analyzer.clear()
                                onNavigateBack()
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
