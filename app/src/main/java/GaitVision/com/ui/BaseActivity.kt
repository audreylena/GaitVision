package GaitVision.com.ui

import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import GaitVision.com.R

/**
 * Base activity class that provides common functionality for all activities
 * including back button handling and consistent theming.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val btnBack: ImageButton? by lazy { findViewById(R.id.btnBack) }

    /**
     * Set up the common header with back button and title
     * @param title The title to display in the header
     */
    protected fun setupCommonHeader(title: String) {
        setupCommonHeader(title, true)
    }

    /**
     * Set up the common header with optional back button
     * @param title The title to display in the header
     * @param showBackButton Whether to show the back button
     */
    protected fun setupCommonHeader(title: String, showBackButton: Boolean) {
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvTitle?.text = title

        if (showBackButton) {
            btnBack?.setOnClickListener {
                finish()
            }
        }
    }

    /**
     * custom back button action
     */
    protected fun setCustomBackAction(action: () -> Unit) {
        btnBack?.setOnClickListener {
            action()
        }
    }
}