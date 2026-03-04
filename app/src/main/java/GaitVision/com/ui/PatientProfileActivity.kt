package GaitVision.com.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import GaitVision.com.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import GaitVision.com.data.*
import GaitVision.com.AnalysisSession
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PatientProfileActivity : BaseActivity() {

    private lateinit var patientDao: PatientDao
    private lateinit var analysisResultDao: AnalysisResultDao
    
    private lateinit var tvPatientId: TextView
    private lateinit var tvPatientName: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var tvTotalAnalyses: TextView
    private lateinit var tvAvgScore: TextView
    private lateinit var progressChart: LineChart
    private val dateFormat = SimpleDateFormat("MMM d, yyyy \u2022 h:mm a", Locale.getDefault())
    private var patientIdArg: Int = -1
    private var currentPatient: Patient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_profile)

        patientIdArg = intent.getLongExtra("patientId", -1).toInt()
        if (patientIdArg <= 0) {
            Toast.makeText(this, "Invalid patient", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val database = AppDatabase.getDatabase(this)
        patientDao = database.patientDao()
        analysisResultDao = database.analysisResultDao()

        setupCommonHeader("Patient Profile")
        initViews()
        startObservingData()
    }

    private fun initViews() {
        tvPatientId = findViewById(R.id.tvPatientId)
        tvPatientName = findViewById(R.id.tvPatientName)
        tvAge = findViewById(R.id.tvAge)
        tvGender = findViewById(R.id.tvGender)
        tvHeight = findViewById(R.id.tvHeight)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)
        tvTotalAnalyses = findViewById(R.id.tvTotalAnalyses)
        tvAvgScore = findViewById(R.id.tvAvgScore)
        progressChart = findViewById(R.id.progressChart)

        setupChart()

        findViewById<ImageButton>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, PatientCreateActivity::class.java)
            intent.putExtra("patientId", patientIdArg.toLong())
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            showDeleteConfirmation()
        }

        findViewById<ExtendedFloatingActionButton>(R.id.btnNewAnalysis).setOnClickListener {
            startNewAnalysis()
        }

        findViewById<MaterialButton>(R.id.btnViewHistory).setOnClickListener {
            openAnalysisHistory()
        }
    }

    /**
     * Starts the Flow collection exactly once. repeatOnLifecycle(STARTED) will
     * auto-pause when the activity is stopped and resume when it comes back,
     * but we never restart the collector — so isShowingAllAnalyses is preserved.
     */
    private fun startObservingData() {
        lifecycleScope.launch {
            // Load patient info once
            val patient = withContext(Dispatchers.IO) {
                patientDao.getPatientById(patientIdArg)
            }
            if (patient == null) {
                Toast.makeText(this@PatientProfileActivity, "Patient not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            currentPatient = patient
            displayPatientInfo(patient)

            // Observe results — this is the single, permanent collector
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                analysisResultDao.getResultsByPatientIdOrdered(patientIdArg).collect { results ->
                    updateAnalysisList(results)
                    updateStats(results)
                }
            }
        }
    }

    private fun displayPatientInfo(patient: Patient) {
        tvPatientId.text = patient.participantId?.toString() ?: "N/A"
        tvPatientName.text = patient.fullName
        tvAge.text = if (patient.age != null) "${patient.age} years" else "—"
        tvGender.text = patient.gender ?: "—"
        
        val feet = patient.height / 12
        val inches = patient.height % 12
        tvHeight.text = "$feet'$inches\""
        
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        tvCreatedAt.text = "Added: ${dateFormat.format(Date(patient.createdAt))}"
    }

    private fun updateAnalysisList(analyses: List<AnalysisResult>) {
        updateStats(analyses)
    }

    private fun openAnalysisHistory() {
        val patient = currentPatient ?: return
        val intent = Intent(this, AnalysisHistoryActivity::class.java)
        intent.putExtra(AnalysisHistoryActivity.EXTRA_PATIENT_ID, patientIdArg)
        intent.putExtra(AnalysisHistoryActivity.EXTRA_PATIENT_NAME, patient.fullName)
        startActivity(intent)
    }

    private fun updateStats(analyses: List<AnalysisResult>) {
        tvTotalAnalyses.text = analyses.size.toString()
        
        val scores = analyses.mapNotNull { it.overallScore }
        if (scores.isNotEmpty()) {
            val avgScore = scores.average().toInt()
            tvAvgScore.text = avgScore.toString()
        } else {
            tvAvgScore.text = "—"
        }
        
        updateProgressChart(analyses)
    }

    private fun setupChart() {
        progressChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // X-Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    private val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return formatter.format(Date(value.toLong()))
                    }
                }
            }

            // Y-Axis with reference band lines
            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#22FFFFFF")

                // --- Reference band borders ---
                removeAllLimitLines()

                // Upper border: Good zone (≥ 80)
                addLimitLine(LimitLine(80f, "Good").apply {
                    lineWidth = 1.5f
                    lineColor = Color.parseColor("#4CAF50")
                    enableDashedLine(10f, 6f, 0f)
                    textColor = Color.parseColor("#4CAF50")
                    textSize = 9f
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                })

                // Mid border: Fair zone (60–79)
                addLimitLine(LimitLine(60f, "Fair").apply {
                    lineWidth = 1.5f
                    lineColor = Color.parseColor("#FF9800")
                    enableDashedLine(10f, 6f, 0f)
                    textColor = Color.parseColor("#FF9800")
                    textSize = 9f
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                })

                setDrawLimitLinesBehindData(true)   // bands sit behind the score line
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun updateProgressChart(analyses: List<AnalysisResult>) {
        val validAnalyses = analyses.filter { it.overallScore != null }.sortedBy { it.recordedAt }

        if (validAnalyses.size < 2) {
            progressChart.clear()
            progressChart.setNoDataText("Complete at least 2 analyses to see progress")
            progressChart.setNoDataTextColor(Color.GRAY)
            return
        }

        val entries = validAnalyses.map {
            Entry(it.recordedAt.toFloat(), it.overallScore!!.toFloat())
        }

        // Line color reflects the most recent score zone
        val latestScore = validAnalyses.last().overallScore ?: 0.0
        val zoneColor = when {
            latestScore >= 80 -> Color.parseColor("#4CAF50")   // green — Good
            latestScore >= 60 -> Color.parseColor("#FF9800")   // amber — Fair
            else              -> Color.parseColor("#FF5252")   // red   — Needs Attention
        }

        val dataSet = LineDataSet(entries, "Overall Score").apply {
            color = zoneColor
            setCircleColor(zoneColor)
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = Color.parseColor("#252542")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            setDrawFilled(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    value.toInt().toString()
            }
        }

        progressChart.data = LineData(dataSet)
        progressChart.invalidate()
    }

    private fun startNewAnalysis() {
        currentPatient?.let { patient ->
            AnalysisSession.participantId = patient.participantId ?: 0
            AnalysisSession.participantHeight = patient.height
            AnalysisSession.currentPatientId = patient.participantId

            val intent = Intent(this, VideoPickerActivity::class.java)
            intent.putExtra("patientId", patient.participantId)
            intent.putExtra("fromPatientProfile", true)
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Patient")
            .setMessage("Are you sure you want to delete this patient and all their analysis data? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePatient()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePatient() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                patientDao.deletePatientById(patientIdArg)
            }
            Toast.makeText(this@PatientProfileActivity, "Patient deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Nothing to do here — data observation is handled in startObservingData() via repeatOnLifecycle
    }
}
