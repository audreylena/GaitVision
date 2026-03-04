package GaitVision.com.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import GaitVision.com.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import GaitVision.com.data.*
import GaitVision.com.AnalysisSession
import com.github.mikephil.charting.charts.LineChart
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
    private lateinit var analysisListContainer: LinearLayout
    private lateinit var emptyAnalysisState: View
    private lateinit var progressChart: LineChart
    private val dateFormat = SimpleDateFormat("MMM d, yyyy \u2022 h:mm a", Locale.getDefault())
    private var patientIdArg: Int = -1
    private var currentPatient: Patient? = null

    private var allAnalyses: List<AnalysisResult> = emptyList()
    private var isShowingAllAnalyses = false

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
        analysisListContainer = findViewById(R.id.analysisListContainer)
        emptyAnalysisState = findViewById(R.id.emptyAnalysisState)
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

        val tvViewAll = findViewById<TextView>(R.id.tvViewAll)
        tvViewAll.setOnClickListener {
            isShowingAllAnalyses = !isShowingAllAnalyses
            updateAnalysisListUI()
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
        allAnalyses = analyses
        updateAnalysisListUI()
    }

    private fun updateAnalysisListUI() {
        val tvViewAll = findViewById<TextView>(R.id.tvViewAll)
        analysisListContainer.removeAllViews()

        if (allAnalyses.isEmpty()) {
            analysisListContainer.visibility = View.GONE
            emptyAnalysisState.visibility = View.VISIBLE
            tvViewAll.visibility = View.GONE
        } else {
            analysisListContainer.visibility = View.VISIBLE
            emptyAnalysisState.visibility = View.GONE

            val displayList = if (allAnalyses.size > 1) {
                tvViewAll.visibility = View.VISIBLE
                if (isShowingAllAnalyses) {
                    tvViewAll.text = "Show Less \u2191"
                    allAnalyses
                } else {
                    tvViewAll.text = "View All \u2192"
                    allAnalyses.take(1)
                }
            } else {
                tvViewAll.visibility = View.GONE
                allAnalyses
            }

            for (result in displayList) {
                val cardView = LayoutInflater.from(this)
                    .inflate(R.layout.item_analysis_card, analysisListContainer, false)
                bindAnalysisCard(cardView, result)
                analysisListContainer.addView(cardView)
            }
        }
    }

    private fun bindAnalysisCard(view: View, result: AnalysisResult) {
        view.findViewById<TextView>(R.id.tvDate).text = dateFormat.format(Date(result.recordedAt))

        val score = result.overallScore
        val tvScore = view.findViewById<TextView>(R.id.tvScore)
        val scoreContainer = view.findViewById<View>(R.id.scoreContainer)
        val tvLeftKnee = view.findViewById<TextView>(R.id.tvLeftKnee)
        val tvRightKnee = view.findViewById<TextView>(R.id.tvRightKnee)
        val tvLeftHip = view.findViewById<TextView>(R.id.tvLeftHip)
        val tvRightHip = view.findViewById<TextView>(R.id.tvRightHip)
        val tvTorso = view.findViewById<TextView>(R.id.tvTorso)

        if (score != null) {
            tvScore.text = score.toInt().toString()
            scoreContainer.setBackgroundResource(
                when {
                    score >= 80 -> R.drawable.score_badge_background
                    score >= 60 -> R.drawable.badge_background
                    else -> R.drawable.button_outline_background
                }
            )
            tvLeftKnee.text = result.kneeLeftRom?.let { "${it.toInt()}\u00B0" } ?: "\u2014"
            tvRightKnee.text = result.kneeRightRom?.let { "${it.toInt()}\u00B0" } ?: "\u2014"
            tvLeftHip.text = result.ldjHip?.let { String.format("%.2f", it) } ?: "\u2014"
            tvRightHip.text = "\u2014"
            tvTorso.text = result.trunkLeanStdDeg?.let { "${it.toInt()}\u00B0" } ?: "\u2014"
        } else {
            tvScore.text = "\u2014"
            tvLeftKnee.text = "\u2014"
            tvRightKnee.text = "\u2014"
            tvLeftHip.text = "\u2014"
            tvRightHip.text = "\u2014"
            tvTorso.text = "\u2014"
        }

        view.findViewById<Button>(R.id.btnViewCharts).setOnClickListener {
            val intent = Intent(this, ResultsActivity::class.java)
            intent.putExtra(ResultsActivity.EXTRA_RESULT_ID, result.id)
            startActivity(intent)
        }
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

            // Y-Axis
            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#33FFFFFF")
            }
            axisRight.isEnabled = false
            
            legend.isEnabled = false
        }
    }

    private fun updateProgressChart(analyses: List<AnalysisResult>) {
        val validAnalyses = analyses.filter { it.overallScore != null }.sortedBy { it.recordedAt }
        
        if (validAnalyses.size < 2) {
            // Not enough data to draw a line, hide chart or show empty state
            progressChart.clear()
            progressChart.setNoDataText("Complete at least 2 analyses to see progress")
            progressChart.setNoDataTextColor(Color.GRAY)
            return
        }

        val entries = validAnalyses.map {
            Entry(it.recordedAt.toFloat(), it.overallScore!!.toFloat())
        }

        val dataSet = LineDataSet(entries, "Overall Score").apply {
            color = Color.parseColor("#4FC3F7")
            setCircleColor(Color.parseColor("#4FC3F7"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = Color.parseColor("#252542")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            
            // Format the score text
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        progressChart.data = LineData(dataSet)
        progressChart.invalidate() // refresh
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
