package com.gaitvision.platform

import androidx.compose.runtime.Composable

/**
 * Opens the platform multi-video picker (Android: PickMultipleVisualMedia).
 */
expect class MultiVideoPicker {
    @Composable
    fun Register(onVideosSelected: (List<String>) -> Unit)

    fun launchPicker()
}
