package com.gaitvision

import androidx.compose.ui.window.ComposeUIViewController
import com.gaitvision.data.getAppDatabase
import com.gaitvision.platform.IOSPoseDetector
import com.gaitvision.platform.IOSVideoProcessor

// Last-resort: log library-internal coroutine exceptions (navigation-compose alpha) instead of crashing on iOS.
@OptIn(ExperimentalStdlibApi::class)
fun MainViewController() = run {
    setUnhandledExceptionHook { throwable ->
        println("GaitVision: Caught unhandled exception: ${throwable::class.simpleName}: ${throwable.message}")
    }
    ComposeUIViewController {
        App(
            poseDetector = IOSPoseDetector(),
            videoProcessor = IOSVideoProcessor(),
            database = getAppDatabase()
        )
    }
}
