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
import kotlinx.coroutines.launch

@Composable
fun PatientCreateScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit,
    onPatientCreated: () -> Unit,
    onNavigateToCamera: (Long) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var heightFeet by remember { mutableStateOf("") }
    var heightInches by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var biologicalSex by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Patient ID Card (mocked to "1" for creation like screenshot)
            Card(
                backgroundColor = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Patient ID", style = MaterialTheme.typography.caption, color = TextSlate)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
                }
            }

            Text("Personal Information", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colors.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name *") },
                placeholder = { Text("Enter first name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color(0xFF1E1E1E), // Darker gray for inputs
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
                    colors = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = Color(0xFF1E1E1E), unfocusedBorderColor = Color.Transparent),
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (biologicalSex == "Male") PrimaryBlue else Color.Transparent)
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("Male", color = if (biologicalSex == "Male") PrimaryBlue else Color.White) }
                        }
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable { biologicalSex = "Female" },
                            color = if (biologicalSex == "Female") PrimaryBlue.copy(alpha = 0.2f) else Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (biologicalSex == "Female") PrimaryBlue else Color.Transparent)
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("Female", color = if (biologicalSex == "Female") PrimaryBlue else Color.White) }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Height *", style = MaterialTheme.typography.caption, color = TextSlate, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = heightFeet,
                    onValueChange = { heightFeet = it },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = Color(0xFF1E1E1E), unfocusedBorderColor = Color.Transparent),
                    shape = RoundedCornerShape(8.dp)
                )
                Text(" ft ", color = TextSlate, modifier = Modifier.padding(horizontal = 8.dp))
                OutlinedTextField(
                    value = heightInches,
                    onValueChange = { heightInches = it },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = Color(0xFF1E1E1E), unfocusedBorderColor = Color.Transparent),
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

            // Create Buttons Card
            Card(
                backgroundColor = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (firstName.isBlank() || lastName.isBlank() || biologicalSex.isBlank() || heightFeet.isBlank() || heightInches.isBlank()) {
                                errorMsg = "Please fill in all required fields"
                            } else {
                                scope.launch {
                                    val totalHeight = (heightFeet.toIntOrNull() ?: 0) * 12 + (heightInches.toIntOrNull() ?: 0)
                                    database.patientDao().insertPatient(
                                        com.gaitvision.data.PatientEntity(
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            age = age.toIntOrNull(),
                                            biologicalSex = biologicalSex,
                                            height = totalHeight,
                                            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                        )
                                    )
                                    onPatientCreated()
                                }
                            }
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Create Patient", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colors.onSurface)
                    }
                    Divider(color = MaterialTheme.colors.background)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            if (firstName.isBlank() || lastName.isBlank() || biologicalSex.isBlank() || heightFeet.isBlank() || heightInches.isBlank()) {
                                errorMsg = "Please fill in all required fields"
                            } else {
                                scope.launch {
                                    val totalHeight = (heightFeet.toIntOrNull() ?: 0) * 12 + (heightInches.toIntOrNull() ?: 0)
                                    val newPatientId = database.patientDao().insertPatient(
                                        com.gaitvision.data.PatientEntity(
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            age = age.toIntOrNull(),
                                            biologicalSex = biologicalSex,
                                            height = totalHeight,
                                            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                        )
                                    )
                                    onNavigateToCamera(newPatientId)
                                }
                            }
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryPurple)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Create & Start Analysis", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colors.onSurface)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
