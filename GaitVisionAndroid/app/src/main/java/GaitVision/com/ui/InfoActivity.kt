package GaitVision.com.ui

import android.os.Bundle
import GaitVision.com.R

class InfoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        setupCommonHeader("Info")
        NavigationHelper.setupBottomNav(this)
    }
}
