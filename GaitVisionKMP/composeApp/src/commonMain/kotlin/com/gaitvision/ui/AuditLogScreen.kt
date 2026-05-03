package com.gaitvision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.AuditLogEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun AuditLogScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    val logs by database.auditLogDao().getAllLogsFlow().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ActivityContainerBg)
    ) {
        CommonScreenHeader(title = "HIPAA Audit Log", onBack = onNavigateBack)

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No audit entries recorded yet.",
                    color = AppColors.ChartAxisText,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs, key = { it.id }) { entry ->
                    AuditLogRow(entry)
                }
            }
        }
    }
}

@Composable
private fun AuditLogRow(entry: AuditLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AppColors.CardSurfaceDark,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                formatAuditInstant(entry.timestamp),
                color = AppColors.TableHeaderText,
                fontSize = 11.sp
            )
            Text(
                entry.action,
                color = AppColors.TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            val detail = buildString {
                entry.targetPatientId?.let { append("Patient #$it") }
                entry.targetRecordId?.let {
                    if (isNotEmpty()) append(" · ")
                    append("Record #$it")
                }
            }
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    color = AppColors.ChartAxisText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun formatAuditInstant(epochMs: Long): String = try {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    "${ldt.month.name.take(3)} ${ldt.dayOfMonth}, ${ldt.year}  ${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}:${ldt.second.toString().padStart(2, '0')}"
} catch (_: Exception) {
    "—"
}
