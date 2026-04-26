package com.gaitvision.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
actual class CsvSharer {

    actual fun saveCsv(csvContent: String, filename: String): String? {
        return try {
            val docs = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, true
            ).firstOrNull() as? String ?: return null
            val path = "$docs/$filename"
            val nsString = NSString.create(string = csvContent)
            val success = nsString.writeToFile(
                path,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
            if (success) path else null
        } catch (e: Exception) {
            null
        }
    }

    actual fun shareCsv(filePath: String) {
        try {
            val url = NSURL.fileURLWithPath(filePath)
            val activityVC = UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null
            )
            val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootVC?.presentViewController(activityVC, animated = true, completion = null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
