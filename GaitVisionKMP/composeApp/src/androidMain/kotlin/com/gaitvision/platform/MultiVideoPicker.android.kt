package com.gaitvision.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

actual class MultiVideoPicker {

    private var launcher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>? = null

    @Composable
    actual fun Register(onVideosSelected: (List<String>) -> Unit) {
        launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(50)
        ) { uris ->
            onVideosSelected(uris.map { it.toString() })
        }
    }

    actual fun launchPicker() {
        launcher?.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }
}
