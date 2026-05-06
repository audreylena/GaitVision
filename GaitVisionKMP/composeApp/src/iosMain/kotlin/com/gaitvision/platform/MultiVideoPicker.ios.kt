package com.gaitvision.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.UniformTypeIdentifiers.UTTypeVideo
import platform.darwin.NSObject

/**
 * Batch video selection via the system Files / iCloud document picker (multiple movie files).
 * Matches [FilePicker] patterns so paths are sandbox-safe copy URLs suitable for [IOSVideoProcessor].
 */
actual class MultiVideoPicker actual constructor() {

    private var callback: ((List<String>) -> Unit)? = null

    private val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>
        ) {
            val paths = didPickDocumentsAtURLs.mapNotNull { (it as? NSURL)?.path }
            callback?.invoke(paths)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            // Intentionally no callback — BatchAnalysisScreen treats empty lists as no-op.
        }
    }

    @Composable
    actual fun Register(onVideosSelected: (List<String>) -> Unit) {
        callback = onVideosSelected
    }

    actual fun launchPicker() {
        val types = listOf(UTTypeMovie, UTTypeVideo)
        @Suppress("UNCHECKED_CAST")
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = types as List<platform.UniformTypeIdentifiers.UTType>,
            asCopy = true
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = true
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        // animated: false avoids an iOS bug where multi-select does not appear when presenting with animation.
        rootController?.presentViewController(picker, animated = false, completion = null)
    }
}
