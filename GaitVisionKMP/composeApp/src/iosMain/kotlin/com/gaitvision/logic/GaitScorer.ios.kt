package com.gaitvision.logic

actual class GaitScorer {
    actual fun initialize(): Boolean {
        Log.d("GaitScorer-iOS", "Mock initialization for iOS due to missing TFLite.")
        return true
    }

    actual fun score(features: GaitFeatures): ScoringResult {
        Log.d("GaitScorer-iOS", "Returning mock scores for iOS target.")
        return ScoringResult(aeScore = 85.0f, ridgeScore = 80.0f, pcaScore = 90.0f)
    }

    actual fun release() {
        Log.d("GaitScorer-iOS", "Released mock scorer.")
    }
}
