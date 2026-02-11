package GaitVision.com.ui

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import GaitVision.com.R
import GaitVision.com.galleryUri
import GaitVision.com.participantId
import GaitVision.com.participantHeight
import GaitVision.com.resetAnalysisState

class QuickAnalysisActivity : BaseActivity() {

    private lateinit var videoPreview: ImageView
    private lateinit var etFeet: EditText
    private lateinit var etInches: EditText

    private val videoPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    galleryUri = uri
                }
            }
            updateVideoPreview()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_analysis)

        resetGlobalState()
        initializeViews()
        setupCommonHeader("Quick Analysis")
        setupButtons()
    }


    override fun onResume() {
        super.onResume()
        updateVideoPreview()
    }

    private fun resetGlobalState() {
        resetAnalysisState()
    }

    private fun initializeViews() {
        videoPreview = findViewById(R.id.ivVideoPreview)
        etFeet = findViewById(R.id.etFeet)
        etInches = findViewById(R.id.etInches)
        
        // Set default height values
        etFeet.setText("5")
        etInches.setText("9")
    }


    private fun setupButtons() {
        findViewById<Button>(R.id.btnRecord).setOnClickListener {
            if (validateAndSaveInputs()) {
                val intent = Intent(this, VideoPickerActivity::class.java)
                intent.putExtra("mode", "record")
                intent.putExtra(VideoPickerActivity.EXTRA_RETURN_RESULT, true)
                videoPickerLauncher.launch(intent)
            }
        }

        findViewById<Button>(R.id.btnSelect).setOnClickListener {
            if (validateAndSaveInputs()) {
                val intent = Intent(this, VideoPickerActivity::class.java)
                intent.putExtra("mode", "gallery")
                intent.putExtra(VideoPickerActivity.EXTRA_RETURN_RESULT, true)
                videoPickerLauncher.launch(intent)
            }
        }

        findViewById<Button>(R.id.btnAnalyze).setOnClickListener {
            if (validateForAnalysis()) {
                startActivity(Intent(this, AnalysisActivity::class.java).apply {
                    putExtra(AnalysisActivity.EXTRA_SHOULD_SAVE, false)
                })
            }
        }

        findViewById<Button>(R.id.btnViewResults).setOnClickListener {
            startActivity(Intent(this, ResultsActivity::class.java))
        }
    }

    private fun validateAndSaveInputs(): Boolean {
        // Validate feet and inches using same logic as PatientCreateActivity
        for ((editText, name, max) in listOf(
            Triple(etFeet, "Feet", 8),
            Triple(etInches, "Inches", 11)
        )) {
            val str = editText.text.toString().trim()
            if (str.isEmpty()) {
                editText.error = "$name is required"
                editText.requestFocus()
                return false
            }

            val value = str.toIntOrNull()
            if (value == null || value < 0 || value > max) {
                editText.error = "$name must be between 0 and $max"
                editText.requestFocus()
                return false
            }
        }

        val feet = etFeet.text.toString().trim().toInt()
        val inches = etInches.text.toString().trim().toInt()
        val heightInInches = (feet * 12) + inches

        participantId = 0 // Dummy ID for quick analysis
        participantHeight = heightInInches

        return true
    }

    private fun validateForAnalysis(): Boolean {
        if (!validateAndSaveInputs()) {
            return false
        }

        if (galleryUri == null) {
            Toast.makeText(this, "Please select or record a video first", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun updateVideoPreview() {
        galleryUri?.let { uri ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                videoPreview.setImageBitmap(frame)
            } catch (e: IllegalArgumentException) {
                Log.e("QuickAnalysisActivity", "Invalid video URI or format", e)
            } catch (e: RuntimeException) {
                Log.e("QuickAnalysisActivity", "Error accessing video file", e)
            } finally {
                retriever.release()
            }
        }
    }
}
