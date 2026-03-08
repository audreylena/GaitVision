package com.gaitvision.platform

import androidx.compose.runtime.Composable
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.UniformTypeIdentifiers.UTTypeVideo
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.Foundation.NSURL
import platform.darwin.NSObject

actual class FilePicker actual constructor(
    private val onFilePicked: (String?) -> Unit
) {
    private val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            onFilePicked(url?.path)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onFilePicked(null)
        }
    }

    @Composable
    actual fun register() {
        // No-op on iOS
    }

    actual fun launch() {
        val types = listOf(UTTypeMovie, UTTypeVideo)
        presentPicker(types)
    }

    actual fun launchCsv() {
        val types = listOf(UTTypeCommaSeparatedText)
        presentPicker(types)
    }

    private fun presentPicker(types: List<Any?>) {
        @Suppress("UNCHECKED_CAST")
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = types as List<platform.UniformTypeIdentifiers.UTType>,
            asCopy = true
        )
        picker.delegate = delegate
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootController?.presentViewController(picker, animated = true, completion = null)
    }
}
