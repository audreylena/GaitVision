package com.gaitvision.data

import kotlinx.datetime.Clock

data class Patient(
    val id: Long = 0,
    val participantId: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val age: Int? = null,
    val gender: String? = null,
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
    val torsoScore: Double? = null
)
