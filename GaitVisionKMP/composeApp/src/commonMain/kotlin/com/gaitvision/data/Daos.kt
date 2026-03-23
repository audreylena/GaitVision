package com.gaitvision.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────
// Existing entities (updated)
// ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "patients",
    indices = [Index(value = ["participantId"], unique = false)]
)
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val participantId: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val age: Int? = null,
    /** "Male" or "Female" — required per SB 1188 § 183.007 */
    val biologicalSex: String = "",
    val height: Int = 0,
    val createdAt: Long = 0
)

@Entity(
    tableName = "videos",
    indices = [Index(value = ["patientId"], unique = false)],
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long = 0,
    val originalVideoPath: String = "",
    val editedVideoPath: String = "",
    val recordedAt: Long = 0,
    val strideLengthAvg: Double? = null,
    val videoLengthMicroseconds: Long? = null
)

@Entity(
    tableName = "gait_scores",
    indices = [
        Index(value = ["patientId"], unique = false),
        Index(value = ["videoId"], unique = false)
    ],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GaitScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long = 0,
    val videoId: Long = 0,
    val overallScore: Double = 0.0,
    val recordedAt: Long = 0,
    val leftKneeScore: Double? = null,
    val rightKneeScore: Double? = null,
    val leftHipScore: Double? = null,
    val rightHipScore: Double? = null,
    val torsoScore: Double? = null,
    /** Patient's biological sex at time of analysis — SB 1188 § 183.007(a)(2) */
    val biologicalSex: String = ""
)

// ─────────────────────────────────────────────────────────────
// New compliance entities
// ─────────────────────────────────────────────────────────────

/**
 * Tracks patient consent to AI-assisted diagnosis.
 * Required per SB 1188 § 183.005(b).
 */
@Entity(
    tableName = "ai_consents",
    indices = [Index(value = ["patientId"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AiConsentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long = 0,
    val consentGiven: Boolean = false,
    val consentTimestamp: Long = 0
)

/**
 * Tracks clinician review of AI-generated gait scores.
 * Required per SB 1188 § 183.005(a)(3). The [isReviewed] boolean is
 * the specific mandated field per feat-law-instructions.
 */
@Entity(
    tableName = "clinician_reviews",
    indices = [Index(value = ["gaitScoreId"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = GaitScoreEntity::class,
        parentColumns = ["id"],
        childColumns = ["gaitScoreId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ClinicianReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gaitScoreId: Long = 0,
    val isReviewed: Boolean = false,
    val reviewTimestamp: Long = 0,
    val notes: String? = null
)

/**
 * HIPAA audit trail entry — records every PHI access event.
 * Required per 45 CFR § 164.312(b).
 * No FK — standalone table so log entries survive patient deletion.
 */
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** One of: VIEW_PATIENT, VIEW_RESULTS, RUN_ANALYSIS, EXPORT_CSV */
    val action: String = "",
    val targetPatientId: Long? = null,
    val targetRecordId: Long? = null,
    val timestamp: Long = 0
)

// ─────────────────────────────────────────────────────────────
// DAOs
// ─────────────────────────────────────────────────────────────

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatientsFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE patientId = :patientId ORDER BY recordedAt DESC")
    fun getVideosForPatientFlow(patientId: Long): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long
}

@Dao
interface GaitScoreDao {
    @Query("SELECT * FROM gait_scores WHERE patientId = :patientId ORDER BY recordedAt ASC")
    fun getScoresForPatientFlow(patientId: Long): Flow<List<GaitScoreEntity>>

    @Query("SELECT * FROM gait_scores WHERE id = :id")
    suspend fun getScoreById(id: Long): GaitScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: GaitScoreEntity): Long

    @Query("SELECT * FROM gait_scores WHERE patientId = :patientId ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestScoreForPatient(patientId: Long): GaitScoreEntity?

    @Query("SELECT * FROM gait_scores ORDER BY recordedAt DESC")
    fun getAllScoresFlow(): Flow<List<GaitScoreEntity>>
}

@Dao
interface AiConsentDao {
    @Query("SELECT * FROM ai_consents WHERE patientId = :patientId LIMIT 1")
    suspend fun getConsentForPatient(patientId: Long): AiConsentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsent(consent: AiConsentEntity): Long
}

@Dao
interface ClinicianReviewDao {
    @Query("SELECT * FROM clinician_reviews WHERE gaitScoreId = :gaitScoreId LIMIT 1")
    suspend fun getReviewForScore(gaitScoreId: Long): ClinicianReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ClinicianReviewEntity): Long

    @Query("SELECT COUNT(*) FROM clinician_reviews WHERE isReviewed = 0")
    fun getUnreviewedCountFlow(): Flow<Int>
}

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insertLog(entry: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>
}
