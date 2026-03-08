package com.gaitvision.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberCsvSharer(): CsvSharer {
    val context = LocalContext.current
    return CsvSharer(context)
}
