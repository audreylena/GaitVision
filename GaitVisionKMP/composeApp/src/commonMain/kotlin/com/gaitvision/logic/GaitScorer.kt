
package com.gaitvision.logic

expect class GaitScorer() {
    fun initialize(): Boolean
    fun score(features: GaitFeatures): ScoringResult
    fun release()
}

