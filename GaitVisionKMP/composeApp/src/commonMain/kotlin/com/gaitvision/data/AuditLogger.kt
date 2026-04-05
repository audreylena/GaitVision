package com.gaitvision.data

import kotlinx.datetime.Clock

/**
 * Utility for inserting HIPAA audit trail entries (45 CFR § 164.312(b)).
 * Call from each screen that accesses PHI.
 */
object AuditLogger {
    suspend fun log(
        dao: AuditLogDao,
        action: String,
        patientId: Long? = null,
        recordId: Long? = null
    ) {
        dao.insertLog(
            AuditLogEntity(
                action = action,
                targetPatientId = patientId,
                targetRecordId = recordId,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
    }
}
