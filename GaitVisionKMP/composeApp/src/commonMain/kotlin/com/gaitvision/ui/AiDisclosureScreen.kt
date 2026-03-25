package com.gaitvision.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
    val scope = rememberCoroutineScope()

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
                        text = "This practice uses artificial intelligence (AI) technology to assist in analyzing your gait (walking pattern). The AI system examines video of your movement to measure joint angles, stride length, balance, and other gait parameters.",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Important things to know:",
                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• AI is used as a diagnostic support tool only\n" +
                                "• All AI-generated results will be reviewed by your healthcare practitioner\n" +
                                "• AI results do not replace professional medical judgment\n" +
                                "• Your practitioner will make all final clinical decisions",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "By tapping 'I Acknowledge' below, you confirm that you have been informed about the use of AI in your care.",
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
                    scope.launch {
                        database.aiConsentDao().insertConsent(
                            AiConsentEntity(
                                patientId = patientId,
                                consentGiven = true,
                                consentTimestamp = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                        onConsentGranted()
                    }
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
