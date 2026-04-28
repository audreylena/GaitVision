package GaitVision.com.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import GaitVision.com.AnalysisSession
import GaitVision.com.ProcVidEmpty
import GaitVision.com.R
import GaitVision.com.data.AppDatabase
import GaitVision.com.data.repository.PatientRepository
import GaitVision.com.extractRecordingDate
import GaitVision.com.lookupDisplayName
import GaitVision.com.persistCurrentSession
import GaitVision.com.ui.adapter.BatchVideoAdapter
import GaitVision.com.ui.adapter.BatchVideoRow
import GaitVision.com.ui.adapter.BatchVideoStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BatchAnalysisActivity : BaseActivity() {

    companion object {
        const val EXTRA_PATIENT_ID = "patientId"
    }

    // hero card
    private lateinit var tvPatientName: TextView
    private lateinit var tvPatientId: TextView
    private lateinit var tvSelectedCount: TextView
    private lateinit var tvDoneCount: TextView
    private lateinit var tvFailedCount: TextView
    private lateinit var tvLastScore: TextView

    // action card
    private lateinit var btnPick: LinearLayout
    private lateinit var btnStart: LinearLayout
    private lateinit var btnOverrideDate: LinearLayout
    private lateinit var tvOverrideDateValue: TextView

    // progress card
    private lateinit var cardProgress: View
    private lateinit var tvOverallLabel: TextView
    private lateinit var tvOverallPercent: TextView
    private lateinit var barOverall: ProgressBar
    private lateinit var tvCurrentVideo: TextView
    private lateinit var barCurrent: ProgressBar

    // list + empty state
    private lateinit var tvVideoSectionHeader: TextView
    private lateinit var rvVideos: RecyclerView
    private lateinit var emptyState: View
    private val adapter = BatchVideoAdapter()

    private var patientIdArg: Int = -1
    private var patientHeight: Int = 0
    private val rows: MutableList<BatchVideoRow> = mutableListOf()
    private var isRunning = false
    private var doneCount = 0
    private var failedCount = 0
    private var lastScore: Int? = null
    // null = use per-video metadata dates; set = override all videos with this date
    private var overrideDateMillis: Long? = null
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    private val pickVideos =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult
            val existing = rows.map { it.uri }.toSet()
            val newUris = uris.filter { it !in existing }
            newUris.forEach { uri ->
                rows.add(BatchVideoRow(uri = uri, displayName = lookupName(uri)))
            }
            refreshListUi()
            // Extract metadata dates in background; switch back to Main to write rows
            lifecycleScope.launch(Dispatchers.IO) {
                newUris.forEach { uri ->
                    val date = extractRecordingDate(this@BatchAnalysisActivity, uri)
                    withContext(Dispatchers.Main) {
                        val idx = rows.indexOfFirst { it.uri == uri }
                        if (idx >= 0) {
                            rows[idx] = rows[idx].copy(recordedDateMillis = date)
                            adapter.submitList(rows.toList())
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_analysis)

        patientIdArg = intent.getIntExtra(EXTRA_PATIENT_ID, -1)
        if (patientIdArg <= 0) {
            Toast.makeText(this, "Invalid patient", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        setupCommonHeader("Batch Analysis")
        setCustomBackAction { confirmBackIfRunning() }

        bindViews()
        setupList()
        loadPatient()
    }

    override fun onBackPressed() {
        confirmBackIfRunning()
    }

    private fun bindViews() {
        tvPatientName = findViewById(R.id.tvBatchPatientName)
        tvPatientId = findViewById(R.id.tvBatchPatientId)
        tvSelectedCount = findViewById(R.id.tvBatchSelectedCount)
        tvDoneCount = findViewById(R.id.tvBatchDoneCount)
        tvFailedCount = findViewById(R.id.tvBatchFailedCount)
        tvLastScore = findViewById(R.id.tvBatchLastScore)

        btnPick = findViewById(R.id.btnBatchPick)
        btnStart = findViewById(R.id.btnBatchStart)
        btnOverrideDate = findViewById(R.id.btnOverrideDate)
        tvOverrideDateValue = findViewById(R.id.tvOverrideDateValue)

        cardProgress = findViewById(R.id.cardBatchProgress)
        tvOverallLabel = findViewById(R.id.tvBatchOverallLabel)
        tvOverallPercent = findViewById(R.id.tvBatchOverallPercent)
        barOverall = findViewById(R.id.barBatchOverall)
        tvCurrentVideo = findViewById(R.id.tvBatchCurrentVideo)
        barCurrent = findViewById(R.id.barBatchCurrent)

        tvVideoSectionHeader = findViewById(R.id.tvVideoSectionHeader)
        rvVideos = findViewById(R.id.rvBatchVideos)
        emptyState = findViewById(R.id.batchEmptyState)

        btnPick.setOnClickListener {
            if (isRunning) return@setOnClickListener
            pickVideos.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    .build()
            )
        }
        btnStart.setOnClickListener {
            if (isRunning || rows.none { it.status == BatchVideoStatus.QUEUED }) return@setOnClickListener
            startBatch()
        }
        btnOverrideDate.setOnClickListener {
            if (isRunning) return@setOnClickListener
            showOverrideDatePicker()
        }
    }

    private fun showOverrideDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = overrideDateMillis ?: System.currentTimeMillis()
        }
        val dialog = DatePickerDialog(
            this,
            R.style.Theme_GaitVision_DatePicker,
            { _, year, month, day ->
                overrideDateMillis = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                updateOverrideDateDisplay()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        // "Clear" button resets the override back to metadata mode
        dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear override") { _, _ ->
            overrideDateMillis = null
            updateOverrideDateDisplay()
        }
        dialog.show()
        val white = ContextCompat.getColor(this, R.color.text_white)
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(white)
        dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(white)
        dialog.getButton(DatePickerDialog.BUTTON_NEUTRAL)?.setTextColor(white)
    }

    private fun updateOverrideDateDisplay() {
        if (overrideDateMillis != null) {
            tvOverrideDateValue.text = "Override: ${dateFormat.format(Date(overrideDateMillis!!))}"
            tvOverrideDateValue.setTextColor(ContextCompat.getColor(this, R.color.icon_light_blue))
        } else {
            tvOverrideDateValue.text = "Use metadata dates"
            tvOverrideDateValue.setTextColor(ContextCompat.getColor(this, R.color.chart_axis_text))
        }
    }

    private fun setupList() {
        rvVideos.layoutManager = LinearLayoutManager(this)
        rvVideos.adapter = adapter
        adapter.onDateEditClick = { uri, currentMillis ->
            if (!isRunning) showPerVideoDatePicker(uri, currentMillis)
        }
    }

    private fun showPerVideoDatePicker(uri: Uri, currentMillis: Long?) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentMillis ?: System.currentTimeMillis()
        }
        val dialog = DatePickerDialog(
            this,
            R.style.Theme_GaitVision_DatePicker,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val idx = rows.indexOfFirst { it.uri == uri }
                if (idx >= 0) {
                    rows[idx] = rows[idx].copy(recordedDateMillis = selected)
                    adapter.submitList(rows.toList())
                }
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
        val white = ContextCompat.getColor(this, R.color.text_white)
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(white)
        dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(white)
        dialog.getButton(DatePickerDialog.BUTTON_NEUTRAL)?.setTextColor(white)
    }

    private fun loadPatient() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@BatchAnalysisActivity)
            val patient = withContext(Dispatchers.IO) {
                db.patientDao().getPatientById(patientIdArg)
            }
            if (patient == null) {
                Toast.makeText(this@BatchAnalysisActivity, "Patient not found", Toast.LENGTH_SHORT).show()
                finish(); return@launch
            }
            patientHeight = patient.height
            tvPatientName.text = patient.fullName
            tvPatientId.text = "ID ${patient.participantId ?: "—"}"
        }
    }

    private fun refreshListUi() {
        adapter.submitList(rows.toList())
        tvSelectedCount.text = rows.size.toString()
        val anyQueued = rows.any { it.status == BatchVideoStatus.QUEUED }
        btnStart.alpha = if (anyQueued && !isRunning) 1f else 0.4f
        emptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        if (rows.isEmpty()) {
            tvVideoSectionHeader.visibility = View.GONE
        } else {
            tvVideoSectionHeader.visibility = View.VISIBLE
            tvVideoSectionHeader.text = if (isRunning) {
                val done = rows.count { it.status == BatchVideoStatus.DONE }
                val failed = rows.count { it.status == BatchVideoStatus.FAILED }
                val suffix = buildString {
                    if (done > 0) append("$done done")
                    if (done > 0 && failed > 0) append(", ")
                    if (failed > 0) append("$failed failed")
                }
                if (suffix.isEmpty()) "Running…" else "Running — $suffix"
            } else {
                val n = rows.size
                "$n video${if (n != 1) "s" else ""} selected"
            }
        }
    }

    private fun startBatch() {
        isRunning = true
        cardProgress.visibility = View.VISIBLE
        btnPick.alpha = 0.4f
        btnStart.alpha = 0.4f

        lifecycleScope.launch {
            try {
            val db = AppDatabase.getDatabase(this@BatchAnalysisActivity)
            // make sure patient row exists, mirrors AnalysisActivity
            val patient = withContext(Dispatchers.IO) {
                PatientRepository(db.patientDao())
                    .findOrCreatePatientByParticipantId(patientIdArg, patientHeight)
            }
            val resolvedPatientId = patient.participantId ?: patientIdArg

            val total = rows.size
            for (i in rows.indices) {
                val row = rows[i]
                if (row.status != BatchVideoStatus.QUEUED) continue

                updateRow(i, row.copy(status = BatchVideoStatus.RUNNING))
                tvOverallLabel.text = "Video ${i + 1} of $total"
                val overallPct = (i * 100 / total)
                barOverall.progress = overallPct
                tvOverallPercent.text = "$overallPct%"
                tvCurrentVideo.text = "Processing ${row.displayName}"
                barCurrent.progress = 0

                try {
                    runOneVideo(row, resolvedPatientId, db)
                    val score = AnalysisSession.scoringResult?.getScoreForDatabase()?.toInt()
                    lastScore = score
                    doneCount++
                    updateRow(i, row.copy(status = BatchVideoStatus.DONE, score = score))
                } catch (e: Exception) {
                    Log.e("BatchAnalysis", "video failed: ${row.displayName}", e)
                    failedCount++
                    updateRow(
                        i,
                        row.copy(
                            status = BatchVideoStatus.FAILED,
                            errorMessage = e.message?.take(60) ?: "Failed"
                        )
                    )
                }

                tvDoneCount.text = doneCount.toString()
                tvFailedCount.text = failedCount.toString()
                tvFailedCount.setTextColor(ContextCompat.getColor(
                    this@BatchAnalysisActivity,
                    if (failedCount > 0) R.color.score_poor else R.color.chart_axis_text
                ))
                tvLastScore.text = lastScore?.toString() ?: "—"
                refreshListUi()  // keeps section header in sync ("Running — 2 done")
            }

            // batch finished
            barOverall.progress = 100
            tvOverallPercent.text = "100%"
            tvOverallLabel.text = "Done — $doneCount ok, $failedCount failed"
            tvCurrentVideo.text = ""
            barCurrent.progress = 0
            Toast.makeText(
                this@BatchAnalysisActivity,
                "Batch complete: $doneCount saved, $failedCount failed",
                Toast.LENGTH_LONG
            ).show()
            } finally {
                // always release the running lock, even if something unexpected throws
                isRunning = false
                btnPick.alpha = 1f
                refreshListUi()
            }
        }
    }

    private suspend fun runOneVideo(row: BatchVideoRow, patientId: Int, db: AppDatabase) {
        // reset first so stale results from the previous video can't leak into this one
        AnalysisSession.reset()
        AnalysisSession.galleryUri = row.uri
        AnalysisSession.currentPatientId = patientId
        AnalysisSession.participantId = patientId
        AnalysisSession.participantHeight = patientHeight
        // override wins over metadata; metadata wins over "right now"
        AnalysisSession.recordingDate = overrideDateMillis
            ?: row.recordedDateMillis
            ?: System.currentTimeMillis()

        val outPath = "${cacheDir.absolutePath}/batch_${System.currentTimeMillis()}.mp4"

        AnalysisSession.editedUri = withContext(Dispatchers.IO) {
            File(outPath).takeIf { it.exists() }?.delete()
            ProcVidEmpty(this@BatchAnalysisActivity, outPath) { _, percent ->
                runOnUiThread { barCurrent.progress = percent }
            }
        }

        if (AnalysisSession.extractedFeatures != null) {
            persistCurrentSession(
                context = this@BatchAnalysisActivity,
                db = db,
                patientId = patientId,
                videoFileName = row.displayName,
            )
        } else {
            throw IllegalStateException("no gait features extracted")
        }

        // overlay file not needed in batch mode, clean up now
        withContext(Dispatchers.IO) { File(outPath).takeIf { it.exists() }?.delete() }
    }

    private fun updateRow(index: Int, newRow: BatchVideoRow) {
        rows[index] = newRow
        adapter.submitList(rows.toList())
    }

    private fun lookupName(uri: Uri) =
        lookupDisplayName(this, uri).ifBlank { uri.lastPathSegment ?: "video" }

    private fun confirmBackIfRunning() {
        if (!isRunning) { finish(); return }
        AlertDialog.Builder(this)
            .setTitle("Batch in progress")
            .setMessage("A batch run is still going. Leaving will cancel the remaining videos. Already-saved analyses are kept.")
            .setPositiveButton("Leave") { _, _ -> finish() }
            .setNegativeButton("Stay", null)
            .show()
    }
}
