package GaitVision.com.data.repository

import GaitVision.com.data.SignalData
import GaitVision.com.data.SignalDataDao

class SignalDataRepository(private val signalDataDao: SignalDataDao) {

    // Create
    suspend fun insertSignalDataList(signalDataList: List<SignalData>): List<Long> {
        return signalDataDao.insertSignalDataList(signalDataList)
    }

    // Read
    suspend fun getSignalDataByResultId(resultId: Long): List<SignalData> {
        return signalDataDao.getSignalDataByResultId(resultId)
    }

    // Delete
    suspend fun deleteSignalDataByResultId(resultId: Long): Boolean {
        return signalDataDao.deleteSignalDataByResultId(resultId) > 0
    }
}
