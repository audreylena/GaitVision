package GaitVision.com.ui

import android.os.Bundle
import GaitVision.com.R

class HelpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        setupCommonHeader("Help/Tutorial")
        NavigationHelper.setupBottomNav(this)
    }
}
