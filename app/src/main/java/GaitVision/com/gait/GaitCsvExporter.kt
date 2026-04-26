package GaitVision.com.gait

import android.util.Log
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV export utility for gait analysis results.
 * Exports in PC pipeline compatible format.
 * Uses OutputStream so callers can write to any destination (SAF, file, etc.)
 */
object GaitCsvExporter {
    
    private const val TAG = "GaitCsvExporter"

    /** Sanitize a value for safe CSV output (prevents formula injection in Excel/Sheets). */
    private fun sanitize(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val startsWithFormula = value.firstOrNull()?.let { it == '=' || it == '+' || it == '-' || it == '@' } ?: false
        return when {
            startsWithFormula -> "\"'${value.replace("\"", "\"\"")}\""
            needsQuoting -> "\"${value.replace("\"", "\"\"")}\""
            else -> value
        }
    }

    /** Generate a suggested filename for the CSV export. */
    fun generateFilename(participantId: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${participantId}_gait_${timestamp}.csv"
    }

    /**
     * Write gait features and diagnostics as CSV to the given OutputStream.
     * Returns true on success.
     */
    fun writeToStream(
        outputStream: OutputStream,
        features: GaitFeatures?,
        diagnostics: GaitDiagnostics,
        score: ScoringResult?,
        participantId: String,
        videoName: String
    ): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            OutputStreamWriter(outputStream).use { writer ->
                // Header row
                val headers = mutableListOf(
                    "participant_id",
                    "video_name",
                    "timestamp",
                    "quality_flag",
                    "walking_direction",
                    "was_flipped",
                    "fps_detected",
                    "duration_s",
                    "num_frames_total",
                    "num_frames_valid",
                    "valid_frame_rate",
                    "num_steps_detected",
                    "num_strides_valid"
                )
                
                // Add feature columns
                headers.addAll(GaitFeatures.FEATURE_COLUMNS)
                
                // Add score columns (3 models)
                headers.addAll(listOf(
                    "ae_score",
                    "ridge_score",
                    "pca_score"
                ))
                
                writer.write(headers.joinToString(","))
                writer.write("\n")
                
                // Data row
                val values = mutableListOf<String>()
                
                // Metadata (sanitize user-controlled strings to prevent CSV injection)
                values.add(sanitize(participantId))
                values.add(sanitize(videoName))
                values.add(timestamp)
                values.add(diagnostics.qualityFlag.name)
                values.add(sanitize(diagnostics.walkingDirection))
                values.add(diagnostics.wasFlipped.toString())
                values.add(diagnostics.fpsDetected.toString())
                values.add(diagnostics.durationS.toString())
                values.add(diagnostics.numFramesTotal.toString())
                values.add(diagnostics.numFramesValid.toString())
                values.add(diagnostics.validFrameRate.toString())
                values.add(diagnostics.numStepsDetected.toString())
                values.add(diagnostics.numStridesValid.toString())
                
                // Features (or NaN if not available)
                if (features != null) {
                    val featureArray = features.toFeatureArray()
                    for (f in featureArray) {
                        values.add(if (f.isNaN()) "NaN" else f.toString())
                    }
                } else {
                    repeat(16) { values.add("NaN") }
                }
                
                // Scores (3 models)
                if (score != null) {
                    values.add(if (score.aeScore.isNaN()) "NaN" else score.aeScore.toString())
                    values.add(if (score.ridgeScore.isNaN()) "NaN" else score.ridgeScore.toString())
                    values.add(if (score.pcaScore.isNaN()) "NaN" else score.pcaScore.toString())
                } else {
                    repeat(3) { values.add("NaN") }
                }
                
                writer.write(values.joinToString(","))
                writer.write("\n")
            }
            
            Log.d(TAG, "CSV written successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing CSV", e)
            false
        }
    }
    
    /**
     * Generate a summary string for display.
     */
    fun generateSummary(
        features: GaitFeatures?,
        diagnostics: GaitDiagnostics,
        score: ScoringResult?
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("=== Gait Analysis Summary ===")
        sb.appendLine()
        
        // Quality status
        sb.appendLine("Quality: ${diagnostics.qualityFlag}")
        if (diagnostics.rejectionReasons.isNotEmpty()) {
            sb.appendLine("Issues: ${diagnostics.rejectionReasons.joinToString(", ")}")
        }
        sb.appendLine()
        
        // Video info
        sb.appendLine("Video Info:")
        sb.appendLine("  Duration: %.1f s".format(diagnostics.durationS))
        sb.appendLine("  Frames: ${diagnostics.numFramesValid}/${diagnostics.numFramesTotal} valid")
        sb.appendLine("  Detection rate: ${(diagnostics.validFrameRate * 100).toInt()}%")
        sb.appendLine("  Walking direction: ${diagnostics.walkingDirection}")
        sb.appendLine()
        
        if (features != null) {
            // Key metrics
            sb.appendLine("Gait Metrics:")
            sb.appendLine("  Cadence: %.1f steps/min".format(features.cadence_spm))
            sb.appendLine("  Stride time: %.2f s".format(features.stride_time_s))
            sb.appendLine("  Step asymmetry: %.1f%%".format(features.step_time_asymmetry * 100))
            sb.appendLine()
            
            sb.appendLine("Range of Motion:")
            sb.appendLine("  Knee (L/R): %.1f° / %.1f°".format(features.knee_left_rom, features.knee_right_rom))
            sb.appendLine("  Max knee flexion (L/R): %.1f° / %.1f°".format(features.knee_left_max, features.knee_right_max))
            sb.appendLine()
            
            sb.appendLine("Stability:")
            sb.appendLine("  Trunk lean std: %.1f°".format(features.trunk_lean_std_deg))
            sb.appendLine("  Inter-ankle CV: %.2f".format(features.inter_ankle_cv))
            sb.appendLine()
        }
        
        if (score != null) {
            sb.appendLine("Model Scores (100 = healthy):")
            if (!score.aeScore.isNaN()) sb.appendLine("  AE (DB): %.0f/100".format(score.aeScore))
            if (!score.ridgeScore.isNaN()) sb.appendLine("  Ridge: %.0f/100".format(score.ridgeScore))
            if (!score.pcaScore.isNaN()) sb.appendLine("  PCA: %.0f/100".format(score.pcaScore))
        }
        
        return sb.toString()
    }
}
