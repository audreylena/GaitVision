package com.gaitvision.utils

object ValidationUtils {

    /**
     * Validates if a given participant ID meets the expected criteria.
     * Criteria: Alphanumeric and exactly 6 characters long.
     */
    fun isValidParticipantId(id: String): Boolean {
        val trimmed = id.trim()
        if (trimmed.length != 6) return false
        return trimmed.all { it.isLetterOrDigit() }
    }

    /**
     * Validates if height inputs are within human ranges.
     */
    fun isValidHeight(feet: Int, inches: Int): Boolean {
        if (feet < 2 || feet > 8) return false
        if (inches < 0 || inches > 11) return false
        return true
    }

    /**
     * Validates an email address format safely without Android-specific Patterns.
     */
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        return email.matches(emailRegex)
    }

    /**
     * Checks if a provided score is within the valid 0.0 to 100.0 range.
     */
    fun isValidScore(score: Double): Boolean {
        return score in 0.0..100.0
    }

    /**
     * Ensures an input string is safe for file names (no special characters).
     */
    fun isSafeFileName(fileName: String): Boolean {
        val unsafeChars = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
        if (fileName.isEmpty()) return false
        return fileName.none { it in unsafeChars }
    }

    /**
     * Sanitizes an input string to create a safe file name.
     */
    fun sanitizeFileName(fileName: String): String {
        val unsafeChars = "[\\\\/:*?\"<>|]".toRegex()
        return fileName.replace(unsafeChars, "_").trim()
    }
}
