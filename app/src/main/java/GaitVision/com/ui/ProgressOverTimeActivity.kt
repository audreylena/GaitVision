package GaitVision.com.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import GaitVision.com.R
import GaitVision.com.data.AnalysisResult
import GaitVision.com.data.AnalysisResultDao
import GaitVision.com.data.AppDatabase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressOverTimeActivity : BaseActivity() {

    // ── Metric descriptor ─────────────────────────────────────────────────────
    data class MetricDef(
        val label: String,
        val unit: String,                   // shown on Y-axis labels
        val goodThreshold: Float? = null,   // dashed green line at this value
        val warnThreshold: Float? = null,   // dashed amber line at this value
        val getValue: (AnalysisResult) -> Float?
    )

    companion object {
        const val EXTRA_PATIENT_ID   = "patientId"
        const val EXTRA_PATIENT_NAME = "patientName"

        private val DATE_FMT = SimpleDateFormat("MM/dd/yy", Locale.getDefault())

        /**
         * All metrics rendered on the progress screen.
         * To add a new gait feature: add one MetricDef here — nothing else changes.
         */
        val METRICS: List<MetricDef> = listOf(
            MetricDef("Overall Score",        "/100", goodThreshold = 80f, warnThreshold = 60f) {
                it.overallScore?.toFloat()
            },
            MetricDef("L Knee ROM",           "°")    { it.kneeLeftRom },
            MetricDef("R Knee ROM",           "°")    { it.kneeRightRom },
            MetricDef("Cadence",              "spm")  { it.cadenceSpm },
            MetricDef("Stride Time",          "s")    { it.strideTimeS },
            MetricDef("Stride Time CV",       "%")    { it.strideTimeCv },
            MetricDef("Step Asymmetry",       "")     { it.stepTimeAsymmetry },
            MetricDef("Stride Length (norm)", "")     { it.strideLengthNorm },
            MetricDef("Trunk Lean σ",         "°")    { it.trunkLeanStdDeg },
            MetricDef("Inter-Ankle CV",       "")     { it.interAnkleCv }
        )

        // Palette — one colour per line (cycles if more metrics than colours)
        // Defined as R.color references so they live in colors.xml
        private val LINE_COLOR_RES = listOf(
            R.color.table_header_text,   // #4FC3F7  cyan-blue
            R.color.score_good,          // #4CAF50  green
            R.color.score_warn,          // #FF9800  amber
            R.color.accent_purple,       // #8B5CF6  purple
            R.color.accent_rose,         // #F43F5E  rose
            R.color.secondary_teal_light,// #22D3EE  teal
            R.color.accent_amber,        // #F59E0B  yellow
            R.color.accent_red,          // #F43F5E  red (reuse)
            R.color.primary_blue_light,  // #60A5FA  light-blue
            R.color.accent_green         // #10B981  emerald
        )
    }

    private lateinit var analysisResultDao: AnalysisResultDao
    private lateinit var chartContainer: LinearLayout
    private var patientIdArg: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress_over_time)

        patientIdArg = intent.getIntExtra(EXTRA_PATIENT_ID, -1)
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Patient"

        if (patientIdArg <= 0) { finish(); return }

        setupCommonHeader("Progress — $patientName")

        analysisResultDao = AppDatabase.getDatabase(this).analysisResultDao()
        chartContainer    = findViewById(R.id.chartContainer)

        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                analysisResultDao.getResultsByPatientIdOrdered(patientIdArg)
                    .collect { results ->
                        renderCharts(results.sortedBy { it.recordedAt })
                    }
            }
        }
    }

    // ── Chart rendering ───────────────────────────────────────────────────────

    private fun renderCharts(analyses: List<AnalysisResult>) {
        chartContainer.removeAllViews()

        METRICS.forEachIndexed { index, metric ->
            val entries: List<Entry> = analyses.mapNotNull { result ->
                metric.getValue(result)?.let { v ->
                    Entry(result.recordedAt.toFloat(), v)
                }
            }
            chartContainer.addView(buildChartCard(metric, entries, index))
        }
    }

    private fun buildChartCard(
        metric: MetricDef,
        entries: List<Entry>,
        colorIndex: Int
    ): CardView {
        val density = resources.displayMetrics.density

        // ── Card wrapper ──────────────────────────────────────────────────────
        val card = CardView(this).apply {
            radius = 12 * density
            setCardBackgroundColor(ContextCompat.getColor(this@ProgressOverTimeActivity, R.color.table_row_odd))
            cardElevation = 4 * density
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (16 * density).toInt() }
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (16 * density).toInt(), (16 * density).toInt(),
                (16 * density).toInt(), (16 * density).toInt()
            )
        }

        // ── Title ─────────────────────────────────────────────────────────────
        inner.addView(TextView(this).apply {
            text = metric.label
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (10 * density).toInt() }
        })

        // ── Chart ─────────────────────────────────────────────────────────────
        val lineColor = ContextCompat.getColor(this, LINE_COLOR_RES[colorIndex % LINE_COLOR_RES.size])
        val chart = buildLineChart(metric, entries, lineColor)
        inner.addView(chart)

        card.addView(inner)
        return card
    }

    private fun buildLineChart(
        metric: MetricDef,
        entries: List<Entry>,
        lineColor: Int
    ): LineChart {
        val density = resources.displayMetrics.density
        val chart = LineChart(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (180 * density).toInt()
            )
        }

        // ── Style ──────────────────────────────────────────────────────────────
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)

            xAxis.apply {
                position    = XAxis.XAxisPosition.BOTTOM
                textColor   = ContextCompat.getColor(this@ProgressOverTimeActivity, R.color.chart_axis_text)
                textSize    = 9f
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) =
                        DATE_FMT.format(Date(value.toLong()))
                }
            }

            axisLeft.apply {
                textColor = ContextCompat.getColor(this@ProgressOverTimeActivity, R.color.chart_axis_text)
                textSize  = 9f
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@ProgressOverTimeActivity, R.color.chart_grid)

                removeAllLimitLines()
                metric.goodThreshold?.let { threshold ->
                    addLimitLine(LimitLine(threshold).apply {
                        lineWidth = 1.5f
                        setLineColor(ContextCompat.getColor(this@ProgressOverTimeActivity, R.color.score_good))
                        enableDashedLine(10f, 6f, 0f)
                    })
                }
                metric.warnThreshold?.let { threshold ->
                    addLimitLine(LimitLine(threshold).apply {
                        lineWidth = 1.5f
                        setLineColor(ContextCompat.getColor(this@ProgressOverTimeActivity, R.color.score_warn))
                        enableDashedLine(10f, 6f, 0f)
                    })
                }
                setDrawLimitLinesBehindData(true)
            }
            axisRight.isEnabled = false
            legend.isEnabled    = false
        }

        // ── Data or no-data state ──────────────────────────────────────────────
        if (entries.size < 2) {
            chart.clear()
            chart.setNoDataText(
                if (entries.isEmpty()) "No data available"
                else                   "Need at least 2 sessions to plot"
            )
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.chart_no_data_text))
            chart.getPaint(com.github.mikephil.charting.charts.Chart.PAINT_INFO).textSize =
                14f * resources.displayMetrics.scaledDensity
        } else {
            val dataSet = LineDataSet(entries, metric.label).apply {
                color = lineColor
                setCircleColor(lineColor)
                lineWidth       = 2.5f
                circleRadius    = 4f
                setDrawCircleHole(true)
                circleHoleColor = ContextCompat.getColor(this@ProgressOverTimeActivity, R.color.table_row_odd)
                valueTextColor  = Color.WHITE
                valueTextSize   = 9f
                setDrawFilled(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val formatted = if (value == value.toLong().toFloat())
                            value.toLong().toString()
                        else String.format("%.1f", value)
                        return if (metric.unit.isNotEmpty()) "$formatted ${metric.unit}" else formatted
                    }
                }
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }

        return chart
    }
}
