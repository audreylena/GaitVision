package GaitVision.com.ui

import android.view.View
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

        btnBack?.let { button ->
            if (showBackButton) {
                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    finish()
                }
            } else {
                button.visibility = View.GONE
            }
        }
    }

    protected fun setHeaderSubtitle(subtitle: String) {
        val tv = findViewById<TextView>(R.id.tvSubtitle) ?: return
        tv.text = subtitle
        tv.visibility = if (subtitle.isNotEmpty()) View.VISIBLE else View.GONE
    }

    protected fun setCustomBackAction(action: () -> Unit) {
        btnBack?.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                action()
            }
        }
    }
}