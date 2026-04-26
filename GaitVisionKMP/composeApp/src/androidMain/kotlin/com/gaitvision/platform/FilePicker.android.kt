package com.gaitvision.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

actual class FilePicker actual constructor(
    private val onFilePicked: (String?) -> Unit
) {
    private var videoLauncher: androidx.activity.result.ActivityResultLauncher<String>? = null
    private var csvLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null

    @Composable
    actual fun register() {
        videoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            onFilePicked(uri?.toString())
        }
        csvLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            onFilePicked(uri?.toString())
        }
    }

    actual fun launch() {
        videoLauncher?.launch("video/*")
    }

    actual fun launchCsv() {
        csvLauncher?.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
    }
}
