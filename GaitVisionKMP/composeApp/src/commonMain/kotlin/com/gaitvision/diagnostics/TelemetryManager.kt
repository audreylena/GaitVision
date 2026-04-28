package com.gaitvision.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * A data class representing a snapshot of the system's telemetry metrics.
 */
data class TelemetrySnapshot(
    val timestampMs: Long,
    val memoryUsageMb: Float,
    val estimatedCpuLoad: Float,
    val activeCoroutines: Int,
    val analysisQueueSize: Int,
    val isCameraActive: Boolean
)

/**
 * Manages the collection and distribution of application telemetry data.
 * This class abstracts the logic required to monitor KMP memory state and
 * active processing pipelines.
 */
class TelemetryManager {

    private val _telemetryState = MutableStateFlow(
        TelemetrySnapshot(
            timestampMs = Clock.System.now().toEpochMilliseconds(),
            memoryUsageMb = 0f,
            estimatedCpuLoad = 0f,
            activeCoroutines = 0,
            analysisQueueSize = 0,
            isCameraActive = false
        )
    )
    val telemetryState: StateFlow<TelemetrySnapshot> = _telemetryState.asStateFlow()

    private var isTracking = false

    /**
     * Updates the current telemetry snapshot with new simulated or hardware-polled data.
     */
    fun updateMetrics(
        memoryMb: Float? = null,
        cpuLoad: Float? = null,
        coroutines: Int? = null,
        queueSize: Int? = null,
        cameraActive: Boolean? = null
    ) {
        val currentState = _telemetryState.value
        _telemetryState.value = currentState.copy(
            timestampMs = Clock.System.now().toEpochMilliseconds(),
            memoryUsageMb = memoryMb ?: currentState.memoryUsageMb,
            estimatedCpuLoad = cpuLoad ?: currentState.estimatedCpuLoad,
            activeCoroutines = coroutines ?: currentState.activeCoroutines,
            analysisQueueSize = queueSize ?: currentState.analysisQueueSize,
            isCameraActive = cameraActive ?: currentState.isCameraActive
        )
    }

    /**
     * Starts tracking logic. In a full implementation, this would spawn a coroutine
     * to poll hardware APIs across iOS and Android via expect/actual.
     */
    fun startTracking() {
        if (isTracking) return
        isTracking = true
        // Initialize baseline metrics
        updateMetrics(memoryMb = 45.2f, cpuLoad = 12.5f, coroutines = 1, queueSize = 0, cameraActive = false)
    }

    /**
     * Halts telemetry polling to conserve battery life.
     */
    fun stopTracking() {
        isTracking = false
    }
    
    /**
     * Resets the telemetry state to zeroed baselines.
     */
    fun reset() {
        _telemetryState.value = TelemetrySnapshot(
            timestampMs = Clock.System.now().toEpochMilliseconds(),
            memoryUsageMb = 0f,
            estimatedCpuLoad = 0f,
            activeCoroutines = 0,
            analysisQueueSize = 0,
            isCameraActive = false
        )
    }
}
