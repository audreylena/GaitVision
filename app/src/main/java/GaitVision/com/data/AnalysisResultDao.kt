package GaitVision.com.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Lightweight POJO for bulk "how many results per patient?" query */
data class PatientResultCount(val patientId: Int, val count: Int)

@Dao
interface AnalysisResultDao {

    // Create
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: AnalysisResult): Long

    // Read
    @Query("SELECT * FROM results WHERE id = :resultId")
    suspend fun getResultById(resultId: Long): AnalysisResult?

    @Query("SELECT * FROM results WHERE patientId = :patientId ORDER BY recordedAt DESC")
    fun getResultsByPatientIdOrdered(patientId: Int): Flow<List<AnalysisResult>>

    // Delete
    @Query("DELETE FROM results WHERE id = :resultId")
    suspend fun deleteResultById(resultId: Long): Int

    // Utility
    @Query("SELECT COUNT(*) FROM results WHERE patientId = :patientId")
    suspend fun getResultCountForPatient(patientId: Int): Int

    /** Single query to get analysis counts for all patients (avoids N+1) */
    @Query("SELECT patientId, COUNT(*) as count FROM results GROUP BY patientId")
    suspend fun getAllResultCounts(): List<PatientResultCount>
}
