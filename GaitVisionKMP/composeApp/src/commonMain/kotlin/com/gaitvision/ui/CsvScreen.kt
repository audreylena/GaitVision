package com.gaitvision.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gaitvision.logic.*
import com.gaitvision.platform.FilePicker
import com.gaitvision.platform.rememberCsvSharer

@Composable
fun CsvScreen(onNavigateBack: () -> Unit) {
    val csvSharer = rememberCsvSharer()

    // Export state
    var exportStatus by remember { mutableStateOf("") }
    var exportSuccess by remember { mutableStateOf<Boolean?>(null) }

    // Import state
    var importedContent by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf("") }

    val csvPicker = remember {
        FilePicker { path ->
            if (path != null) {
                importStatus = "Imported: ${path.substringAfterLast("/")}"
                importedContent = "File picked from:\n$path\n\n(Open with a spreadsheet app to view full contents)"
            } else {
                importStatus = "Import cancelled"
            }
        }
    }
    csvPicker.register()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data / CSV") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 4.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Export Section ───────────────────────────────────────────────
            Text(
                "EXPORT",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.secondary,
                fontWeight = FontWeight.Bold
            )

            Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Export Gait CSV",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Export a sample gait analysis result as a CSV file compatible with the PC pipeline.",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            // Generate a sample CSV using GaitCsvExporter
                            val sampleDiagnostics = GaitDiagnostics(
                                videoId = "sample_001",
                                fpsDetected = 30f,
                                durationS = 10f,
                                numFramesTotal = 300,
                                numFramesValid = 260,
                                validFrameRate = 0.87f,
                                numStepsDetected = 18,
                                numStridesValid = 8,
                                estimatedCadenceSpm = 108f,
                                walkingDirection = "right",
                                wasFlipped = false,
                                qualityFlag = QualityFlag.OK
                            )
                            val sampleFeatures = GaitFeatures(
                                cadence_spm = 108f,
                                stride_time_s = 1.11f,
                                stride_time_cv = 0.04f,
                                step_time_asymmetry = 0.02f,
                                stride_length_norm = 0.88f,
                                stride_amp_norm = 0.45f,
                                step_length_asymmetry = 0.03f,
                                knee_left_rom = 42f,
                                knee_right_rom = 44f,
                                knee_left_max = 62f,
                                knee_right_max = 63f,
                                ldj_knee_left = -4.2f,
                                ldj_knee_right = -4.0f,
                                ldj_hip = -3.8f,
                                trunk_lean_std_deg = 2.1f,
                                inter_ankle_cv = 0.08f,
                                valid_stride_count = 8
                            )
                            val sampleScore = ScoringResult(
                                aeScore = 82f,
                                ridgeScore = 79f,
                                pcaScore = 85f
                            )
                            val csv = GaitCsvExporter.generateCsvString(
                                features = sampleFeatures,
                                diagnostics = sampleDiagnostics,
                                score = sampleScore,
                                participantId = "P001",
                                videoName = "sample_video.mp4"
                            )
                            val filename = GaitCsvExporter.generateFilename("P001")
                            val path = csvSharer.saveCsv(csv, filename)
                            if (path != null) {
                                exportSuccess = true
                                exportStatus = "Saved: $filename"
                                csvSharer.shareCsv(path)
                            } else {
                                exportSuccess = false
                                exportStatus = "Export failed — check storage permissions"
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export & Share CSV")
                    }

                    if (exportStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            exportStatus,
                            style = MaterialTheme.typography.caption,
                            color = if (exportSuccess == true) Color(0xFF137333) else Color(0xFFC5221F)
                        )
                    }
                }
            }

            // ── Import Section ───────────────────────────────────────────────
            Text(
                "IMPORT",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.secondary,
                fontWeight = FontWeight.Bold
            )

            Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Import Gait CSV",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Import a previously exported CSV file from the PC pipeline or another device.",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { csvPicker.launchCsv() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose CSV File")
                    }

                    if (importStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            importStatus,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary
                        )
                    }

                    if (importedContent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                importedContent,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // ── Info ─────────────────────────────────────────────────────────
            Text(
                "CSV files follow the PC pipeline format with participant ID, video name, gait metrics, and model scores.",
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
        }
    }
}
