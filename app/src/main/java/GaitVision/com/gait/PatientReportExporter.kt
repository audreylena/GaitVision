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
 * Motivation:
 *   GaitCsvExporter only supports single-session export (one row per call).
 *   Clinicians need longitudinal data — all sessions for a patient in one file
 *   to track progress over time, import into Excel/SPSS, or share with colleagues.
 *
 * Usage:
 *   val exporter = PatientReportExporter(context)
 *   val uri = exporter.exportPatientHistory(patientId, patientName, results)
 *   if (uri != null) exporter.shareReport(uri)
 */
class PatientReportExporter(private val context: Context) {

    companion object {
        private const val TAG = "PatientReportExporter"

        // CSV column headers — one per field in AnalysisResult that is clinically meaningful
        private val HEADERS = listOf(
            "session_number",
            "recorded_at",
            "video_file",
            "quality_flag",
            "valid_stride_count",
            "step_signal_mode",
            "fps_detected",
            "duration_s",
            "num_frames_total",
            "num_frames_valid",
            "valid_frame_rate_pct",
            "num_steps_detected",
            "walking_direction",
            "was_flipped",
            // Scores
            "ae_score",
            "ridge_score",
            "pca_score",
            "overall_score",
            // 16 gait features
            "cadence_spm",
            "stride_time_s",
            "stride_time_cv",
            "step_time_asymmetry",
            "stride_length_norm",
            "stride_amp_norm",
            "step_length_asymmetry",
            "knee_left_rom",
            "knee_right_rom",
            "knee_left_max",
            "knee_right_max",
            "ldj_knee_left",
            "ldj_knee_right",
            "ldj_hip",
            "trunk_lean_std_deg",
            "inter_ankle_cv"
        )

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        private val FILE_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }

    /**
     * Export all analysis results for a patient to a CSV file.
     *
     * Results are sorted oldest-first so session numbers increase chronologically,
     * making longitudinal tracking straightforward in spreadsheet tools.
     *
     * @param patientId   Numeric patient ID (for filename)
     * @param patientName Display name (written into file header comment)
     * @param results     All AnalysisResult rows for this patient
     * @return            FileProvider URI of the exported file, or null on failure
     */
    fun exportPatientHistory(
        patientId: Int,
        patientName: String,
        results: List<AnalysisResult>
    ): Uri? {
        if (results.isEmpty()) {
            Log.w(TAG, "No results to export for patient $patientId")
            return null
        }

        // Sort oldest-first for chronological session numbering
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

    /**
     * Build a share Intent for the exported file so the user can send it
     * via email, Google Drive, WhatsApp, etc.
     */
    fun buildShareIntent(uri: Uri, patientName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "GaitVision Report — $patientName")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the longitudinal gait analysis report for $patientName, " +
                "exported from GaitVision."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun createOutputFile(patientId: Int): File {
        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val timestamp = FILE_DATE_FORMAT.format(Date())
        return File(exportDir, "patient_${patientId}_report_${timestamp}.csv")
    }

    private fun fileToUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Write a human-readable file header as CSV comments (lines starting with #).
     * These are ignored by most CSV parsers but helpful when opened in a text editor.
     */
    private fun writeFileHeader(
        writer: FileWriter,
        patientName: String,
        patientId: Int,
        sessionCount: Int
    ) {
        val now = DATE_FORMAT.format(Date())
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
     * Convert one AnalysisResult into a CSV row.
     * All nullable fields default to empty string (not "NaN") for cleaner
     * spreadsheet handling — blank cells are easier to filter than "NaN" strings.
     */
    private fun writeResultRow(writer: FileWriter, sessionNumber: Int, result: AnalysisResult) {
        val row = buildList {
            add(sessionNumber.toString())
            add(DATE_FORMAT.format(Date(result.recordedAt)))
            add(sanitize(result.videoFileName))
            add(result.qualityFlag ?: "")
            add(result.validStrideCount.toString())
            add(result.stepSignalMode ?: "")
            add(result.fpsDetected?.fmt() ?: "")
            add(result.videoLengthMicroseconds?.let { (it / 1_000_000.0).fmt() } ?: "")
            add(result.numFramesTotal.toString())
            add(result.numFramesValid.toString())
            add(result.validFrameRate?.let { (it * 100).fmt() } ?: "")
            add(result.numStepsDetected.toString())
            add(result.walkingDirection ?: "")
            add(if (result.wasFlipped) "1" else "0")
            // Scores
            add(result.aeScore?.fmt() ?: "")
            add(result.ridgeScore?.fmt() ?: "")
            add(result.pcaScore?.fmt() ?: "")
            add(result.overallScore?.fmt() ?: "")
            // 16 features
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

    /**
     * Write a summary section at the bottom of the file showing averages
     * across all valid sessions. Useful for quick clinical review without
     * needing to open a spreadsheet tool.
     *
     * Only sessions with QualityFlag=OK are included in averages.
     */
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

        // Score trend: compare first vs last valid session
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

    /**
     * Sanitize a value for safe CSV output.
     * Prevents formula injection (=, +, -, @) and handles embedded commas/quotes.
     */
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

    // Format a number to 4 decimal places, stripping unnecessary trailing zeros
    private fun Float.fmt(): String =
        if (this % 1.0f == 0.0f) this.toLong().toString()
        else String.format("%.4f", this).trimEnd('0').trimEnd('.')

    private fun Double.fmt(): String =
        if (this % 1.0 == 0.0) this.toLong().toString()
        else String.format("%.4f", this).trimEnd('0').trimEnd('.')
}