package com.gaitvision.diagnostics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel to bridge the TelemetryManager logic to the Compose UI.
 * Handles the periodic polling and formatting of telemetry data.
 */
class DiagnosticsViewModel {

    private val telemetryManager = TelemetryManager()
    
    // Expose state flow directly to Compose
    val uiState: StateFlow<TelemetrySnapshot> = telemetryManager.telemetryState

    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())
    private var pollingJob: Job? = null

    init {
        telemetryManager.startTracking()
        startPolling()
    }

    /**
     * Simulates background hardware polling by periodically updating the manager.
     */
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                // In a production environment, this would call actual memory/CPU APIs
                val current = telemetryManager.telemetryState.value
                
                // Simulate minor fluctuations
                val memDelta = (-2..5).random() / 10f
                val cpuDelta = (-1..2).random() / 10f
                
                telemetryManager.updateMetrics(
                    memoryMb = maxOf(20f, current.memoryUsageMb + memDelta),
                    cpuLoad = maxOf(0f, current.estimatedCpuLoad + cpuDelta).coerceAtMost(100f)
                )
                
                delay(1000L) // Poll every second
            }
        }
    }

    /**
     * Toggles the state of the camera flag in telemetry.
     */
    fun toggleCameraState(isActive: Boolean) {
        telemetryManager.updateMetrics(cameraActive = isActive)
    }

    /**
     * Simulates the queuing of an analysis task.
     */
    fun incrementAnalysisQueue() {
        val current = telemetryManager.telemetryState.value
        telemetryManager.updateMetrics(queueSize = current.analysisQueueSize + 1)
    }

    /**
     * Simulates the completion of an analysis task.
     */
    fun decrementAnalysisQueue() {
        val current = telemetryManager.telemetryState.value
        if (current.analysisQueueSize > 0) {
            telemetryManager.updateMetrics(queueSize = current.analysisQueueSize - 1)
        }
    }

    /**
     * Cleans up resources when the ViewModel is destroyed.
     */
    fun onCleared() {
        pollingJob?.cancel()
        telemetryManager.stopTracking()
    }
}
