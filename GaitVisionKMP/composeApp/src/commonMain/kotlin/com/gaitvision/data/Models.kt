package com.gaitvision.data

import kotlinx.datetime.Clock

data class Patient(
    val id: Long = 0,
    val participantId: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val age: Int? = null,
    /** "Male" or "Female" — required per SB 1188 § 183.007 */
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
    /** Patient's biological sex at time of analysis — SB 1188 § 183.007(a)(2) */
    val biologicalSex: String = ""
)

/**
 * Records that a patient has been informed about and consented to AI-assisted diagnosis.
 * Required per SB 1188 § 183.005(b) — practitioner must disclose AI use to patients.
 */
data class AiConsent(
    val id: Long = 0,
    val patientId: Long = 0,
    val consentGiven: Boolean = false,
    val consentTimestamp: Long = 0
)

/**
 * Records clinician sign-off on an AI-generated gait analysis result.
 * Required per SB 1188 § 183.005(a)(3) — practitioner must review all AI-generated records
 * in a manner consistent with Texas Medical Board standards.
 * The [isReviewed] boolean is the specific field mandated in feat-law-instructions.
 */
data class ClinicianReview(
    val id: Long = 0,
    val gaitScoreId: Long = 0,
    val isReviewed: Boolean = false,
    val reviewTimestamp: Long = 0,
    val notes: String? = null
)

/**
 * HIPAA-required audit trail entry (45 CFR § 164.312(b)).
 * Records every instance of PHI access within the app.
 */
data class AuditLogEntry(
    val id: Long = 0,
    /** One of: VIEW_PATIENT, VIEW_RESULTS, RUN_ANALYSIS, EXPORT_CSV */
    val action: String = "",
    val targetPatientId: Long? = null,
    val targetRecordId: Long? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
