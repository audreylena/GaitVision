package GaitVision.com.gait

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import GaitVision.com.data.AnalysisResult
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PatientReportExporter.kt
 *
 * Exports all analysis sessions for a single patient as a multi-row CSV file.
 *
 * Fixes applied after Gemini code review:
 *   - FileProvider authority corrected to match AndroidManifest.xml
 *   - SimpleDateFormat moved out of companion object (not thread-safe)
 *   - String.format now always uses Locale.US to avoid decimal separator issues
 *   - sanitize() applied consistently to all string fields to prevent CSV injection
 */
class PatientReportExporter(private val context: Context) {

    companion object {
        private const val TAG = "PatientReportExporter"

        private val HEADERS = listOf(
            "session_number", "recorded_at", "video_file", "quality_flag",
            "valid_stride_count", "step_signal_mode", "fps_detected", "duration_s",
            "num_frames_total", "num_frames_valid", "valid_frame_rate_pct",
            "num_steps_detected", "walking_direction", "was_flipped",
            "ae_score", "ridge_score", "pca_score", "overall_score",
            "cadence_spm", "stride_time_s", "stride_time_cv", "step_time_asymmetry",
            "stride_length_norm", "stride_amp_norm", "step_length_asymmetry",
            "knee_left_rom", "knee_right_rom", "knee_left_max", "knee_right_max",
            "ldj_knee_left", "ldj_knee_right", "ldj_hip",
            "trunk_lean_std_deg", "inter_ankle_cv"
        )
    }

    // FIX: SimpleDateFormat is not thread-safe — create per-use, not in companion object
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private fun fileDateFormat() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportPatientHistory(
        patientId: Int,
        patientName: String,
        results: List<AnalysisResult>
    ): Uri? {
        if (results.isEmpty()) {
            Log.w(TAG, "No results to export for patient $patientId")
            return null
        }

        val sorted = results.sortedBy { it.recordedAt }

        return try {
            val file = createOutputFile(patientId)
            FileWriter(file).use { writer ->
                writeFileHeader(writer, patientName, patientId, sorted.size)
                writeColumnHeaders(writer)
                sorted.forEachIndexed { index, result ->
                    writeResultRow(writer, sessionNumber = index + 1, result = result)
                }
                writeSummarySection(writer, sorted)
            }
            Log.d(TAG, "Exported ${sorted.size} sessions for patient $patientId to ${file.name}")
            fileToUri(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export patient history for $patientId: ${e.message}", e)
            null
        }
    }

    fun buildShareIntent(uri: Uri, patientName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "GaitVision Report — $patientName")
            putExtra(Intent.EXTRA_TEXT,
                "Attached is the longitudinal gait analysis report for $patientName, " +
                "exported from GaitVision.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun createOutputFile(patientId: Int): File {
        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val timestamp = fileDateFormat().format(Date())
        return File(exportDir, "patient_${patientId}_report_${timestamp}.csv")
    }

    private fun fileToUri(file: File): Uri {
        // FIX: authority must match AndroidManifest.xml declaration exactly
        return FileProvider.getUriForFile(context, "GaitVision.com.provider", file)
    }

    private fun writeFileHeader(writer: FileWriter, patientName: String, patientId: Int, sessionCount: Int) {
        val now = dateFormat().format(Date())
        writer.write("# GaitVision Patient Longitudinal Report\n")
        writer.write("# Patient: ${sanitize(patientName)} (ID: $patientId)\n")
        writer.write("# Exported: $now\n")
        writer.write("# Sessions: $sessionCount\n")
        writer.write("# Scores: 0=severely impaired, 100=healthy\n")
        writer.write("# AE score is the primary clinical score (used in patient database)\n")
        writer.write("#\n")
    }

    private fun writeColumnHeaders(writer: FileWriter) {
        writer.write(HEADERS.joinToString(","))
        writer.write("\n")
    }

    /**
     * FIX: sanitize() now applied to ALL string fields consistently —
     * qualityFlag, stepSignalMode, and walkingDirection were previously missing it,
     * which could allow CSV formula injection when opened in Excel/Sheets.
     */
    private fun writeResultRow(writer: FileWriter, sessionNumber: Int, result: AnalysisResult) {
        val row = buildList {
            add(sessionNumber.toString())
            add(dateFormat().format(Date(result.recordedAt)))
            add(sanitize(result.videoFileName))
            add(sanitize(result.qualityFlag ?: ""))         // FIX: added sanitize()
            add(result.validStrideCount.toString())
            add(sanitize(result.stepSignalMode ?: ""))      // FIX: added sanitize()
            add(result.fpsDetected?.fmt() ?: "")
            add(result.videoLengthMicroseconds?.let { (it / 1_000_000.0).fmt() } ?: "")
            add(result.numFramesTotal.toString())
            add(result.numFramesValid.toString())
            add(result.validFrameRate?.let { (it * 100).fmt() } ?: "")
            add(result.numStepsDetected.toString())
            add(sanitize(result.walkingDirection ?: ""))    // FIX: added sanitize()
            add(if (result.wasFlipped) "1" else "0")
            add(result.aeScore?.fmt() ?: "")
            add(result.ridgeScore?.fmt() ?: "")
            add(result.pcaScore?.fmt() ?: "")
            add(result.overallScore?.fmt() ?: "")
            add(result.cadenceSpm?.fmt() ?: "")
            add(result.strideTimeS?.fmt() ?: "")
            add(result.strideTimeCv?.fmt() ?: "")
            add(result.stepTimeAsymmetry?.fmt() ?: "")
            add(result.strideLengthNorm?.fmt() ?: "")
            add(result.strideAmpNorm?.fmt() ?: "")
            add(result.stepLengthAsymmetry?.fmt() ?: "")
            add(result.kneeLeftRom?.fmt() ?: "")
            add(result.kneeRightRom?.fmt() ?: "")
            add(result.kneeLeftMax?.fmt() ?: "")
            add(result.kneeRightMax?.fmt() ?: "")
            add(result.ldjKneeLeft?.fmt() ?: "")
            add(result.ldjKneeRight?.fmt() ?: "")
            add(result.ldjHip?.fmt() ?: "")
            add(result.trunkLeanStdDeg?.fmt() ?: "")
            add(result.interAnkleCv?.fmt() ?: "")
        }
        writer.write(row.joinToString(","))
        writer.write("\n")
    }

    private fun writeSummarySection(writer: FileWriter, results: List<AnalysisResult>) {
        val validResults = results.filter { it.qualityFlag == "OK" }
        if (validResults.isEmpty()) return

        writer.write("\n")
        writer.write("# ---- Summary (OK sessions only: ${validResults.size}/${results.size}) ----\n")

        fun avg(selector: (AnalysisResult) -> Float?): String {
            val values = validResults.mapNotNull(selector).filter { !it.isNaN() }
            return if (values.isNotEmpty()) values.average().fmt() else ""
        }

        writer.write("# avg_ae_score,${avg { it.aeScore }}\n")
        writer.write("# avg_cadence_spm,${avg { it.cadenceSpm }}\n")
        writer.write("# avg_stride_time_s,${avg { it.strideTimeS }}\n")
        writer.write("# avg_stride_time_cv,${avg { it.strideTimeCv }}\n")
        writer.write("# avg_step_time_asymmetry,${avg { it.stepTimeAsymmetry }}\n")
        writer.write("# avg_knee_left_rom,${avg { it.kneeLeftRom }}\n")
        writer.write("# avg_knee_right_rom,${avg { it.kneeRightRom }}\n")
        writer.write("# avg_trunk_lean_std_deg,${avg { it.trunkLeanStdDeg }}\n")
        writer.write("# avg_inter_ankle_cv,${avg { it.interAnkleCv }}\n")

        if (validResults.size >= 2) {
            val firstScore = validResults.first().aeScore
            val lastScore = validResults.last().aeScore
            if (firstScore != null && lastScore != null && !firstScore.isNaN() && !lastScore.isNaN()) {
                val delta = lastScore - firstScore
                val trend = when {
                    delta > 5f  -> "improving"
                    delta < -5f -> "declining"
                    else        -> "stable"
                }
                writer.write("# score_trend,$trend (${if (delta >= 0) "+" else ""}${delta.fmt()} points)\n")
            }
        }
    }

    private fun sanitize(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val startsWithFormula = value.firstOrNull()
            ?.let { it == '=' || it == '+' || it == '-' || it == '@' } ?: false
        return when {
            startsWithFormula -> "\"'${value.replace("\"", "\"\"")}\""
            needsQuoting      -> "\"${value.replace("\"", "\"\"")}\""
            else              -> value
        }
    }

    // FIX: Always use Locale.US so decimal separator is always a period.
    // Without this, devices in Germany/France output commas, breaking CSV structure.
    private fun Float.fmt(): String =
        if (this % 1.0f == 0.0f) this.toLong().toString()
        else String.format(Locale.US, "%.4f", this).trimEnd('0').trimEnd('.')

    private fun Double.fmt(): String =
        if (this % 1.0 == 0.0) this.toLong().toString()
        else String.format(Locale.US, "%.4f", this).trimEnd('0').trimEnd('.')
}