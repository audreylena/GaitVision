package com.gaitvision.utils

import kotlinx.datetime.Clock

/**
 * A utility class representing an analytics event for the GaitVision application.
 */
data class AnalyticsEvent(
    val eventName: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val properties: Map<String, String> = emptyMap()
)

/**
 * A central logger for tracking usage events across the application.
 * Currently prints to the console, but can be hooked up to an actual analytics provider in the future.
 */
object AnalyticsEventLogger {

    private val eventHistory = mutableListOf<AnalyticsEvent>()
    
    var isEnabled: Boolean = true

    fun logEvent(eventName: String, properties: Map<String, String> = emptyMap()) {
        if (!isEnabled) return
        
        val event = AnalyticsEvent(eventName = eventName, properties = properties)
        eventHistory.add(event)
        
        println("Analytics Logger: Captured event -> $eventName")
        if (properties.isNotEmpty()) {
            properties.forEach { (key, value) ->
                println("  |-- $key: $value")
            }
        }
    }

    fun logScreenView(screenName: String) {
        logEvent(
            eventName = "screen_viewed",
            properties = mapOf("screen_name" to screenName)
        )
    }

    fun logAnalysisStarted(videoId: String, isCamera: Boolean) {
        logEvent(
            eventName = "analysis_started",
            properties = mapOf(
                "video_id" to videoId,
                "source" to if (isCamera) "camera" else "storage"
            )
        )
    }

    fun logAnalysisCompleted(videoId: String, processingTimeMs: Long, success: Boolean) {
        logEvent(
            eventName = "analysis_completed",
            properties = mapOf(
                "video_id" to videoId,
                "processing_time_ms" to processingTimeMs.toString(),
                "success" to success.toString()
            )
        )
    }

    fun getEventHistory(): List<AnalyticsEvent> {
        return eventHistory.toList()
    }

    fun clearHistory() {
        eventHistory.clear()
    }
}
