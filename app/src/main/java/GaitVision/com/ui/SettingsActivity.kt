package GaitVision.com.ui

import android.os.Bundle
import GaitVision.com.R

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupCommonHeader("Settings")
    }
}
