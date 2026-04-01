package com.gaitvision.logic

import com.gaitvision.data.GaitScoreEntity
import com.gaitvision.platform.Pose
import com.gaitvision.platform.LandmarkType
import kotlinx.datetime.Clock

/**
 * Main orchestrator for gait analysis.
 * Collects poses from video/camera, builds a PoseSequence, runs FeatureExtractor,
 * and scores with GaitScorer to produce a GaitScoreEntity ready for DB persistence.
 */
class GaitAnalyzer {
    private val poses = mutableListOf<Pose>()
    private val poseTimestamps = mutableListOf<Float>()
    private val startTimeMs = Clock.System.now().toEpochMilliseconds()
    private val scorer = GaitScorer()
    private var scorerInitialized = false
    private val extractor = FeatureExtractor()

    private var videoWidth = 1080
    private var videoHeight = 1920
    private var fps = 30f

    /** Holds the last computed Signals for the Signals Dashboard. */
    var lastSignals: Signals? = null
        private set

    /** Holds the last extracted GaitFeatures. */
    var lastFeatures: GaitFeatures = GaitFeatures.empty()
        private set

    fun setVideoMeta(width: Int, height: Int, fps: Float) {
        this.videoWidth = width
        this.videoHeight = height
        this.fps = fps
    }

    fun addPose(pose: Pose) {
        poses.add(pose)
        val elapsedS = ((Clock.System.now().toEpochMilliseconds() - startTimeMs) / 1000f)
        poseTimestamps.add(elapsedS)
    }

    /**
     * Build a PoseSequence from collected poses and run the full pipeline:
     * 1. FeatureExtractor → GaitFeatures + Signals
     * 2. GaitScorer → ScoringResult
     * 3. Map to GaitScoreEntity ready for Room DB
     *
     * @param patientId Patient to associate the score with.
     * @param videoId   Video entity ID (0 if unknown/live camera).
     */
    fun analyze(patientId: Long = 0L, videoId: Long = 0L): GaitScoreEntity {
        // Lazily initialize scorer
        if (!scorerInitialized) {
            scorerInitialized = scorer.initialize()
        }

        val poseSequence = buildPoseSequence()

        var features: GaitFeatures = GaitFeatures.empty()
        var signals: Signals? = null

        if (poseSequence.frames.size >= 20) {
            val (extractedFeatures, _diagnostics) = extractor.extract(poseSequence)
            if (extractedFeatures != null) {
                features = extractedFeatures
            }
            // Signals are computed internally; we expose them via last call state
            signals = buildSignalsPreview(poseSequence)
        }

        lastFeatures = features
        lastSignals = signals

        val scoringResult = if (scorerInitialized && features.cadence_spm.isFinite()) {
            scorer.score(features)
        } else {
            ScoringResult.default()
        }

        val overallScore = when {
            !scoringResult.aeScore.isNaN() -> scoringResult.aeScore.toDouble()
            !scoringResult.ridgeScore.isNaN() -> scoringResult.ridgeScore.toDouble()
            !scoringResult.pcaScore.isNaN() -> scoringResult.pcaScore.toDouble()
            else -> 0.0
        }

        return GaitScoreEntity(
            patientId = patientId,
            videoId = videoId,
            overallScore = overallScore,
            recordedAt = Clock.System.now().toEpochMilliseconds(),
            leftKneeScore = features.knee_left_rom.toDoubleOrNull(),
            rightKneeScore = features.knee_right_rom.toDoubleOrNull(),
            leftHipScore = features.ldj_hip.toDoubleOrNull(),
            rightHipScore = if (!scoringResult.ridgeScore.isNaN()) scoringResult.ridgeScore.toDouble() else null,
            torsoScore = features.trunk_lean_std_deg.toDoubleOrNull()
        )
    }

    private fun buildPoseSequence(): PoseSequence {
        val frames = poses.mapIndexed { index, pose ->
            val keypoints = Array(33) { FloatArray(2) }
            val confidences = FloatArray(33)

            // Map Pose landmarks → 33-keypoint MediaPipe format using LandmarkType ordinal
            LandmarkType.values().forEachIndexed { i, type ->
                if (i >= 33) return@forEachIndexed
                val lm = pose.getLandmark(type)
                if (lm != null) {
                    keypoints[i][0] = lm.position.x
                    keypoints[i][1] = lm.position.y
                    confidences[i] = lm.visibility
                }
            }

            val ts = poseTimestamps.getOrElse(index) { index.toFloat() / fps }
            PoseFrame(
                frameIdx = index,
                timestampS = ts,
                keypoints = keypoints,
                confidences = confidences,
                isValid = true
            )
        }

        return PoseSequence(
            videoId = "session_${startTimeMs}",
            fps = fps,
            frameWidth = videoWidth,
            frameHeight = videoHeight,
            numFramesTotal = poses.size,
            frames = frames
        )
    }

    /**
     * Build a minimal Signals object from raw pose data for visualization in SignalsDashboard.
     * Uses normalized positions directly (no full signal smoothing pipeline).
     */
    private fun buildSignalsPreview(seq: PoseSequence): Signals? {
        if (seq.frames.isEmpty()) return null
        val n = seq.frames.size
        val ts = FloatArray(n) { seq.frames[it].timestampS }
        val fi = IntArray(n) { seq.frames[it].frameIdx }
        val valid = BooleanArray(n) { seq.frames[it].isValid }

        fun kp(frame: PoseFrame, idx: Int, coord: Int): Float =
            if (frame.confidences.getOrElse(idx) { 0f } > 0.3f) frame.keypoints[idx][coord] else Float.NaN

        val lkAngle = FloatArray(n) { i -> kp(seq.frames[i], 25, 1) } // Left knee Y as proxy
        val rkAngle = FloatArray(n) { i -> kp(seq.frames[i], 26, 1) }
        val trunkAngle = FloatArray(n) { i ->
            val lh = kp(seq.frames[i], 23, 1)
            val rh = kp(seq.frames[i], 24, 1)
            if (!lh.isNaN() && !rh.isNaN()) (lh + rh) / 2f else Float.NaN
        }
        val interAnkle = FloatArray(n) { i ->
            val la = kp(seq.frames[i], 27, 0)
            val ra = kp(seq.frames[i], 28, 0)
            if (!la.isNaN() && !ra.isNaN()) kotlin.math.abs(la - ra) else Float.NaN
        }

        val zeros = FloatArray(n) { 0f }
        return Signals(
            timestamps = ts,
            frameIndices = fi,
            isValid = valid,
            interAnkleDist = interAnkle,
            kneeAngleLeft = lkAngle,
            kneeAngleRight = rkAngle,
            trunkAngle = trunkAngle,
            ankleAngleLeft = zeros,
            ankleAngleRight = zeros,
            hipAngleLeft = zeros,
            hipAngleRight = zeros,
            strideAngle = zeros,
            ankleLeftX = FloatArray(n) { i -> kp(seq.frames[i], 27, 0) },
            ankleRightX = FloatArray(n) { i -> kp(seq.frames[i], 28, 0) },
            ankleLeftY = FloatArray(n) { i -> kp(seq.frames[i], 27, 1) },
            ankleRightY = FloatArray(n) { i -> kp(seq.frames[i], 28, 1) },
            hipLeftY = FloatArray(n) { i -> kp(seq.frames[i], 23, 1) },
            hipRightY = FloatArray(n) { i -> kp(seq.frames[i], 24, 1) },
            ankleLeftVy = zeros,
            ankleRightVy = zeros,
            hipAvgVy = zeros
        )
    }

    fun clear() {
        poses.clear()
        poseTimestamps.clear()
        lastSignals = null
        lastFeatures = GaitFeatures.empty()
    }

    fun release() {
        scorer.release()
        scorerInitialized = false
    }
}

// Extension helpers to safely convert Float to Double (NaN → null)
private fun Float.toDoubleOrNull(): Double? = if (this.isNaN()) null else this.toDouble()
