package GaitVision.com.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import GaitVision.com.R
import GaitVision.com.AnalysisSession

class QuickAnalysisActivity : BaseActivity() {

    private lateinit var etFeet: EditText
    private lateinit var etInches: EditText
    private lateinit var btnContinue: View
    private lateinit var btnViewResults: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_analysis)

        resetGlobalState()
        initializeViews()
        setupCommonHeader("Quick Analysis")
        setupButtons()
    }

    private fun resetGlobalState() {
        AnalysisSession.reset()
    }

    private fun initializeViews() {
        etFeet = findViewById(R.id.etFeet)
        etInches = findViewById(R.id.etInches)
        btnContinue = findViewById(R.id.btnContinue)
        btnViewResults = findViewById(R.id.btnViewResults)

        etFeet.setText("5")
        etInches.setText("9")
    }

    private fun setupButtons() {
        btnContinue.setOnClickListener {
            if (validateAndSaveInputs()) {
                startActivity(Intent(this, VideoPickerActivity::class.java).apply {
                    putExtra(AnalysisActivity.EXTRA_SHOULD_SAVE, false)
                })
            }
        }

        btnViewResults.setOnClickListener {
            startActivity(Intent(this, ResultsActivity::class.java))
        }
    }

    private fun validateAndSaveInputs(): Boolean {
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

        AnalysisSession.participantId = 0 // Dummy ID for quick analysis
        AnalysisSession.participantHeight = heightInInches

        return true
    }
}
