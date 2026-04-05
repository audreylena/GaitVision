package com.gaitvision.data

import kotlinx.datetime.Clock

data class Patient(
    val id: Long = 0,
    val participantId: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val age: Int? = null,
    val biologicalSex: String = "",
    val height: Int = 0,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    val fullName: String
        get() = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
            "$firstName $lastName".trim()
        } else {
            participantId ?: "Unknown"
        }
}

data class Video(
    val id: Long = 0,
    val patientId: Long = 0,
    val originalVideoPath: String = "",
    val editedVideoPath: String = "",
    val recordedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val strideLengthAvg: Double? = null,
    val videoLengthMicroseconds: Long? = null
)

data class GaitScore(
    val id: Long = 0,
    val patientId: Long = 0,
    val videoId: Long = 0,
    val overallScore: Double = 0.0,
    val recordedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val leftKneeScore: Double? = null,
    val rightKneeScore: Double? = null,
    val leftHipScore: Double? = null,
    val rightHipScore: Double? = null,
    val torsoScore: Double? = null,
    val biologicalSex: String = ""
)

/**
 * Records that a patient has been informed about and consented to AI-assisted diagnosis.
 */
data class AiConsent(
    val id: Long = 0,
    val patientId: Long = 0,
    val consentGiven: Boolean = false,
    val consentTimestamp: Long = 0
)

/**
 * Records clinician sign-off on an AI-generated gait analysis result.
 */
data class ClinicianReview(
    val id: Long = 0,
    val gaitScoreId: Long = 0,
    val isReviewed: Boolean = false,
    val reviewTimestamp: Long = 0,
    val notes: String? = null
)

/**
 * HIPAA audit trail entry records every PHI access event.
 */
data class AuditLogEntry(
    val id: Long = 0,
    /** One of: VIEW_PATIENT, VIEW_RESULTS, RUN_ANALYSIS, EXPORT_CSV */
    val action: String = "",
    val targetPatientId: Long? = null,
    val targetRecordId: Long? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
