package GaitVision.com.mediapipe

import GaitVision.com.mediapipe.onnx.HalpeMapper
import GaitVision.com.mediapipe.onnx.RtmPoseEstimator
import GaitVision.com.mediapipe.onnx.YoloxTinyDetector

class RtmPoseBackendCore(
    private val detector: YoloxTinyDetector,
    private val estimator: RtmPoseEstimator,
    private val benchmark: RtmPoseBenchmark? = null,
    trackingConfig: RtmPoseTrackingConfig = RtmPoseTrackingConfig()
) : AutoCloseable {
    data class TrackingSummary(
        val yoloxIntervalFrames: Int,
        val frames: Int,
        val yoloxRuns: Int,
        val scheduledYoloxRuns: Int,
        val trackedBboxFrames: Int,
        val trackingFailures: Int,
        val reacquisitionRuns: Int,
        val detectorMisses: Int,
        val detectorMissFallbackFrames: Int
    )

    private val trackingConfig = trackingConfig
    private val bboxTracker = RtmPoseBboxTracker(trackingConfig)
    private var frames = 0
    private var yoloxRuns = 0
    private var scheduledYoloxRuns = 0
    private var trackedBboxFrames = 0
    private var trackingFailures = 0
    private var reacquisitionRuns = 0
    private var detectorMisses = 0
    private var detectorMissFallbackFrames = 0

    fun detectPose(
        imageBgr: IntArray,
        width: Int,
        height: Int,
        frameIdx: Int,
        timestampS: Float
    ): PoseFrame? {
        frames++
        val scheduledYolox = bboxTracker.shouldRunYolox(frameIdx)
        var bbox = if (scheduledYolox) {
            scheduledYoloxRuns++
            val trackedFallback = bboxTracker.currentBbox()
            detectLargestPerson(imageBgr, width, height, frameIdx)
                ?: trackedFallback?.also {
                    trackedBboxFrames++
                    detectorMissFallbackFrames++
                }
        } else {
            trackedBboxFrames++
            bboxTracker.currentBbox()
        } ?: return null

        var pose = estimatePose(imageBgr, width, height, bbox)
        val trackingUpdated = bboxTracker.updateFromPose(pose)
        if (!scheduledYolox && !trackingUpdated) {
            trackingFailures++
            bboxTracker.clearTrackedBbox()
            reacquisitionRuns++
            val reacquiredBbox = detectLargestPerson(imageBgr, width, height, frameIdx)
            if (reacquiredBbox != null) {
                bbox = reacquiredBbox
                pose = estimatePose(imageBgr, width, height, bbox)
                bboxTracker.updateFromPose(pose)
            }
        }

        val mapped = HalpeMapper.mapToMediaPipe33(
            halpeKeypointsPx = pose.keypointsPx,
            halpeScores = pose.scores,
            frameWidth = width,
            frameHeight = height
        )

        return PoseFrame(
            frameIdx = frameIdx,
            timestampS = timestampS,
            keypoints = mapped.keypointsNorm33.toPointArray33(),
            confidences = mapped.confidences33,
            detectionConfidence = mapped.confidences33.averageCoreConfidence(),
            isValid = true
        )
    }

    fun trackingSummary(): TrackingSummary = TrackingSummary(
        yoloxIntervalFrames = trackingConfig.yoloxIntervalFrames,
        frames = frames,
        yoloxRuns = yoloxRuns,
        scheduledYoloxRuns = scheduledYoloxRuns,
        trackedBboxFrames = trackedBboxFrames,
        trackingFailures = trackingFailures,
        reacquisitionRuns = reacquisitionRuns,
        detectorMisses = detectorMisses,
        detectorMissFallbackFrames = detectorMissFallbackFrames
    )

    private fun detectLargestPerson(
        imageBgr: IntArray,
        width: Int,
        height: Int,
        frameIdx: Int
    ): FloatArray? {
        yoloxRuns++
        val detections = benchmark?.measure(RtmPoseBenchmark.Stage.YOLOX) {
            detector.detect(imageBgr, width, height)
        } ?: detector.detect(imageBgr, width, height)
        return detector.largestArea(detections)?.toArray().also { bbox ->
            bboxTracker.onYoloxResult(frameIdx, bbox)
            if (bbox == null) detectorMisses++
        }
    }

    private fun estimatePose(
        imageBgr: IntArray,
        width: Int,
        height: Int,
        bbox: FloatArray
    ): RtmPoseEstimator.PoseResult = benchmark?.measure(RtmPoseBenchmark.Stage.RTMPOSE) {
        estimator.estimate(imageBgr, width, height, bbox)
    } ?: estimator.estimate(imageBgr, width, height, bbox)

    override fun close() {
        detector.close()
        estimator.close()
    }

    private fun FloatArray.toPointArray33(): Array<FloatArray> {
        require(size == 33 * 2)
        return Array(33) { idx ->
            floatArrayOf(this[idx * 2], this[idx * 2 + 1])
        }
    }

    private fun FloatArray.averageCoreConfidence(): Float {
        val core = intArrayOf(
            MediaPipePoseBackend.LEFT_SHOULDER,
            MediaPipePoseBackend.RIGHT_SHOULDER,
            MediaPipePoseBackend.LEFT_HIP,
            MediaPipePoseBackend.RIGHT_HIP,
            MediaPipePoseBackend.LEFT_KNEE,
            MediaPipePoseBackend.RIGHT_KNEE,
            MediaPipePoseBackend.LEFT_ANKLE,
            MediaPipePoseBackend.RIGHT_ANKLE
        )
        return core.map { this[it] }.average().toFloat().coerceIn(0f, 1f)
    }
}
