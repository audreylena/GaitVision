package com.gaitvision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.PatientEntity
import kotlinx.datetime.Clock

private enum class PatientListFilter { All, Recent, WithVideos }

private data class PatientSort(val column: SortColumn, val ascending: Boolean)

private enum class SortColumn { Id, FirstName, LastName, Age, Videos }

@Composable
fun PatientListScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onNavigateToCreatePatient: () -> Unit,
    onNavigateToPatientProfile: (Long) -> Unit
) {
    val allPatients by database.patientDao().getAllPatientsFlow().collectAsState(initial = emptyList())
    val allVideos by database.videoDao().getAllVideosFlow().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(PatientListFilter.All) }
    var sort by remember { mutableStateOf(PatientSort(SortColumn.Id, ascending = false)) }

    val videoCounts = remember(allVideos) {
        allVideos.groupingBy { it.patientId }.eachCount()
    }

    val filteredPatients = remember(allPatients, query, filter, sort, videoCounts) {
        applyPatientFilters(allPatients, query, filter, sort, videoCounts)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePatient,
                backgroundColor = AppColors.IconGreen,
                contentColor = AppColors.TextWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Patient")
            }
        },
        backgroundColor = AppColors.ActivityContainerBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.ActivityContainerBg)
        ) {
            CommonScreenHeader(
                title = "Patient Directory",
                onBack = onNavigateBack,
                endContent = {
                    Text(
                        text = "${filteredPatients.size} patient${if (filteredPatients.size != 1) "s" else ""}",
                        color = AppColors.TableHeaderGray,
                        fontSize = 14.sp
                    )
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(AppColors.CardInnerDark, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Search by name or patient ID...",
                            color = AppColors.TableHeaderGray,
                            fontSize = 16.sp
                        )
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        textColor = AppColors.TextWhite,
                        cursorColor = AppColors.PrimaryBlue,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = AppColors.TableHeaderGray)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegacyFilterChip(
                    text = "All",
                    selected = filter == PatientListFilter.All,
                    onClick = { filter = PatientListFilter.All }
                )
                LegacyFilterChip(
                    text = "Recent",
                    selected = filter == PatientListFilter.Recent,
                    onClick = { filter = PatientListFilter.Recent }
                )
                LegacyFilterChip(
                    text = "With Videos",
                    selected = filter == PatientListFilter.WithVideos,
                    onClick = { filter = PatientListFilter.WithVideos }
                )
            }

            PatientTableHeaderRow(
                sort = sort,
                onSortChange = { sort = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AppColors.PatientListBackground)
            ) {
                if (filteredPatients.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = AppColors.TableHeaderGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (query.isNotEmpty()) "No patients match \"$query\"" else "No patients found",
                            fontSize = 18.sp,
                            color = AppColors.TableHeaderGray,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Button(
                            onClick = onNavigateToCreatePatient,
                            modifier = Modifier.padding(top = 24.dp),
                            colors = androidx.compose.material.ButtonDefaults.buttonColors(
                                backgroundColor = AppColors.IconGreen
                            )
                        ) {
                            Text("+ Add First Patient", color = AppColors.TextWhite)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(filteredPatients, key = { it.id }) { patient ->
                            PatientDirectoryDataRow(
                                patient = patient,
                                videoCount = videoCounts[patient.id] ?: 0,
                                onClick = { onNavigateToPatientProfile(patient.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun applyPatientFilters(
    patients: List<PatientEntity>,
    query: String,
    filter: PatientListFilter,
    sort: PatientSort,
    videoCounts: Map<Long, Int>
): List<PatientEntity> {
    val weekMs = 7L * 24 * 60 * 60 * 1000
    val now = Clock.System.now().toEpochMilliseconds()

    var list = patients.asSequence()

    if (query.isNotEmpty()) {
        val q = query.lowercase()
        list = list.filter {
            it.firstName.lowercase().contains(q) ||
                it.lastName.lowercase().contains(q) ||
                (it.participantId?.lowercase()?.contains(q) == true) ||
                it.id.toString().contains(q)
        }
    }

    when (filter) {
        PatientListFilter.All -> { }
        PatientListFilter.Recent -> {
            list = list.filter { it.createdAt >= now - weekMs }
        }
        PatientListFilter.WithVideos -> {
            list = list.filter { (videoCounts[it.id] ?: 0) > 0 }
        }
    }

    val cmp: Comparator<PatientEntity> = when (sort.column) {
        SortColumn.Id -> compareBy(
            { parseParticipantSortKey(it).first },
            { parseParticipantSortKey(it).second }
        )
        SortColumn.FirstName -> compareBy({ it.firstName.lowercase() })
        SortColumn.LastName -> compareBy({ it.lastName.lowercase() })
        SortColumn.Age -> compareBy({ it.age ?: Int.MIN_VALUE })
        SortColumn.Videos -> compareBy({ videoCounts[it.id] ?: 0 })
    }

    val sorted = list.sortedWith(if (sort.ascending) cmp else cmp.reversed())
    return sorted.toList()
}

private fun parseParticipantSortKey(p: PatientEntity): Pair<Int, String> {
    val raw = p.participantId?.trim().orEmpty()
    val num = raw.toIntOrNull()
    return if (num != null) Pair(0, num.toString().padStart(8, '0')) else Pair(1, raw.lowercase())
}

@Composable
private fun PatientTableHeaderRow(
    sort: PatientSort,
    onSortChange: (PatientSort) -> Unit,
    modifier: Modifier = Modifier
) {
    fun arrow(col: SortColumn) =
        if (sort.column == col) if (sort.ascending) " \u2191" else " \u2193" else ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.CardSurfaceDark)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("ID${arrow(SortColumn.Id)}", Modifier.weight(1f)) {
            onSortChange(
                if (sort.column == SortColumn.Id) sort.copy(ascending = !sort.ascending)
                else PatientSort(SortColumn.Id, ascending = false)
            )
        }
        HeaderCell("FIRST${arrow(SortColumn.FirstName)}", Modifier.weight(1f)) {
            onSortChange(
                if (sort.column == SortColumn.FirstName) sort.copy(ascending = !sort.ascending)
                else PatientSort(SortColumn.FirstName, ascending = true)
            )
        }
        HeaderCell("LAST${arrow(SortColumn.LastName)}", Modifier.weight(1f)) {
            onSortChange(
                if (sort.column == SortColumn.LastName) sort.copy(ascending = !sort.ascending)
                else PatientSort(SortColumn.LastName, ascending = true)
            )
        }
        HeaderCell("AGE${arrow(SortColumn.Age)}", Modifier.weight(1f), center = true) {
            onSortChange(
                if (sort.column == SortColumn.Age) sort.copy(ascending = !sort.ascending)
                else PatientSort(SortColumn.Age, ascending = true)
            )
        }
        HeaderCell("VIDEOS${arrow(SortColumn.Videos)}", Modifier.weight(1f), center = true) {
            onSortChange(
                if (sort.column == SortColumn.Videos) sort.copy(ascending = !sort.ascending)
                else PatientSort(SortColumn.Videos, ascending = true)
            )
        }
    }
}

@Composable
private fun HeaderCell(
    label: String,
    modifier: Modifier = Modifier,
    center: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TableHeaderGray
        )
    }
}

/** Mirrors [item_patient_row.xml]. */
@Composable
private fun PatientDirectoryDataRow(
    patient: PatientEntity,
    videoCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = patient.participantId ?: patient.id.toString(),
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.IconLightBlue
        )
        Text(
            text = patient.firstName,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = AppColors.TextWhite,
            maxLines = 1
        )
        Text(
            text = patient.lastName,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = AppColors.TextWhite,
            maxLines = 1
        )
        Text(
            text = patient.age?.toString() ?: "—",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = AppColors.TextWhite
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = videoCount.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.IconGreen
            )
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                tint = AppColors.ChartAxisText,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp)
            )
        }
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "View details",
            tint = AppColors.TextTertiary.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}
