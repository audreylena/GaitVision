package GaitVision.com.mediapipe

import GaitVision.com.mediapipe.onnx.BitmapConverters
import GaitVision.com.mediapipe.onnx.RtmPoseEstimator
import GaitVision.com.mediapipe.onnx.YoloxTinyDetector
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.util.Locale

class RtmPoseBackend(
    private val core: RtmPoseBackendCore,
    private val benchmark: RtmPoseBenchmark? = null
) : PoseBackend {

    override fun detectPose(bitmap: Bitmap, frameIdx: Int, timestampMs: Long): PoseFrame? {
        val image = benchmark?.measure(RtmPoseBenchmark.Stage.BITMAP_TO_BGR) {
            BitmapConverters.bitmapToBgr(bitmap)
        } ?: BitmapConverters.bitmapToBgr(bitmap)
        return core.detectPose(
            imageBgr = image.data,
            width = image.width,
            height = image.height,
            frameIdx = frameIdx,
            timestampS = timestampMs / 1000f
        )
    }

    override fun close() {
        try {
            logTrackingSummary(core.trackingSummary())
            core.close()
        } finally {
            benchmark?.let(::logBenchmarkSummary)
        }
    }

    companion object {
        private const val YOLOX_ASSET = "yolox_tiny_8xb8-300e_humanart-6f3252f9.onnx"
        private const val RTMPOSE_ASSET = "rtmpose-s_simcc-body7_pt-body7-halpe26_700e-256x192-7f134165_20230605.onnx"

        fun fromAssets(
            context: Context,
            benchmarkEnabled: Boolean = false,
            trackingConfig: RtmPoseTrackingConfig = RtmPoseTrackingConfig()
        ): RtmPoseBackend {
            val assets = context.applicationContext.assets
            val detector = YoloxTinyDetector(assets.open(YOLOX_ASSET).use { it.readBytes() })
            val estimator = RtmPoseEstimator(assets.open(RTMPOSE_ASSET).use { it.readBytes() })
            val benchmark = if (benchmarkEnabled) RtmPoseBenchmark() else null
            return RtmPoseBackend(
                core = RtmPoseBackendCore(detector, estimator, benchmark, trackingConfig),
                benchmark = benchmark
            )
        }

        private fun logTrackingSummary(summary: RtmPoseBackendCore.TrackingSummary) {
            Log.i(
                "RtmPoseTracking",
                "yolox_interval_frames=${summary.yoloxIntervalFrames} " +
                    "frames=${summary.frames} yolox_runs=${summary.yoloxRuns} " +
                    "scheduled_yolox_runs=${summary.scheduledYoloxRuns} " +
                    "tracked_bbox_frames=${summary.trackedBboxFrames} " +
                    "tracking_failures=${summary.trackingFailures} " +
                    "reacquisition_runs=${summary.reacquisitionRuns} " +
                    "detector_misses=${summary.detectorMisses} " +
                    "detector_miss_fallback_frames=${summary.detectorMissFallbackFrames}"
            )
        }

        private fun logBenchmarkSummary(benchmark: RtmPoseBenchmark) {
            fun format(stage: RtmPoseBenchmark.Stage): String {
                val summary = benchmark.summary(stage)
                return "${stage.name.lowercase(Locale.US)}_count=${summary.count} " +
                    "${stage.name.lowercase(Locale.US)}_mean_ms=${String.format(Locale.US, "%.3f", summary.meanMs)} " +
                    "${stage.name.lowercase(Locale.US)}_p95_ms=${String.format(Locale.US, "%.3f", summary.p95Ms)}"
            }
            Log.i(
                "RtmPoseBench",
                listOf(
                    format(RtmPoseBenchmark.Stage.BITMAP_TO_BGR),
                    format(RtmPoseBenchmark.Stage.YOLOX),
                    format(RtmPoseBenchmark.Stage.RTMPOSE)
                ).joinToString(" ")
            )
        }
    }
}
