package com.gaitvision.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        PatientEntity::class,
        VideoEntity::class,
        GaitScoreEntity::class,
        AiConsentEntity::class,
        ClinicianReviewEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun videoDao(): VideoDao
    abstract fun gaitScoreDao(): GaitScoreDao
    abstract fun aiConsentDao(): AiConsentDao
    abstract fun clinicianReviewDao(): ClinicianReviewDao
    abstract fun auditLogDao(): AuditLogDao
}

// The Room compiler generates the `AppDatabase::class.instantiateImpl()` function.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
