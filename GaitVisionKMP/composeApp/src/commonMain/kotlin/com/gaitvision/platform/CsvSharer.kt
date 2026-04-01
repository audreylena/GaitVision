package com.gaitvision.platform

/**
 * Platform-specific CSV file sharing and saving.
 */
expect class CsvSharer {
    /** Save CSV content to the documents directory. Returns the saved file path or null on failure. */
    fun saveCsv(csvContent: String, filename: String): String?

    /** Open the native share sheet for the given file path. */
    fun shareCsv(filePath: String)
}
