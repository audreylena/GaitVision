package GaitVision.com.ui

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import android.widget.Toast
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import GaitVision.com.R
import GaitVision.com.AnalysisSession
import android.widget.ImageButton
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

class VideoPickerActivity : BaseActivity() {

    private var selectedVideo: Uri? = null

    private lateinit var videoView: VideoView
    private lateinit var tvPlaceholder: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnContinue: LinearLayout
    private lateinit var cardContinue: View
    private lateinit var tvDate: TextView
    private lateinit var btnEditDate: ImageButton

    private var selectedDateMillis: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_picker)

        // Initialize view properties
        videoView = findViewById(R.id.videoView)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)
        tvStatus = findViewById(R.id.tvStatus)
        btnContinue = findViewById(R.id.btnContinue)
        // The parent CardView controls visibility (the LinearLayout is inside it)
        cardContinue = (btnContinue.parent as View)
        tvDate = findViewById(R.id.tvDate)
        btnEditDate = findViewById(R.id.btnEditDate)

        setupCommonHeader("Select Video")
        setupButtons()
    }

    private fun setupButtons() {

        // photo picker api
        val pickVideoLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
                Log.d("VideoPicker", "Selected URI: $uri")

                uri?.let { videoUri ->
                    selectedVideo = videoUri

                    // Show the video view and hide placeholder
                    videoView.visibility = View.VISIBLE
                    tvPlaceholder.visibility = View.GONE
                    cardContinue.visibility = View.VISIBLE

                    // Update status text
                    tvStatus.text = "Video loaded successfully"

                    // Set and start the video
                    videoView.setVideoURI(videoUri)
                    videoView.start()

                    Log.d("VideoPicker", "Video playback started")
                } ?: run {
                    Log.e("VideoPicker", "URI is null - no video selected or permission denied")
                    Toast.makeText(
                        this,
                        "Failed to load video. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    tvStatus.text = "Failed to load video"
                }
            }

        findViewById<LinearLayout>(R.id.btnPick).setOnClickListener {
            // Use PickVisualMedia to select only videos
            val request = PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
                .build()
            pickVideoLauncher.launch(request)
        }

        findViewById<LinearLayout>(R.id.btnRecord).setOnClickListener {
            val recordIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            startActivity(recordIntent)
        }

        // Initialize display date
        updateDateDisplay()

        btnEditDate.setOnClickListener {
            showDatePicker()
        }

        btnContinue.setOnClickListener {
            selectedVideo?.let { uri ->
                AnalysisSession.recordingDate = selectedDateMillis
                val shouldSave = intent.getBooleanExtra(AnalysisActivity.EXTRA_SHOULD_SAVE, true)
                val analysisIntent = Intent(this, AnalysisActivity::class.java).apply {
                    data = uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(AnalysisActivity.EXTRA_SHOULD_SAVE, shouldSave)
                }
                startActivity(analysisIntent)
            } ?: run {
                Toast.makeText(this, "Please select a video first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                    // Normalize to start of day — time of day is not used by this app
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDateMillis = selectedCalendar.timeInMillis
                updateDateDisplay()
            },
            year,
            month,
            dayOfMonth
        )
        // Ensure user can't select future dates
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        if (isToday(selectedDateMillis)) {
            tvDate.text = getString(R.string.label_date_today)
        } else {
            tvDate.text = dateFormat.format(Date(selectedDateMillis))
        }
    }

    private fun isToday(inTimeMillis: Long): Boolean {
        val calendarTarget = Calendar.getInstance().apply { timeInMillis = inTimeMillis }
        val calendarToday = Calendar.getInstance()
        return calendarTarget.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR) &&
               calendarTarget.get(Calendar.DAY_OF_YEAR) == calendarToday.get(Calendar.DAY_OF_YEAR)
    }
}
