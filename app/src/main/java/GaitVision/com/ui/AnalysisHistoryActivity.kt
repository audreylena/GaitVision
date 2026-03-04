package GaitVision.com.ui

import android.content.Intent
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import GaitVision.com.R
import GaitVision.com.data.AnalysisResult
import GaitVision.com.data.AppDatabase
import GaitVision.com.data.AnalysisResultDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Displays all AnalysisResults for a patient as a dynamic, horizontally-scrollable
 * data table.  Columns are driven by reflection over [AnalysisResult] — no hard-coded
 * feature columns.  Adding a new numeric field to [AnalysisResult] automatically
 * surfaces it here without any adapter or layout changes.
 */
class AnalysisHistoryActivity : BaseActivity() {

    // ── Metadata fields we intentionally skip (non-feature data) ──────────────
    private val SKIP_FIELDS = setOf(
        "id", "patientId", "recordedAt", "videoFileName",
        "videoLengthMicroseconds", "stepSignalMode", "qualityFlag",
        "walkingDirection", "wasFlipped", "stridesJson", "selectedStrideIndicesJson"
    )

    // ── Column descriptor ──────────────────────────────────────────────────────
    data class ColumnDef(
        val label: String,
        val minWidthDp: Int = 80,
        val getValue: (AnalysisResult) -> String
    )

    companion object {
        const val EXTRA_PATIENT_ID = "patientId"
        const val EXTRA_PATIENT_NAME = "patientName"
        private val DATE_FMT = SimpleDateFormat("MM/dd/yy  h:mm a", Locale.getDefault())

        /** Convert camelCase property name → human-readable label */
        private fun camelToLabel(name: String): String =
            name.replace(Regex("([A-Z])"), " $1")
                .replaceFirstChar { it.uppercaseChar() }
                .trim()

        /** Format a nullable Number nicely */
        private fun fmt(value: Any?): String = when (value) {
            null -> "—"
            is Float  -> if (value == value.toLong().toFloat()) value.toLong().toString()
                         else String.format("%.2f", value)
            is Double -> if (value == value.toLong().toDouble()) value.toLong().toString()
                         else String.format("%.2f", value)
            is Boolean -> if (value) "Yes" else "No"
            else -> value.toString()
        }
    }

    private lateinit var analysisResultDao: AnalysisResultDao
    private lateinit var rvTable: RecyclerView
    private lateinit var headerRow: LinearLayout
    private lateinit var emptyState: View

    private var patientIdArg: Int = -1
    private lateinit var columns: List<ColumnDef>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_history)

        patientIdArg = intent.getIntExtra(EXTRA_PATIENT_ID, -1)
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Patient"

        if (patientIdArg <= 0) { finish(); return }

        setupCommonHeader("Analysis History — $patientName")

        analysisResultDao = AppDatabase.getDatabase(this).analysisResultDao()

        rvTable    = findViewById(R.id.rvAnalysisTable)
        headerRow  = findViewById(R.id.tableHeaderRow)
        emptyState = findViewById(R.id.emptyState)

        // Build column list dynamically via reflection
        columns = buildColumns()
        buildHeaderRow()

        rvTable.layoutManager = LinearLayoutManager(this)
        rvTable.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        observeData()
    }

    // ── Reflection-based column builder ───────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun buildColumns(): List<ColumnDef> {
        val fixedCols = listOf(
            ColumnDef("Date", minWidthDp = 130) { result ->
                DATE_FMT.format(Date(result.recordedAt))
            },
            ColumnDef("Score", minWidthDp = 72) { result ->
                result.overallScore?.let { "${it.toInt()}/100" } ?: "—"
            }
        )

        // All fields not in SKIP_FIELDS and not already covered by fixedCols
        val skippedByFixed = setOf("overallScore")
        val reflectedCols = AnalysisResult::class.memberProperties
            .filter { prop ->
                prop.name !in SKIP_FIELDS && prop.name !in skippedByFixed
            }
            .sortedBy { it.name }
            .map { prop ->
                val getter = prop as KProperty1<AnalysisResult, *>
                ColumnDef(
                    label = camelToLabel(prop.name),
                    minWidthDp = 90
                ) { result -> fmt(getter.get(result)) }
            }

        return fixedCols + reflectedCols
    }

    // ── Programmatic header row ────────────────────────────────────────────────

    private fun buildHeaderRow() {
        val density = resources.displayMetrics.density
        columns.forEach { col ->
            val tv = TextView(this).apply {
                text = col.label
                setTextColor(ContextCompat.getColor(this@AnalysisHistoryActivity, R.color.table_header_text))
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                val minPx = (col.minWidthDp * density).toInt()
                minWidth = minPx
                setPadding(
                    (12 * density).toInt(), 0,
                    (12 * density).toInt(), 0
                )
            }
            headerRow.addView(tv)
        }
        // "Actions" header at the end
        val actionsLabel = TextView(this).apply {
            text = "Actions"
            setTextColor(ContextCompat.getColor(this@AnalysisHistoryActivity, R.color.table_header_text))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minWidth = (96 * resources.displayMetrics.density).toInt()
            setPadding(
                (12 * resources.displayMetrics.density).toInt(), 0,
                (12 * resources.displayMetrics.density).toInt(), 0
            )
        }
        headerRow.addView(actionsLabel)
    }

    // ── Data observation ───────────────────────────────────────────────────────

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                analysisResultDao.getResultsByPatientIdOrdered(patientIdArg)
                    .collect { results ->
                        if (results.isEmpty()) {
                            rvTable.visibility    = View.GONE
                            headerRow.visibility  = View.GONE
                            emptyState.visibility = View.VISIBLE
                        } else {
                            rvTable.visibility    = View.VISIBLE
                            headerRow.visibility  = View.VISIBLE
                            emptyState.visibility = View.GONE
                            rvTable.adapter = AnalysisTableAdapter(results, columns) { result ->
                                openResults(result)
                            }
                        }
                    }
            }
        }
    }

    private fun openResults(result: AnalysisResult) {
        startActivity(
            Intent(this, ResultsActivity::class.java)
                .putExtra(ResultsActivity.EXTRA_RESULT_ID, result.id)
        )
    }
}
