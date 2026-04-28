package com.gaitvision.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gaitvision.data.AiConsentEntity
import com.gaitvision.data.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun AiDisclosureScreen(
    patientId: Long,
    database: AppDatabase,
    onConsentGranted: () -> Unit,
    onDecline: () -> Unit
) {
    val scope = rememberSafeCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Use Disclosure") },
                navigationIcon = {
                    IconButton(onClick = onDecline) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Decline and Back")
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {

            Card(
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Important Information About Your Care",
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pursuant to Texas Health and Safety Code Chapter 183 (Senate Bill 1188) and the Texas Responsible AI Governance Act (House Bill 149), you are being notified that this practice uses an artificial intelligence (AI) system to assist in your care. This technology analyzes video of your movement to measure gait parameters, such as joint angles and balance, and acts as a diagnostic support tool.",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Important Legal and Clinical Disclosures:",
                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• The AI system does not provide isolated medical diagnoses and does not replace professional medical judgment.\n" +
                                "• All AI-generated data is manually reviewed by a licensed healthcare practitioner who makes all final clinical decisions regarding your treatment.\n" +
                                "• Certain demographic information, such as observed biological sex at birth, may be processed by the algorithm in accordance with Texas law to generate baseline comparisons.\n" +
                                "• Your data remains physically stored in the United States and is secured in compliance with State and Federal privacy requirements.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "By tapping 'I Acknowledge' below, you confirm that you have been clearly and conspicuously informed about the use of AI in your health care services.",
                        style = MaterialTheme.typography.body2,
                        color = TextSlate,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (patientId != 0L) {
                        scope.launch {
                            try {
                                database.aiConsentDao().insertConsent(
                                    AiConsentEntity(
                                        patientId = patientId,
                                        consentGiven = true,
                                        consentTimestamp = Clock.System.now().toEpochMilliseconds()
                                    )
                                )
                            } catch (e: Exception) {
                                println("AiDisclosureScreen: consent insert failed: ${e.message}")
                            }
                        }
                    }
                    onConsentGranted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue)
            ) {
                Text("I Acknowledge", color = Color.White, style = MaterialTheme.typography.button)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, contentColor = PrimaryBlue)
            ) {
                Text("Decline")
            }
        }
    }
}
