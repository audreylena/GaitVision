package com.gaitvision.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

actual class CsvSharer(private val context: Context) {

    actual fun saveCsv(csvContent: String, filename: String): String? {
        return try {
            val dir = context.getExternalFilesDir("csv") ?: context.filesDir
            dir.mkdirs()
            val file = File(dir, filename)
            file.writeText(csvContent)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    actual fun shareCsv(filePath: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
