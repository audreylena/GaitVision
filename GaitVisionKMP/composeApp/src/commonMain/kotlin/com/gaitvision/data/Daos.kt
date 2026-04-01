package com.gaitvision.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
    val gender: String? = null,
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
    val torsoScore: Double? = null
)

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatientsFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

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
}
