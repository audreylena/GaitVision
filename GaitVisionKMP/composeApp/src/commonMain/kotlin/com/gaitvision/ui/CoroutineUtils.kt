package com.gaitvision.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberSafeCoroutineScope(): CoroutineScope = rememberCoroutineScope {
    CoroutineExceptionHandler { _, throwable ->
        println("GaitVision: Caught unhandled coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
    }
}
