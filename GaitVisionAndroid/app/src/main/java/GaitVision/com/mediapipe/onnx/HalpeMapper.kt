package GaitVision.com.mediapipe.onnx

import GaitVision.com.gait.GaitConfig

object HalpeMapper {
    val HALPE_TO_MEDIAPIPE: Map<Int, Int> = mapOf(
        0 to 0,
        5 to 11,
        6 to 12,
        7 to 13,
        8 to 14,
        9 to 15,
        10 to 16,
        11 to 23,
        12 to 24,
        13 to 25,
        14 to 26,
        15 to 27,
        16 to 28,
        20 to 31,
        21 to 32,
        24 to 29,
        25 to 30
    )

    data class MappedPose(
        val keypointsNorm33: FloatArray,
        val confidences33: FloatArray
    )

    fun mapToMediaPipe33(
        halpeKeypointsPx: FloatArray,
        halpeScores: FloatArray,
        frameWidth: Int,
        frameHeight: Int
    ): MappedPose {
        require(halpeKeypointsPx.size == 26 * 2)
        require(halpeScores.size == 26)
        val keypoints = FloatArray(33 * 2) { Float.NaN }
        val confidences = FloatArray(33)
        for ((halpeIndex, mediapipeIndex) in HALPE_TO_MEDIAPIPE) {
            val x = halpeKeypointsPx[halpeIndex * 2]
            val y = halpeKeypointsPx[halpeIndex * 2 + 1]
            if (x.isFinite() && y.isFinite()) {
                keypoints[mediapipeIndex * 2] = x / frameWidth.toFloat()
                keypoints[mediapipeIndex * 2 + 1] = y / frameHeight.toFloat()
                confidences[mediapipeIndex] = halpeScores[halpeIndex]
            }
        }
        return MappedPose(keypoints, confidences)
    }

    fun isConfident(score: Float): Boolean =
        score >= GaitConfig.MIN_CONFIDENCE_RTMPOSE
}
