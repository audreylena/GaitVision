package com.gaitvision.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gaitvision.data.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToInfo: () -> Unit,
    database: AppDatabase,
    onNavigateToCsvExport: (Long) -> Unit,
    onNavigateToAuditLog: () -> Unit
) {
    var notificationEnabled by remember { mutableStateOf(true) }
    val systemDark = isSystemInDarkTheme()
    var darkThemeEnabled by remember { mutableStateOf(ThemeConfig.isDarkMode ?: systemDark) }
    var csvExportHint by remember { mutableStateOf<String?>(null) }
    val scope = rememberSafeCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                elevation = 4.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Application Preferences",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                SettingSwitchRow(title = "Enable Notifications", checked = notificationEnabled) {
                    notificationEnabled = it
                }
            }

            item {
                SettingSwitchRow(title = "Dark Theme", checked = darkThemeEnabled) {
                    darkThemeEnabled = it
                    ThemeConfig.isDarkMode = it
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingActionRow(
                        title = "Data / CSV",
                        onClick = {
                            scope.launch {
                                val latest = database.gaitScoreDao().getLatestScoreGlobally()
                                if (latest != null) {
                                    csvExportHint = null
                                    onNavigateToCsvExport(latest.id)
                                } else {
                                    csvExportHint = "No analysis data yet — run an analysis first."
                                }
                            }
                        }
                    )
                    csvExportHint?.let { hint ->
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }
                }
            }

            item {
                Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Text(
                    text = "Legal & Compliance (SB 1188)",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Diagnostic Support Tools", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.body2)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This application uses AI for diagnostic support. All scores must be manually reviewed by a clinician. A patient-facing AI disclosure is presented prior to data capture.",
                            style = MaterialTheme.typography.caption,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            item {
                SettingActionRow(title = "HIPAA Audit Log", onClick = onNavigateToAuditLog)
            }

            item {
                Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                Text(
                    text = "Support & About",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                SettingActionRow(title = "Help / FAQ", onClick = onNavigateToHelp)
            }

            item {
                SettingActionRow(title = "App Information", onClick = onNavigateToInfo)
            }
        }
    }
}

@Composable
fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.body1)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.secondary)
            )
        }
    }
}

@Composable
fun SettingActionRow(title: String, onClick: () -> Unit) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.body1)
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}
