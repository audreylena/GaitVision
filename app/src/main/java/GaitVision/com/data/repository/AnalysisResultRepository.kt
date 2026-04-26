package GaitVision.com.data.repository

import GaitVision.com.data.AnalysisResult
import GaitVision.com.data.AnalysisResultDao
import kotlinx.coroutines.flow.Flow

class AnalysisResultRepository(private val analysisResultDao: AnalysisResultDao) {

    // Create
    suspend fun insertResult(result: AnalysisResult): Long {
        return analysisResultDao.insertResult(result)
    }

    // Read
    suspend fun getResultById(resultId: Long): AnalysisResult? {
        return analysisResultDao.getResultById(resultId)
    }

    fun getResultsByPatientIdOrdered(patientId: Int): Flow<List<AnalysisResult>> {
        return analysisResultDao.getResultsByPatientIdOrdered(patientId)
    }

    // Delete
    suspend fun deleteResultById(resultId: Long): Boolean {
        return analysisResultDao.deleteResultById(resultId) > 0
    }

    // Utility
    suspend fun getResultCountForPatient(patientId: Int): Int {
        return analysisResultDao.getResultCountForPatient(patientId)
    }
}
