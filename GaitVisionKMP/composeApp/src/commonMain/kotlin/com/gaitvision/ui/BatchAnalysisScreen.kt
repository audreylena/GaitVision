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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.AuditLogger
import com.gaitvision.data.VideoEntity
import com.gaitvision.logic.GaitAnalyzer
import com.gaitvision.platform.MultiVideoPicker
import com.gaitvision.platform.VideoProcessor
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private enum class BatchVideoStatus { QUEUED, RUNNING, DONE, FAILED }

private data class BatchVideoItem(
    val uri: String,
    val displayName: String,
    val status: BatchVideoStatus,
    val score: Int? = null,
    val errorMessage: String? = null
)

private fun processedVideoOutputPath(inputPath: String): String {
    val lastSlash = inputPath.lastIndexOf('/')
    val lastDot = inputPath.lastIndexOf('.')
    val filenameHasExtension = lastDot > lastSlash
    return if (filenameHasExtension) {
        inputPath.substring(0, lastDot) + "_processed.mp4"
    } else {
        inputPath + "_processed.mp4"
    }
}

@Composable
fun BatchAnalysisScreen(
    patientId: Long,
    database: AppDatabase,
    videoProcessor: VideoProcessor,
    onNavigateBack: () -> Unit
) {
    val scope = rememberSafeCoroutineScope()
    var patientName by remember { mutableStateOf("") }
    var patientParticipantLabel by remember { mutableStateOf("—") }
    var biologicalSex by remember { mutableStateOf("") }

    val rows = remember { mutableStateListOf<BatchVideoItem>() }
    var isRunning by remember { mutableStateOf(false) }
    var doneCount by remember { mutableIntStateOf(0) }
    var failedCount by remember { mutableIntStateOf(0) }
    var lastScore by remember { mutableStateOf<Int?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var overallLabel by remember { mutableStateOf("") }
    var overallPct by remember { mutableIntStateOf(0) }
    var currentVideoLabel by remember { mutableStateOf("") }
    var currentVideoPct by remember { mutableIntStateOf(0) }

    val picker = remember { MultiVideoPicker() }
    picker.Register { paths ->
        if (isRunning || paths.isEmpty()) return@Register
        val existing = rows.map { it.uri }.toSet()
        paths.filter { it !in existing }.forEach { uri ->
            rows.add(
                BatchVideoItem(
                    uri = uri,
                    displayName = uri.substringAfterLast('/').substringBefore('?').ifBlank { "video" },
                    status = BatchVideoStatus.QUEUED
                )
            )
        }
    }

    LaunchedEffect(patientId) {
        val p = database.patientDao().getPatientById(patientId) ?: return@LaunchedEffect
        patientName = "${p.firstName} ${p.lastName}".trim().ifBlank { "Patient" }
        patientParticipantLabel = p.participantId ?: p.id.toString()
        biologicalSex = p.biologicalSex
    }

    fun attemptLeave() {
        if (isRunning) showLeaveDialog = true else onNavigateBack()
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Batch in progress") },
            text = {
                Text("Leaving will cancel the remaining videos. Already-saved analyses are kept.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onNavigateBack()
                }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Stay") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
    ) {
        CommonScreenHeader(
            title = "Batch Analysis",
            onBack = { attemptLeave() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = AppColors.CardSurfaceDark,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(patientName, color = AppColors.TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("ID $patientParticipantLabel", color = AppColors.TableHeaderText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTiny("Selected", rows.size.toString())
                        StatTiny("Done", doneCount.toString())
                        StatTiny("Failed", failedCount.toString())
                        StatTiny("Last score", lastScore?.toString() ?: "—")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { picker.launchPicker() },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) { Text("Pick videos") }
                Button(
                    onClick = {
                        scope.launch {
                            if (isRunning || rows.none { it.status == BatchVideoStatus.QUEUED }) return@launch
                            isRunning = true
                            doneCount = 0
                            failedCount = 0
                            lastScore = null
                            overallLabel = ""
                            val analyzer = GaitAnalyzer()
                            val queuedIndices = rows.mapIndexedNotNull { idx, r ->
                                if (r.status == BatchVideoStatus.QUEUED) idx else null
                            }
                            val total = queuedIndices.size
                            queuedIndices.forEachIndexed { qi, idx ->
                                val row = rows[idx]
                                rows[idx] = row.copy(status = BatchVideoStatus.RUNNING)
                                overallLabel = "Video ${qi + 1} of $total"
                                overallPct = if (total > 0) qi * 100 / total else 0
                                currentVideoLabel = row.displayName
                                currentVideoPct = 0
                                try {
                                    val inputPath = row.uri
                                    val outputPath = processedVideoOutputPath(inputPath)
                                    val videoId = database.videoDao().insertVideo(
                                        VideoEntity(
                                            patientId = patientId,
                                            originalVideoPath = inputPath,
                                            editedVideoPath = outputPath,
                                            recordedAt = Clock.System.now().toEpochMilliseconds()
                                        )
                                    )
                                    videoProcessor.processVideo(
                                        inputPath = inputPath,
                                        outputPath = outputPath,
                                        onProgress = { p -> currentVideoPct = p },
                                        onPoseDetected = { pose -> analyzer.addPose(pose) }
                                    )
                                    val scoreEntity = analyzer.analyze(
                                        patientId = patientId,
                                        videoId = videoId,
                                        biologicalSex = biologicalSex
                                    )
                                    val scoreId = database.gaitScoreDao().insertScore(scoreEntity)
                                    AuditLogger.log(
                                        database.auditLogDao(),
                                        "RUN_ANALYSIS",
                                        patientId = patientId,
                                        recordId = scoreId
                                    )
                                    analyzer.clear()
                                    val sc = scoreEntity.overallScore.toInt()
                                    lastScore = sc
                                    doneCount++
                                    rows[idx] = row.copy(status = BatchVideoStatus.DONE, score = sc)
                                } catch (e: Exception) {
                                    analyzer.clear()
                                    failedCount++
                                    rows[idx] = row.copy(
                                        status = BatchVideoStatus.FAILED,
                                        errorMessage = e.message?.take(80) ?: "Failed"
                                    )
                                }
                            }
                            overallPct = 100
                            overallLabel = "Done — $doneCount ok, $failedCount failed"
                            currentVideoLabel = ""
                            currentVideoPct = 0
                            isRunning = false
                        }
                    },
                    enabled = !isRunning && rows.any { it.status == BatchVideoStatus.QUEUED },
                    modifier = Modifier.weight(1f)
                ) { Text("Start batch") }
            }

            if (isRunning || overallLabel.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = AppColors.CardInnerDark,
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(overallLabel.ifBlank { "Progress" }, color = AppColors.TextWhite, fontSize = 14.sp)
                        LinearProgressIndicator(
                            progress = (overallPct.coerceIn(0, 100)) / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = AppColors.IconGreen,
                            backgroundColor = Color(0xFF333333)
                        )
                        Text("$overallPct%", color = AppColors.ChartAxisText, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentVideoLabel, color = AppColors.TextMutedGray, fontSize = 12.sp)
                        LinearProgressIndicator(
                            progress = (currentVideoPct.coerceIn(0, 100)) / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = AppColors.RidgeBlue,
                            backgroundColor = Color(0xFF333333)
                        )
                    }
                }
            }

            Text(
                text = when {
                    rows.isEmpty() -> "No videos selected"
                    isRunning -> "Running batch…"
                    else -> "${rows.size} video(s) queued"
                },
                color = AppColors.TableHeaderText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tap Pick videos to add recordings", color = AppColors.ChartAxisText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(rows, key = { _, row -> row.uri }) { _, row ->
                        BatchVideoRowUi(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTiny(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = AppColors.ChartAxisText)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextWhite)
    }
}

@Composable
private fun BatchVideoRowUi(row: BatchVideoItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = AppColors.CardSurfaceDark,
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(row.displayName, color = AppColors.TextWhite, fontWeight = FontWeight.Medium)
            val statusText = when (row.status) {
                BatchVideoStatus.QUEUED -> "Queued"
                BatchVideoStatus.RUNNING -> "Processing…"
                BatchVideoStatus.DONE -> "Done — score ${row.score ?: "—"}"
                BatchVideoStatus.FAILED -> "Failed: ${row.errorMessage ?: "?"}"
            }
            Text(statusText, color = AppColors.TableHeaderText, fontSize = 12.sp)
        }
    }
}
