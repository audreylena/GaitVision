package GaitVision.com.mediapipe

import GaitVision.com.gait.GaitConfig
import GaitVision.com.mediapipe.onnx.RtmPoseEstimator

data class RtmPoseTrackingConfig(
    val yoloxIntervalFrames: Int = 3,
    val minKeypointConfidence: Float = GaitConfig.MIN_CONFIDENCE_RTMPOSE,
    val minValidKeypoints: Int = 6,
    val minValidLowerBodyKeypoints: Int = 4
) {
    init {
        require(yoloxIntervalFrames >= 1)
        require(minKeypointConfidence in 0f..1f)
        require(minValidKeypoints >= 2)
        require(minValidLowerBodyKeypoints >= 2)
    }

    companion object {
        val EVERY_FRAME = RtmPoseTrackingConfig(yoloxIntervalFrames = 1)
    }
}

internal class RtmPoseBboxTracker(
    private val config: RtmPoseTrackingConfig
) {
    private var trackedBbox: FloatArray? = null
    private var lastYoloxFrameIdx: Int? = null

    fun shouldRunYolox(frameIdx: Int): Boolean {
        val last = lastYoloxFrameIdx ?: return true
        if (trackedBbox == null || frameIdx <= last) return true
        return frameIdx - last >= config.yoloxIntervalFrames
    }

    fun currentBbox(): FloatArray? = trackedBbox?.copyOf()

    fun onYoloxResult(frameIdx: Int, bbox: FloatArray?) {
        lastYoloxFrameIdx = frameIdx
        if (bbox != null) trackedBbox = bbox.copyOf()
    }

    fun updateFromPose(pose: RtmPoseEstimator.PoseResult): Boolean {
        var validCount = 0
        var validLowerBodyCount = 0
        for (index in TRACKED_HALPE_INDICES) {
            val x = pose.keypointsPx[index * 2]
            val y = pose.keypointsPx[index * 2 + 1]
            if (
                pose.scores[index] < config.minKeypointConfidence ||
                !x.isFinite() || !y.isFinite()
            ) {
                continue
            }
            validCount++
            if (index in LOWER_BODY_HALPE_INDICES) validLowerBodyCount++
        }
        val valid = validCount >= config.minValidKeypoints &&
            validLowerBodyCount >= config.minValidLowerBodyKeypoints
        if (!valid) trackedBbox = null
        return valid
    }

    fun clearTrackedBbox() {
        trackedBbox = null
    }

    private companion object {
        val TRACKED_HALPE_INDICES = intArrayOf(
            0,
            5, 6,
            7, 8,
            9, 10,
            11, 12,
            13, 14,
            15, 16,
            20, 21,
            24, 25
        )
        val LOWER_BODY_HALPE_INDICES = intArrayOf(11, 12, 13, 14, 15, 16, 20, 21, 24, 25)
    }
}
