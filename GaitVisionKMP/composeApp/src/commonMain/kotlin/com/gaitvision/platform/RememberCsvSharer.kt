package com.gaitvision.platform

import androidx.compose.runtime.Composable

/** Returns a platform-specific CsvSharer instance. Must be called from a Composable. */
@Composable
expect fun rememberCsvSharer(): CsvSharer
