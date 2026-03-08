package com.gaitvision.data

import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

// Android-specific Room entity wrappers
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

@Database(
    entities = [PatientEntity::class, VideoEntity::class, GaitScoreEntity::class],
    version = 1,
    exportSchema = false
)
actual abstract class AppDatabase : RoomDatabase()
