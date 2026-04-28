package com.gaitvision.utils

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Common mathematical utility functions for calculating gait metrics and features.
 */
object MathUtils {

    /**
     * Calculates the Euclidean distance between two 2D points.
     */
    fun calculateDistance2D(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx.pow(2) + dy.pow(2))
    }

    /**
     * Calculates the Euclidean distance between two 3D points.
     */
    fun calculateDistance3D(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return sqrt(dx.pow(2) + dy.pow(2) + dz.pow(2))
    }

    /**
     * Computes the moving average of a given array of floats with a specified window size.
     */
    fun computeMovingAverage(data: List<Float>, windowSize: Int): List<Float> {
        if (data.isEmpty() || windowSize <= 0) return data
        val result = mutableListOf<Float>()
        
        for (i in data.indices) {
            val start = maxOf(0, i - windowSize / 2)
            val end = minOf(data.size - 1, i + windowSize / 2)
            var sum = 0f
            var count = 0
            
            for (j in start..end) {
                sum += data[j]
                count++
            }
            result.add(sum / count)
        }
        return result
    }

    /**
     * Finds local maxima in a series of data points. Useful for peak detection in gait signals.
     */
    fun findPeaks(data: List<Float>, threshold: Float = 0f): List<Int> {
        val peaks = mutableListOf<Int>()
        if (data.size < 3) return peaks
        
        for (i in 1 until data.size - 1) {
            if (data[i] > data[i - 1] && data[i] > data[i + 1] && data[i] >= threshold) {
                peaks.add(i)
            }
        }
        return peaks
    }

    /**
     * Finds local minima in a series of data points. Useful for valley detection in gait signals.
     */
    fun findValleys(data: List<Float>, threshold: Float = Float.MAX_VALUE): List<Int> {
        val valleys = mutableListOf<Int>()
        if (data.size < 3) return valleys
        
        for (i in 1 until data.size - 1) {
            if (data[i] < data[i - 1] && data[i] < data[i + 1] && data[i] <= threshold) {
                valleys.add(i)
            }
        }
        return valleys
    }

    /**
     * Standardizes a dataset (Z-score normalization).
     */
    fun standardizeData(data: List<Float>): List<Float> {
        if (data.isEmpty()) return data
        val mean = data.average().toFloat()
        val variance = data.map { (it - mean).pow(2) }.average().toFloat()
        val stdDev = sqrt(variance)
        
        if (stdDev == 0f) return data.map { 0f }
        return data.map { (it - mean) / stdDev }
    }
}
