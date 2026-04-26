package com.gaitvision.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreVideo.CVPixelBufferRef
import platform.Foundation.NSError
import platform.Vision.VNDetectHumanBodyPoseRequest
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNHumanBodyPoseObservation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IOSPoseDetector : PoseDetector {

    override suspend fun detectPose(image: Any): Pose? {
        val pixelBuffer = image as? CVPixelBufferRef ?: return null

        return suspendCancellableCoroutine { continuation ->
            val request = VNDetectHumanBodyPoseRequest { req, error ->
                if (error != null || req == null) {
                    continuation.resume(null)
                    return@VNDetectHumanBodyPoseRequest
                }
                val observation = req.results?.firstOrNull() as? VNHumanBodyPoseObservation
                continuation.resume(observation?.let { processObservation(it) })
            }

            // The ObjC method: - (BOOL)performRequests:(NSArray *)requests error:(NSError **)error
            // In Kotlin/Native this becomes: performRequests(requests, error)
            // but the binding uses NSErrorPtr. Use memScoped to pass error pointer.
            try {
                memScoped {
                    val errorPtr = alloc<kotlinx.cinterop.ObjCObjectVar<NSError?>>()
                    val handler = VNImageRequestHandler(cVPixelBuffer = pixelBuffer, options = mapOf<Any?, Any>())
                    val success = handler.performRequests(listOf(request), errorPtr.ptr)
                    if (!success) {
                        continuation.resume(null)
                    }
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    private fun processObservation(observation: VNHumanBodyPoseObservation): Pose {
        val landmarks = mutableMapOf<LandmarkType, Landmark>()

        fun mapPoint(vnKey: String, type: LandmarkType) {
            val point = observation.recognizedPointForKey(vnKey, error = null) ?: return
            if (point.confidence < 0.3f) return
            landmarks[type] = Landmark(
                type = type,
                position = Point3D(point.x.toFloat(), 1.0f - point.y.toFloat(), 0f),
                visibility = point.confidence,
                presence = point.confidence
            )
        }

        mapPoint("nose",          LandmarkType.NOSE)
        mapPoint("leftEye",       LandmarkType.LEFT_EYE)
        mapPoint("rightEye",      LandmarkType.RIGHT_EYE)
        mapPoint("leftEar",       LandmarkType.LEFT_EAR)
        mapPoint("rightEar",      LandmarkType.RIGHT_EAR)
        mapPoint("leftShoulder",  LandmarkType.LEFT_SHOULDER)
        mapPoint("rightShoulder", LandmarkType.RIGHT_SHOULDER)
        mapPoint("leftElbow",     LandmarkType.LEFT_ELBOW)
        mapPoint("rightElbow",    LandmarkType.RIGHT_ELBOW)
        mapPoint("leftWrist",     LandmarkType.LEFT_WRIST)
        mapPoint("rightWrist",    LandmarkType.RIGHT_WRIST)
        mapPoint("leftHip",       LandmarkType.LEFT_HIP)
        mapPoint("rightHip",      LandmarkType.RIGHT_HIP)
        mapPoint("leftKnee",      LandmarkType.LEFT_KNEE)
        mapPoint("rightKnee",     LandmarkType.RIGHT_KNEE)
        mapPoint("leftAnkle",     LandmarkType.LEFT_ANKLE)
        mapPoint("rightAnkle",    LandmarkType.RIGHT_ANKLE)

        return Pose(landmarks, 1, 1)
    }
}
