package GaitVision.com

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Tries to find the actual capture date for a video URI.
// Fallback order:
//   1. MediaStore DATE_TAKEN (most reliable for gallery clips)
//   2. METADATA_KEY_DATE embedded in the container (good for imported files)
//   3. MediaStore DATE_MODIFIED * 1000 (file-system timestamp, last resort)
// Returns null if nothing usable is found; callers decide what to do (today, show "unknown", etc.)
fun extractRecordingDate(context: Context, uri: Uri): Long? {
    // 1. MediaStore - DATE_TAKEN and DATE_MODIFIED in one query
    try {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Video.Media.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val takenIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val modIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                if (takenIdx >= 0) {
                    val taken = cursor.getLong(takenIdx)
                    if (taken > 0) return taken
                }
                // DATE_MODIFIED is seconds since epoch, not millis
                if (modIdx >= 0) {
                    val modified = cursor.getLong(modIdx)
                    if (modified > 0) return modified * 1000L
                }
            }
        }
    } catch (_: Exception) {}

    // 2. Embedded container metadata (works for files not indexed by MediaStore)
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        retriever.release()
        if (!raw.isNullOrBlank()) {
            val parsed = parseMetadataDate(raw)
            if (parsed != null) return parsed
        }
    } catch (_: Exception) {}

    return null
}

// METADATA_KEY_DATE is technically ISO8601 but encoders disagree on the exact format.
// Cover the common variants in order of likelihood.
private val DATE_FORMATS = listOf(
    "yyyyMMdd'T'HHmmss.SSS'Z'",
    "yyyyMMdd'T'HHmmss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd HH:mm:ss",
)

private fun parseMetadataDate(raw: String): Long? {
    for (fmt in DATE_FORMATS) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(raw)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    return null
}

// Shared display-name lookup used by both BatchAnalysisActivity and SessionPersistence.
// Returns empty string on failure; callers fall back however they like.
fun lookupDisplayName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver
            .query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: uri.lastPathSegment
            ?: ""
    } catch (_: Exception) {
        uri.lastPathSegment ?: ""
    }
}
