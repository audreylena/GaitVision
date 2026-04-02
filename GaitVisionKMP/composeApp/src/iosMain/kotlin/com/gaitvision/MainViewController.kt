package com.gaitvision

import androidx.compose.ui.window.ComposeUIViewController
import com.gaitvision.data.getAppDatabase
import com.gaitvision.platform.IOSPoseDetector
import com.gaitvision.platform.IOSVideoProcessor

fun MainViewController() = ComposeUIViewController {
    App(
        poseDetector = IOSPoseDetector(),
        videoProcessor = IOSVideoProcessor(),
        database = getAppDatabase()
    )
}
