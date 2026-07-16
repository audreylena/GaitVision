package GaitVision.com.mediapipe.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.nio.FloatBuffer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class YoloxTinyDetector private constructor(
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

    data class LetterboxResult(
        val imageBgr: IntArray,
        val width: Int,
        val height: Int,
        val ratio: Float
    )

    data class RawOutput(
        val boxes: FloatArray,
        val boxShape: LongArray,
        val classIds: LongArray,
        val classShape: LongArray
    )

    data class Detection(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val score: Float,
        val classId: Int
    ) {
        val area: Float
            get() = max(0f, x2 - x1) * max(0f, y2 - y1)

        fun toArray(): FloatArray = floatArrayOf(x1, y1, x2, y2)
    }

    fun letterbox(imageBgr: IntArray, width: Int, height: Int): LetterboxResult {
        require(imageBgr.size == width * height * 3) { "imageBgr must be HWC BGR" }
        val output = IntArray(INPUT_SIZE * INPUT_SIZE * 3) { PAD_VALUE }
        val ratio = min(INPUT_SIZE.toFloat() / height.toFloat(), INPUT_SIZE.toFloat() / width.toFloat())
        val resizedWidth = (width * ratio).toInt()
        val resizedHeight = (height * ratio).toInt()
        resizeBilinearToTopLeft(
            src = imageBgr,
            srcWidth = width,
            srcHeight = height,
            dst = output,
            dstStrideWidth = INPUT_SIZE,
            resizedWidth = resizedWidth,
            resizedHeight = resizedHeight
        )
        return LetterboxResult(output, INPUT_SIZE, INPUT_SIZE, ratio)
    }

    fun inputTensorNchw(letterboxedBgr: IntArray): FloatArray {
        require(letterboxedBgr.size == INPUT_SIZE * INPUT_SIZE * 3)
        val out = FloatArray(1 * 3 * INPUT_SIZE * INPUT_SIZE)
        var dst = 0
        for (channel in 0 until 3) {
            for (y in 0 until INPUT_SIZE) {
                for (x in 0 until INPUT_SIZE) {
                    out[dst++] = letterboxedBgr[((y * INPUT_SIZE + x) * 3) + channel].toFloat()
                }
            }
        }
        return out
    }

    fun runSession(inputNchw: FloatArray): RawOutput {
        require(inputNchw.size == 1 * 3 * INPUT_SIZE * INPUT_SIZE)
        val inputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputNchw),
            longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        )
        try {
            val result = session.run(mapOf(inputName to inputTensor))
            try {
                val boxes = result.get(0).tensorFloats()
                val classIds = result.get(1).tensorLongs()
                return RawOutput(
                    boxes = boxes.data,
                    boxShape = boxes.shape,
                    classIds = classIds.data,
                    classShape = classIds.shape
                )
            } finally {
                result.close()
            }
        } finally {
            inputTensor.close()
        }
    }

    fun detect(imageBgr: IntArray, width: Int, height: Int): List<Detection> {
        val letterboxed = letterbox(imageBgr, width, height)
        val raw = runSession(inputTensorNchw(letterboxed.imageBgr))
        return postprocess(raw, letterboxed.ratio)
    }

    fun postprocess(raw: RawOutput, ratio: Float): List<Detection> {
        require(raw.boxShape.size == 3 && raw.boxShape[0] == 1L && raw.boxShape[2] == 5L) {
            "expected boxes shape (1,N,5), got ${raw.boxShape.contentToString()}"
        }
        require(raw.classShape.size == 2 && raw.classShape[0] == 1L) {
            "expected class shape (1,N), got ${raw.classShape.contentToString()}"
        }
        val count = raw.boxShape[1].toInt()
        val detections = ArrayList<Detection>(count)
        for (i in 0 until count) {
            val classId = raw.classIds[i].toInt()
            val score = raw.boxes[i * 5 + 4]
            if (classId == PERSON_CLASS_ID && score > SCORE_THRESHOLD) {
                detections += Detection(
                    x1 = raw.boxes[i * 5] / ratio,
                    y1 = raw.boxes[i * 5 + 1] / ratio,
                    x2 = raw.boxes[i * 5 + 2] / ratio,
                    y2 = raw.boxes[i * 5 + 3] / ratio,
                    score = score,
                    classId = classId
                )
            }
        }
        return detections
    }

    fun largestArea(detections: List<Detection>): Detection? =
        detections.maxByOrNull { it.area }

    override fun close() {
        session.close()
    }

    companion object {
        const val INPUT_SIZE = 416
        const val PAD_VALUE = 114
        const val SCORE_THRESHOLD = 0.3f
        const val PERSON_CLASS_ID = 0

        internal fun resizeBilinearToTopLeft(
            src: IntArray,
            srcWidth: Int,
            srcHeight: Int,
            dst: IntArray,
            dstStrideWidth: Int,
            resizedWidth: Int,
            resizedHeight: Int
        ) {
            val scaleX = srcWidth.toDouble() / resizedWidth.toDouble()
            val scaleY = srcHeight.toDouble() / resizedHeight.toDouble()
            for (y in 0 until resizedHeight) {
                val srcY = (y + 0.5) * scaleY - 0.5
                val y0Raw = floor(srcY).toInt()
                val yWeight = srcY - y0Raw
                val y0 = y0Raw.coerceIn(0, srcHeight - 1)
                val y1 = (y0Raw + 1).coerceIn(0, srcHeight - 1)
                for (x in 0 until resizedWidth) {
                    val srcX = (x + 0.5) * scaleX - 0.5
                    val x0Raw = floor(srcX).toInt()
                    val xWeight = srcX - x0Raw
                    val x0 = x0Raw.coerceIn(0, srcWidth - 1)
                    val x1 = (x0Raw + 1).coerceIn(0, srcWidth - 1)
                    val dstBase = (y * dstStrideWidth + x) * 3
                    val src00 = (y0 * srcWidth + x0) * 3
                    val src01 = (y0 * srcWidth + x1) * 3
                    val src10 = (y1 * srcWidth + x0) * 3
                    val src11 = (y1 * srcWidth + x1) * 3
                    for (c in 0 until 3) {
                        val top = src[src00 + c] * (1.0 - xWeight) + src[src01 + c] * xWeight
                        val bottom = src[src10 + c] * (1.0 - xWeight) + src[src11 + c] * xWeight
                        dst[dstBase + c] = (top * (1.0 - yWeight) + bottom * yWeight)
                            .roundToInt()
                            .coerceIn(0, 255)
                    }
                }
            }
        }
    }
}

internal data class FloatTensorData(val data: FloatArray, val shape: LongArray)

internal data class LongTensorData(val data: LongArray, val shape: LongArray)

internal fun OnnxValue.tensorFloats(): FloatTensorData {
    val tensor = this as OnnxTensor
    val shape = (tensor.info as TensorInfo).shape
    val out = ArrayList<Float>()
    flattenFloats(tensor.value, out)
    return FloatTensorData(out.toFloatArray(), shape)
}

internal fun OnnxValue.tensorLongs(): LongTensorData {
    val tensor = this as OnnxTensor
    val shape = (tensor.info as TensorInfo).shape
    val out = ArrayList<Long>()
    flattenLongs(tensor.value, out)
    return LongTensorData(out.toLongArray(), shape)
}

private fun flattenFloats(value: Any?, out: MutableList<Float>) {
    when (value) {
        is Float -> out += value
        is Double -> out += value.toFloat()
        is Number -> out += value.toFloat()
        is FloatArray -> value.forEach { out += it }
        is DoubleArray -> value.forEach { out += it.toFloat() }
        is Array<*> -> value.forEach { flattenFloats(it, out) }
        else -> error("unsupported float tensor value ${value?.javaClass}")
    }
}

private fun flattenLongs(value: Any?, out: MutableList<Long>) {
    when (value) {
        is Long -> out += value
        is Int -> out += value.toLong()
        is Number -> out += value.toLong()
        is LongArray -> value.forEach { out += it }
        is IntArray -> value.forEach { out += it.toLong() }
        is Array<*> -> value.forEach { flattenLongs(it, out) }
        else -> error("unsupported long tensor value ${value?.javaClass}")
    }
}
