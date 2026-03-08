package com.gaitvision.logic

import com.gaitvision.logic.Log
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * CSV export utility for gait analysis results.
 * Exports in PC pipeline compatible format.
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

    private fun getCurrentTimestampString(): String {
        val currentMoment = Clock.System.now()
        val dt = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.year}${dt.monthNumber.toString().padStart(2, '0')}${dt.dayOfMonth.toString().padStart(2, '0')}_${dt.hour.toString().padStart(2, '0')}${dt.minute.toString().padStart(2, '0')}${dt.second.toString().padStart(2, '0')}"
    }

    /** Generate a suggested filename for the CSV export. */
    fun generateFilename(participantId: String): String {
        return "${participantId}_gait_${getCurrentTimestampString()}.csv"
    }

    /**
     * Write gait features and diagnostics as a CSV string.
     */
    fun generateCsvString(
        features: GaitFeatures?,
        diagnostics: GaitDiagnostics,
        score: ScoringResult?,
        participantId: String,
        videoName: String
    ): String {
        return try {
            val timestamp = getCurrentTimestampString()

            val sb = StringBuilder()
            
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
            
            sb.append(headers.joinToString(","))
            sb.append("\n")
            
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
            
            sb.append(values.joinToString(","))
            sb.append("\n")
            
            Log.d(TAG, "CSV generated successfully")
            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating CSV", e)
            ""
        }
    }
    
    // Helper for simple formatting since String.format is JVM only
    private fun Float.fmt(decimals: Int = 1): String {
        val asString = this.toString()
        val parts = asString.split('.')
        if (parts.size == 1) return asString
        if (parts[1].length <= decimals) return asString
        return "${parts[0]}.${parts[1].substring(0, decimals)}"
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
        
        sb.append("=== Gait Analysis Summary ===\n\n")
        
        // Quality status
        sb.append("Quality: ${diagnostics.qualityFlag}\n")
        if (diagnostics.rejectionReasons.isNotEmpty()) {
            sb.append("Issues: ${diagnostics.rejectionReasons.joinToString(", ")}\n")
        }
        sb.append("\n")
        
        // Video info
        sb.append("Video Info:\n")
        sb.append("  Duration: ${diagnostics.durationS.fmt(1)} s\n")
        sb.append("  Frames: ${diagnostics.numFramesValid}/${diagnostics.numFramesTotal} valid\n")
        sb.append("  Detection rate: ${(diagnostics.validFrameRate * 100).toInt()}%\n")
        sb.append("  Walking direction: ${diagnostics.walkingDirection}\n\n")
        
        if (features != null) {
            // Key metrics
            sb.append("Gait Metrics:\n")
            sb.append("  Cadence: ${features.cadence_spm.fmt(1)} steps/min\n")
            sb.append("  Stride time: ${features.stride_time_s.fmt(2)} s\n")
            sb.append("  Step asymmetry: ${(features.step_time_asymmetry * 100).fmt(1)}%\n\n")
            
            sb.append("Range of Motion:\n")
            sb.append("  Knee (L/R): ${features.knee_left_rom.fmt(1)}° / ${features.knee_right_rom.fmt(1)}°\n")
            sb.append("  Max knee flexion (L/R): ${features.knee_left_max.fmt(1)}° / ${features.knee_right_max.fmt(1)}°\n\n")
            
            sb.append("Stability:\n")
            sb.append("  Trunk lean std: ${features.trunk_lean_std_deg.fmt(1)}°\n")
            sb.append("  Inter-ankle CV: ${features.inter_ankle_cv.fmt(2)}\n\n")
        }
        
        if (score != null) {
            sb.append("Model Scores (100 = healthy):\n")
            if (!score.aeScore.isNaN()) sb.append("  AE (DB): ${score.aeScore.fmt(0)}/100\n")
            if (!score.ridgeScore.isNaN()) sb.append("  Ridge: ${score.ridgeScore.fmt(0)}/100\n")
            if (!score.pcaScore.isNaN()) sb.append("  PCA: ${score.pcaScore.fmt(0)}/100\n")
        }
        
        return sb.toString()
    }
}
