package GaitVision.com.mediapipe.onnx

import android.graphics.Bitmap

data class BgrImage(
    val width: Int,
    val height: Int,
    val data: IntArray
)

object BitmapConverters {
    fun bitmapToBgr(bitmap: Bitmap): BgrImage {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bgr = IntArray(width * height * 3)
        for (i in pixels.indices) {
            val argb = pixels[i]
            val base = i * 3
            bgr[base] = argb and 0xff
            bgr[base + 1] = (argb ushr 8) and 0xff
            bgr[base + 2] = (argb ushr 16) and 0xff
        }
        return BgrImage(width, height, bgr)
    }
}
