package GaitVision.com.mediapipe.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class RtmPoseEstimator private constructor(
    private val session: OrtSession,
    private val environment: OrtEnvironment
) : AutoCloseable {
    private val inputName: String = session.inputNames.first()

    constructor(
        modelPath: String,
        environment: OrtEnvironment = OrtEnvironment.getEnvironment()
    ) : this(environment.createSession(modelPath, OrtSession.SessionOptions()), environment)

    constructor(
        modelBytes: ByteArray,
        environment: OrtEnvironment = OrtEnvironment.getEnvironment()
    ) : this(environment.createSession(modelBytes, OrtSession.SessionOptions()), environment)

    data class PreprocessResult(
        val center: FloatArray,
        val scale: FloatArray,
        val warpMatrix: FloatArray,
        val warpedBgr: IntArray,
        val warpedBgrFloat: FloatArray,
        val inputNchw: FloatArray
    )

    data class SimccOutput(
        val simccX: FloatArray,
        val simccXShape: LongArray,
        val simccY: FloatArray,
        val simccYShape: LongArray
    )

    data class PoseResult(
        val keypointsPx: FloatArray,
        val scores: FloatArray
    )

    fun preprocess(imageBgr: IntArray, width: Int, height: Int, bboxXyxy: FloatArray): PreprocessResult {
        require(imageBgr.size == width * height * 3) { "imageBgr must be HWC BGR" }
        require(bboxXyxy.size == 4)
        val centerAndScale = bboxXyxy2cs(bboxXyxy, padding = 1.25f)
        val center = centerAndScale.first
        val scale = fitScaleToAspectRatio(centerAndScale.second)
        val warpMatrix = getWarpMatrix(center, scale, OUTPUT_WIDTH, OUTPUT_HEIGHT)
        val warped = warpAffineBilinear(
            src = imageBgr,
            srcWidth = width,
            srcHeight = height,
            matrix = warpMatrix,
            dstWidth = OUTPUT_WIDTH,
            dstHeight = OUTPUT_HEIGHT
        )
        val warpedFloat = FloatArray(warped.size) { warped[it].toFloat() }
        val input = normalizedNchw(warped)
        return PreprocessResult(center, scale, warpMatrix, warped, warpedFloat, input)
    }

    fun runSession(inputNchw: FloatArray): SimccOutput {
        require(inputNchw.size == 1 * 3 * OUTPUT_HEIGHT * OUTPUT_WIDTH)
        val inputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputNchw),
            longArrayOf(1, 3, OUTPUT_HEIGHT.toLong(), OUTPUT_WIDTH.toLong())
        )
        try {
            val result = session.run(mapOf(inputName to inputTensor))
            try {
                val x = result.get(0).tensorFloats()
                val y = result.get(1).tensorFloats()
                return SimccOutput(x.data, x.shape, y.data, y.shape)
            } finally {
                result.close()
            }
        } finally {
            inputTensor.close()
        }
    }

    fun decode(simcc: SimccOutput, center: FloatArray, scale: FloatArray): PoseResult {
        require(simcc.simccXShape.contentEquals(longArrayOf(1, KEYPOINT_COUNT.toLong(), (OUTPUT_WIDTH * 2).toLong())))
        require(simcc.simccYShape.contentEquals(longArrayOf(1, KEYPOINT_COUNT.toLong(), (OUTPUT_HEIGHT * 2).toLong())))
        val keypoints = FloatArray(KEYPOINT_COUNT * 2)
        val scores = FloatArray(KEYPOINT_COUNT)
        for (k in 0 until KEYPOINT_COUNT) {
            var bestX = 0
            var bestXVal = Float.NEGATIVE_INFINITY
            val xBase = k * OUTPUT_WIDTH * 2
            for (i in 0 until OUTPUT_WIDTH * 2) {
                val v = simcc.simccX[xBase + i]
                if (v > bestXVal) {
                    bestXVal = v
                    bestX = i
                }
            }

            var bestY = 0
            var bestYVal = Float.NEGATIVE_INFINITY
            val yBase = k * OUTPUT_HEIGHT * 2
            for (i in 0 until OUTPUT_HEIGHT * 2) {
                val v = simcc.simccY[yBase + i]
                if (v > bestYVal) {
                    bestYVal = v
                    bestY = i
                }
            }

            val score = 0.5f * (bestXVal + bestYVal)
            scores[k] = score
            val decodedX = if (score <= 0f) -1f else bestX.toFloat() / SIMCC_SPLIT_RATIO
            val decodedY = if (score <= 0f) -1f else bestY.toFloat() / SIMCC_SPLIT_RATIO
            keypoints[k * 2] = decodedX / OUTPUT_WIDTH.toFloat() * scale[0] + center[0] - scale[0] / 2f
            keypoints[k * 2 + 1] = decodedY / OUTPUT_HEIGHT.toFloat() * scale[1] + center[1] - scale[1] / 2f
        }
        return PoseResult(keypoints, scores)
    }

    fun estimate(imageBgr: IntArray, width: Int, height: Int, bboxXyxy: FloatArray): PoseResult {
        val pre = preprocess(imageBgr, width, height, bboxXyxy)
        return decode(runSession(pre.inputNchw), pre.center, pre.scale)
    }

    override fun close() {
        session.close()
    }

    companion object {
        const val OUTPUT_WIDTH = 192
        const val OUTPUT_HEIGHT = 256
        const val KEYPOINT_COUNT = 26
        const val SIMCC_SPLIT_RATIO = 2.0f
        val MEAN_BGR = floatArrayOf(123.675f, 116.28f, 103.53f)
        val STD_BGR = floatArrayOf(58.395f, 57.12f, 57.375f)

        fun bboxXyxy2cs(bboxXyxy: FloatArray, padding: Float = 1.25f): Pair<FloatArray, FloatArray> {
            val center = floatArrayOf(
                (bboxXyxy[0] + bboxXyxy[2]) * 0.5f,
                (bboxXyxy[1] + bboxXyxy[3]) * 0.5f
            )
            val scale = floatArrayOf(
                (bboxXyxy[2] - bboxXyxy[0]) * padding,
                (bboxXyxy[3] - bboxXyxy[1]) * padding
            )
            return center to scale
        }

        fun fitScaleToAspectRatio(scale: FloatArray): FloatArray {
            val out = scale.copyOf()
            val aspectRatio = OUTPUT_WIDTH.toFloat() / OUTPUT_HEIGHT.toFloat()
            if (out[0] > out[1] * aspectRatio) {
                out[1] = out[0] / aspectRatio
            } else {
                out[0] = out[1] * aspectRatio
            }
            return out
        }

        fun getWarpMatrix(center: FloatArray, scale: FloatArray, outputWidth: Int, outputHeight: Int): FloatArray {
            val srcW = scale[0].toDouble()
            val dstW = outputWidth.toDouble()
            val dstH = outputHeight.toDouble()
            val srcDir = rotatePoint(doubleArrayOf(0.0, srcW * -0.5), 0.0)
            val dstDir = doubleArrayOf(0.0, dstW * -0.5)

            val src0 = doubleArrayOf(center[0].toDouble(), center[1].toDouble())
            val src1 = doubleArrayOf(src0[0] + srcDir[0], src0[1] + srcDir[1])
            val src2 = getThirdPoint(src0, src1)
            val dst0 = doubleArrayOf(dstW * 0.5, dstH * 0.5)
            val dst1 = doubleArrayOf(dst0[0] + dstDir[0], dst0[1] + dstDir[1])
            val dst2 = getThirdPoint(dst0, dst1)
            return affineFromThreePoints(arrayOf(src0, src1, src2), arrayOf(dst0, dst1, dst2))
        }

        fun warpAffineBilinear(
            src: IntArray,
            srcWidth: Int,
            srcHeight: Int,
            matrix: FloatArray,
            dstWidth: Int,
            dstHeight: Int
        ): IntArray {
            val inv = invertAffine(matrix)
            val out = IntArray(dstWidth * dstHeight * 3)
            for (y in 0 until dstHeight) {
                for (x in 0 until dstWidth) {
                    val srcX = inv[0] * x + inv[1] * y + inv[2]
                    val srcY = inv[3] * x + inv[4] * y + inv[5]
                    val dstBase = (y * dstWidth + x) * 3
                    sampleBilinearBorderZero(src, srcWidth, srcHeight, srcX, srcY, out, dstBase)
                }
            }
            return out
        }

        fun normalizedNchw(warpedBgr: IntArray): FloatArray {
            require(warpedBgr.size == OUTPUT_WIDTH * OUTPUT_HEIGHT * 3)
            val out = FloatArray(1 * 3 * OUTPUT_HEIGHT * OUTPUT_WIDTH)
            var dst = 0
            for (channel in 0 until 3) {
                for (y in 0 until OUTPUT_HEIGHT) {
                    for (x in 0 until OUTPUT_WIDTH) {
                        val pixel = warpedBgr[((y * OUTPUT_WIDTH + x) * 3) + channel].toFloat()
                        out[dst++] = (pixel - MEAN_BGR[channel]) / STD_BGR[channel]
                    }
                }
            }
            return out
        }

        private fun rotatePoint(point: DoubleArray, angleRad: Double): DoubleArray {
            val sn = sin(angleRad)
            val cs = cos(angleRad)
            return doubleArrayOf(point[0] * cs - point[1] * sn, point[0] * sn + point[1] * cs)
        }

        private fun getThirdPoint(a: DoubleArray, b: DoubleArray): DoubleArray {
            val directionX = a[0] - b[0]
            val directionY = a[1] - b[1]
            return doubleArrayOf(b[0] - directionY, b[1] + directionX)
        }

        private fun affineFromThreePoints(src: Array<DoubleArray>, dst: Array<DoubleArray>): FloatArray {
            val srcMat = arrayOf(
                doubleArrayOf(src[0][0], src[0][1], 1.0),
                doubleArrayOf(src[1][0], src[1][1], 1.0),
                doubleArrayOf(src[2][0], src[2][1], 1.0)
            )
            val xParams = solve3x3(srcMat, doubleArrayOf(dst[0][0], dst[1][0], dst[2][0]))
            val yParams = solve3x3(srcMat, doubleArrayOf(dst[0][1], dst[1][1], dst[2][1]))
            return floatArrayOf(
                xParams[0].toFloat(),
                xParams[1].toFloat(),
                xParams[2].toFloat(),
                yParams[0].toFloat(),
                yParams[1].toFloat(),
                yParams[2].toFloat()
            )
        }

        private fun solve3x3(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
            val m = Array(3) { row -> doubleArrayOf(a[row][0], a[row][1], a[row][2], b[row]) }
            for (col in 0 until 3) {
                var pivot = col
                for (row in col + 1 until 3) {
                    if (kotlin.math.abs(m[row][col]) > kotlin.math.abs(m[pivot][col])) pivot = row
                }
                if (pivot != col) {
                    val tmp = m[col]
                    m[col] = m[pivot]
                    m[pivot] = tmp
                }
                val div = m[col][col]
                for (j in col until 4) m[col][j] /= div
                for (row in 0 until 3) {
                    if (row == col) continue
                    val factor = m[row][col]
                    for (j in col until 4) m[row][j] -= factor * m[col][j]
                }
            }
            return doubleArrayOf(m[0][3], m[1][3], m[2][3])
        }

        private fun invertAffine(matrix: FloatArray): DoubleArray {
            val a = matrix[0].toDouble()
            val b = matrix[1].toDouble()
            val c = matrix[2].toDouble()
            val d = matrix[3].toDouble()
            val e = matrix[4].toDouble()
            val f = matrix[5].toDouble()
            val det = a * e - b * d
            return doubleArrayOf(
                e / det,
                -b / det,
                (b * f - c * e) / det,
                -d / det,
                a / det,
                (c * d - a * f) / det
            )
        }

        private fun sampleBilinearBorderZero(
            src: IntArray,
            srcWidth: Int,
            srcHeight: Int,
            srcX: Double,
            srcY: Double,
            out: IntArray,
            outBase: Int
        ) {
            var x0 = floor(srcX).toInt()
            var y0 = floor(srcY).toInt()
            var xWeight = ((srcX - x0) * INTER_TAB_SIZE).roundToInt()
            var yWeight = ((srcY - y0) * INTER_TAB_SIZE).roundToInt()
            if (xWeight >= INTER_TAB_SIZE) {
                x0 += 1
                xWeight = 0
            }
            if (yWeight >= INTER_TAB_SIZE) {
                y0 += 1
                yWeight = 0
            }
            val w00 = (INTER_TAB_SIZE - xWeight) * (INTER_TAB_SIZE - yWeight)
            val w01 = xWeight * (INTER_TAB_SIZE - yWeight)
            val w10 = (INTER_TAB_SIZE - xWeight) * yWeight
            val w11 = xWeight * yWeight
            for (channel in 0 until 3) {
                val sum =
                    sampleBorderZero(src, srcWidth, srcHeight, x0, y0, channel) * w00 +
                        sampleBorderZero(src, srcWidth, srcHeight, x0 + 1, y0, channel) * w01 +
                        sampleBorderZero(src, srcWidth, srcHeight, x0, y0 + 1, channel) * w10 +
                        sampleBorderZero(src, srcWidth, srcHeight, x0 + 1, y0 + 1, channel) * w11
                out[outBase + channel] = ((sum + (1 shl (INTER_BITS * 2 - 1))) shr (INTER_BITS * 2))
                    .coerceIn(0, 255)
            }
        }

        private fun sampleBorderZero(
            src: IntArray,
            srcWidth: Int,
            srcHeight: Int,
            x: Int,
            y: Int,
            channel: Int
        ): Int {
            if (x < 0 || y < 0 || x >= srcWidth || y >= srcHeight) return 0
            return src[((y * srcWidth + x) * 3) + channel]
        }

        private const val INTER_BITS = 5
        private const val INTER_TAB_SIZE = 1 shl INTER_BITS
    }
}
