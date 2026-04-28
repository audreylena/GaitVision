package com.gaitvision.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object FormatUtils {

    /**
     * Formats a given timestamp to a readable date string.
     */
    fun formatTimestampToDateString(timestampMillis: Long): String {
        return try {
            val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestampMillis)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${dateTime.year}-${dateTime.monthNumber.toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "Unknown Date"
        }
    }

    /**
     * Formats a duration in milliseconds to a mm:ss format.
     */
    fun formatDuration(durationMillis: Long): String {
        if (durationMillis < 0) return "00:00"
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    /**
     * Formats a score (0.0 to 1.0) into a percentage string.
     */
    fun formatScoreToPercentage(score: Double): String {
        val clamped = score.coerceIn(0.0, 1.0)
        val percentage = (clamped * 100).toInt()
        return "$percentage%"
    }

    /**
     * Formats clinical feature values to 2 decimal places safely.
     */
    fun formatClinicalFeature(value: Double): String {
        // Kotlin multiplatform doesn't have String.format easily available in commonMain,
        // so we manually round to 2 decimal places.
        val rounded = kotlin.math.round(value * 100) / 100.0
        return rounded.toString()
    }

    /**
     * Converts height in inches to a formatted ft/in string.
     */
    fun formatHeightInchesToFeetAndInches(totalInches: Int): String {
        if (totalInches <= 0) return "0' 0\""
        val feet = totalInches / 12
        val inches = totalInches % 12
        return "$feet' $inches\""
    }

    /**
     * Converts a weight in pounds to a formatted string.
     */
    fun formatWeight(weightLbs: Double): String {
        val rounded = kotlin.math.round(weightLbs * 10) / 10.0
        return "$rounded lbs"
    }

    /**
     * Capitalizes the first letter of a given string.
     */
    fun capitalizeFirstLetter(input: String): String {
        if (input.isEmpty()) return input
        return input.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Truncates a string if it exceeds the maximum length.
     */
    fun truncateString(input: String, maxLength: Int): String {
        if (input.length <= maxLength) return input
        return input.substring(0, maxLength) + "..."
    }

    /**
     * Extracts initials from a full name.
     */
    fun extractInitials(fullName: String): String {
        val parts = fullName.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return ""
        val firstInitial = parts.firstOrNull()?.firstOrNull()?.uppercase() ?: ""
        val lastInitial = if (parts.size > 1) parts.lastOrNull()?.firstOrNull()?.uppercase() ?: "" else ""
        return "$firstInitial$lastInitial"
    }
}
