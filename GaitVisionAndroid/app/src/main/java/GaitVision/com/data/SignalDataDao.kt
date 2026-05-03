package GaitVision.com.data

import androidx.room.*

@Dao
interface SignalDataDao {

    // Create
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignalDataList(signalDataList: List<SignalData>): List<Long>

    // Read
    @Query("SELECT * FROM signal_data WHERE resultId = :resultId ORDER BY frameNumber ASC")
    suspend fun getSignalDataByResultId(resultId: Long): List<SignalData>

    // Delete
    @Query("DELETE FROM signal_data WHERE resultId = :resultId")
    suspend fun deleteSignalDataByResultId(resultId: Long): Int
}
