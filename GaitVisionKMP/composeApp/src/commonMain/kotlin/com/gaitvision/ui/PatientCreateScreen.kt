package com.gaitvision.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gaitvision.data.AppDatabase
import com.gaitvision.data.AuditLogger
import com.gaitvision.data.PatientEntity
import kotlinx.coroutines.launch

@Composable
fun PatientCreateScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onPatientCreated: () -> Unit,
    onNavigateToCamera: (Long) -> Unit,
    editingPatientId: Long? = null
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var heightFeet by remember { mutableStateOf("") }
    var heightInches by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var biologicalSex by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberSafeCoroutineScope()

    var loadedPatientForEdit by remember { mutableStateOf<PatientEntity?>(null) }
    var editLoading by remember(editingPatientId) {
        mutableStateOf(editingPatientId != null)
    }
    var editMissing by remember { mutableStateOf(false) }

    LaunchedEffect(editingPatientId) {
        if (editingPatientId == null) {
            loadedPatientForEdit = null
            editLoading = false
            editMissing = false
            firstName = ""
            lastName = ""
            age = ""
            heightFeet = ""
            heightInches = ""
            notes = ""
            biologicalSex = ""
            return@LaunchedEffect
        }
        editLoading = true
        editMissing = false
        val p = database.patientDao().getPatientById(editingPatientId)
        loadedPatientForEdit = p
        if (p == null) {
            editMissing = true
        } else {
            firstName = p.firstName
            lastName = p.lastName
            age = p.age?.toString() ?: ""
            biologicalSex = p.biologicalSex
            heightFeet = (p.height / 12).coerceAtLeast(0).toString()
            heightInches = (p.height % 12).toString()
        }
        editLoading = false
    }

    val isEditMode = editingPatientId != null

    suspend fun persist(andNavigateCamera: Boolean) {
        val totalHeight = (heightFeet.toIntOrNull() ?: 0) * 12 + (heightInches.toIntOrNull() ?: 0)
        val existing = loadedPatientForEdit
        if (existing != null) {
            val updated = existing.copy(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                age = age.toIntOrNull(),
                biologicalSex = biologicalSex,
                height = totalHeight
            )
            database.patientDao().updatePatient(updated)
            AuditLogger.log(database.auditLogDao(), "UPDATE_PATIENT", patientId = existing.id)
            if (andNavigateCamera) onNavigateToCamera(existing.id) else onPatientCreated()
        } else {
            val newId = database.patientDao().insertPatient(
                PatientEntity(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    age = age.toIntOrNull(),
                    biologicalSex = biologicalSex,
                    height = totalHeight,
                    createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                )
            )
            if (andNavigateCamera) onNavigateToCamera(newId) else onPatientCreated()
        }
    }

    fun validate(): Boolean {
        if (firstName.isBlank() || lastName.isBlank() || biologicalSex.isBlank() ||
            heightFeet.isBlank() || heightInches.isBlank()
        ) {
            errorMsg = "Please fill in all required fields"
            return false
        }
        errorMsg = null
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Patient" else "New Patient",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 0.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        when {
            editLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            editMissing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Patient not found.",
                            color = MaterialTheme.colors.onBackground,
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go back")
                        }
                    }
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Card(
                    backgroundColor = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Patient ID", style = MaterialTheme.typography.caption, color = TextSlate)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = loadedPatientForEdit?.let { it.participantId ?: it.id.toString() }
                                ?: "Assigned when saved",
                            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryBlue
                        )
                    }
                }

                Text(
                    "Personal Information",
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colors.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    placeholder = { Text("Enter first name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = Color(0xFF1E1E1E),
                        textColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    placeholder = { Text("Enter last name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = Color(0xFF1E1E1E),
                        textColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age") },
                        placeholder = { Text("Years") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color(0xFF1E1E1E),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight().clickable { biologicalSex = "Male" },
                                color = if (biologicalSex == "Male") PrimaryBlue.copy(alpha = 0.2f) else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (biologicalSex == "Male") PrimaryBlue else Color.Transparent
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Male", color = if (biologicalSex == "Male") PrimaryBlue else Color.White)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight().clickable { biologicalSex = "Female" },
                                color = if (biologicalSex == "Female") PrimaryBlue.copy(alpha = 0.2f) else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (biologicalSex == "Female") PrimaryBlue else Color.Transparent
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Female", color = if (biologicalSex == "Female") PrimaryBlue else Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Height *",
                    style = MaterialTheme.typography.caption,
                    color = TextSlate,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = heightFeet,
                        onValueChange = { heightFeet = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color(0xFF1E1E1E),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(" ft ", color = TextSlate, modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = heightInches,
                        onValueChange = { heightInches = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color(0xFF1E1E1E),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(" in", color = TextSlate, modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add any additional notes about the patient...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = Color(0xFF1E1E1E),
                        textColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    backgroundColor = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!validate()) return@clickable
                                scope.launch {
                                    try {
                                        persist(andNavigateCamera = false)
                                    } catch (e: Exception) {
                                        println("PatientCreateScreen: save failed: ${e.message}")
                                    }
                                }
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                if (isEditMode) "Save Changes" else "Create Patient",
                                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                        Divider(color = MaterialTheme.colors.background)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!validate()) return@clickable
                                scope.launch {
                                    try {
                                        persist(andNavigateCamera = true)
                                    } catch (e: Exception) {
                                        println("PatientCreateScreen: save+analysis failed: ${e.message}")
                                    }
                                }
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                if (isEditMode) "Save & Start Analysis" else "Create & Start Analysis",
                                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
