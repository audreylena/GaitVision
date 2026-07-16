package GaitVision.com.mediapipe

import android.graphics.Bitmap

interface PoseBackend {
    /** Full-frame pose detection. Returns null when no person/pose was found. */
    fun detectPose(bitmap: Bitmap, frameIdx: Int, timestampMs: Long): PoseFrame?

    fun close()
}
