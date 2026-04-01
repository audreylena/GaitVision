package com.gaitvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gaitvision.data.AppDatabase
import com.gaitvision.platform.AndroidPoseDetector
import com.gaitvision.platform.AndroidVideoProcessor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = com.gaitvision.data.getAppDatabase(applicationContext)

        val poseDetector = AndroidPoseDetector()
        val videoProcessor = AndroidVideoProcessor(applicationContext)

        setContent {
            App(
                poseDetector = poseDetector,
                videoProcessor = videoProcessor,
                database = db
            )
        }
    }
}
