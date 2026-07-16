package GaitVision.com.mediapipe

import kotlin.math.ceil

class RtmPoseBenchmark {
    enum class Stage {
        BITMAP_TO_BGR,
        YOLOX,
        RTMPOSE
    }

    data class StageSummary(
        val count: Int,
        val meanMs: Double,
        val p95Ms: Double
    )

    private val samplesNanos = Stage.entries.associateWith { mutableListOf<Long>() }

    fun <T> measure(stage: Stage, block: () -> T): T {
        val start = System.nanoTime()
        return try {
            block()
        } finally {
            record(stage, System.nanoTime() - start)
        }
    }

    internal fun record(stage: Stage, elapsedNanos: Long) {
        samplesNanos.getValue(stage).add(elapsedNanos.coerceAtLeast(0L))
    }

    fun summary(stage: Stage): StageSummary {
        val samples = samplesNanos.getValue(stage)
        if (samples.isEmpty()) return StageSummary(0, 0.0, 0.0)

        val sorted = samples.sorted()
        val p95Index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(sorted.indices)
        return StageSummary(
            count = samples.size,
            meanMs = samples.average() / NANOS_PER_MILLISECOND,
            p95Ms = sorted[p95Index] / NANOS_PER_MILLISECOND
        )
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}
