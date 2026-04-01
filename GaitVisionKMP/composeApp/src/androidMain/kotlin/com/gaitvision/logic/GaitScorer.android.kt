package com.gaitvision.logic

actual class GaitScorer {
    actual fun initialize(): Boolean {
        Log.d("GaitScorer-Android", "Mock initialization for Android due to missing TFLite.")
        return true
    }

    actual fun score(features: GaitFeatures): ScoringResult {
        Log.d("GaitScorer-Android", "Returning mock scores for Android target.")
        return ScoringResult(aeScore = 85.0f, ridgeScore = 80.0f, pcaScore = 90.0f)
    }

    actual fun release() {
        Log.d("GaitScorer-Android", "Released mock scorer.")
    }
}
