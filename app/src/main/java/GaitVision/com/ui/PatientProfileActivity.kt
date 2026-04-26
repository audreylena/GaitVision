package GaitVision.com.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import GaitVision.com.R
import GaitVision.com.data.*
import GaitVision.com.AnalysisSession
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PatientProfileActivity : BaseActivity() {

    private lateinit var patientDao: PatientDao
    private lateinit var analysisResultDao: AnalysisResultDao

    // Hero card
    private lateinit var tvPatientId: TextView
    private lateinit var tvPatientName: TextView
    // Demographics
    private lateinit var tvAge: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvCreatedAt: TextView
    // Stats
    private lateinit var tvTotalAnalyses: TextView
    private lateinit var tvLastScore: TextView
    // Chart
    private lateinit var progressChart: LineChart

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
        // Hero card
        tvPatientId       = findViewById(R.id.tvPatientId)
        tvPatientName     = findViewById(R.id.tvPatientName)
        // Demographics
        tvAge             = findViewById(R.id.tvAge)
        tvGender          = findViewById(R.id.tvGender)
        tvHeight          = findViewById(R.id.tvHeight)
        tvCreatedAt       = findViewById(R.id.tvCreatedAt)
        // Stats strip
        tvTotalAnalyses   = findViewById(R.id.tvTotalAnalyses)
        tvLastScore       = findViewById(R.id.tvLastScore)
        // Chart
        progressChart     = findViewById(R.id.progressChart)

        setupChart()

        // Hero: edit / delete now live in the action list
        findViewById<LinearLayout>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, PatientCreateActivity::class.java)
            intent.putExtra("patientId", patientIdArg.toLong())
            startActivity(intent)
        }
        findViewById<LinearLayout>(R.id.btnDelete).setOnClickListener {
            showDeleteConfirmation()
        }

        // Action cards
        findViewById<LinearLayout>(R.id.btnNewAnalysis).setOnClickListener {
            startNewAnalysis()
        }
        findViewById<LinearLayout>(R.id.btnViewHistory).setOnClickListener {
            openAnalysisHistory()
        }
        findViewById<LinearLayout>(R.id.btnViewProgressCard).setOnClickListener {
            openProgressView()
        }


    }

    /**
     * Starts the Flow collection exactly once. repeatOnLifecycle(STARTED) will
     * auto-pause when the activity is stopped and resume when it comes back.
     */
    private fun startObservingData() {
        lifecycleScope.launch {
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

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                analysisResultDao.getResultsByPatientIdOrdered(patientIdArg).collect { results ->
                    updateStats(results)
                }
            }
        }
    }

    private fun displayPatientInfo(patient: Patient) {
        // Plain numeric ID – no letter prefix
        tvPatientId.text = patient.participantId?.toString() ?: "—"

        tvPatientName.text = patient.fullName
        tvAge.text         = if (patient.age != null) "${patient.age} yr" else "—"
        tvGender.text      = patient.gender ?: "—"

        val feet   = patient.height / 12
        val inches = patient.height % 12
        tvHeight.text = "$feet'$inches\""

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        tvCreatedAt.text = dateFormat.format(Date(patient.createdAt))
    }

    private fun updateStats(analyses: List<AnalysisResult>) {
        tvTotalAnalyses.text = analyses.size.toString()

        // Latest overall score
        val latestScore = analyses
            .filter { it.overallScore != null }
            .maxByOrNull { it.recordedAt }
            ?.overallScore

        if (latestScore != null) {
            tvLastScore.text = latestScore.toInt().toString()
            tvLastScore.setTextColor(scoreColor(latestScore))
        } else {
            tvLastScore.text = "—"
            tvLastScore.setTextColor(ContextCompat.getColor(this, R.color.score_none))
        }

        updateProgressChart(analyses)
    }

    private fun scoreColor(score: Double): Int = when {
        score >= 80 -> ContextCompat.getColor(this, R.color.score_good)
        score >= 60 -> ContextCompat.getColor(this, R.color.score_warn)
        else        -> ContextCompat.getColor(this, R.color.score_poor)
    }

    private fun setupChart() {
        progressChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.text_white)
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    private val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String =
                        fmt.format(Date(value.toLong()))
                }
            }

            axisLeft.apply {
                textColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.text_white)
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.chart_grid)
                removeAllLimitLines()
                addLimitLine(LimitLine(80f, "Good").apply {
                    lineWidth = 1.5f
                    lineColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.score_good)
                    enableDashedLine(10f, 6f, 0f)
                    textColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.score_good)
                    textSize = 9f
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                })
                addLimitLine(LimitLine(60f, "Fair").apply {
                    lineWidth = 1.5f
                    lineColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.score_warn)
                    enableDashedLine(10f, 6f, 0f)
                    textColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.score_warn)
                    textSize = 9f
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                })
                setDrawLimitLinesBehindData(true)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun updateProgressChart(analyses: List<AnalysisResult>) {
        val valid = analyses.filter { it.overallScore != null }.sortedBy { it.recordedAt }

        if (valid.size < 2) {
            progressChart.clear()
            progressChart.setNoDataText("Complete at least 2 analyses to see progress")
            progressChart.setNoDataTextColor(Color.GRAY)
            return
        }

        val entries = valid.map { Entry(it.recordedAt.toFloat(), it.overallScore!!.toFloat()) }
        val zoneColor = scoreColor(valid.last().overallScore ?: 0.0)

        val dataSet = LineDataSet(entries, "Overall Score").apply {
            color = zoneColor
            setCircleColor(zoneColor)
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.table_row_odd)
            valueTextColor = ContextCompat.getColor(this@PatientProfileActivity, R.color.text_white)
            valueTextSize = 10f
            setDrawFilled(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = value.toInt().toString()
            }
        }

        progressChart.data = LineData(dataSet)
        progressChart.invalidate()
    }

    private fun openAnalysisHistory() {
        val patient = currentPatient ?: return
        val intent = Intent(this, AnalysisHistoryActivity::class.java)
        intent.putExtra(AnalysisHistoryActivity.EXTRA_PATIENT_ID, patientIdArg)
        intent.putExtra(AnalysisHistoryActivity.EXTRA_PATIENT_NAME, patient.fullName)
        startActivity(intent)
    }

    private fun openProgressView() {
        val patient = currentPatient ?: return
        val intent = Intent(this, ProgressOverTimeActivity::class.java)
        intent.putExtra(ProgressOverTimeActivity.EXTRA_PATIENT_ID, patientIdArg)
        intent.putExtra(ProgressOverTimeActivity.EXTRA_PATIENT_NAME, patient.fullName)
        startActivity(intent)
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
            .setPositiveButton("Delete") { _, _ -> deletePatient() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePatient() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { patientDao.deletePatientById(patientIdArg) }
            Toast.makeText(this@PatientProfileActivity, "Patient deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Data observation is handled in startObservingData() via repeatOnLifecycle
    }
}
