package GaitVision.com.gait

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import android.content.Context
import android.util.Log
import GaitVision.com.BuildConfig
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Gait scoring using 3 trained models:
 * 1. AE-2D-tapered-latent - Autoencoder (latent: Mahalanobis) - THIS IS USED FOR DB
 * 2. Ridge - Linear regression (severity prediction)
 * 3. PCA-2-all - PCA reconstruction error
 * 
 * Each model has its own scaler embedded in its JSON config.
 * Only AE score is used for the patient database.
 */
class GaitScorer(private val context: Context) {
    
    companion object {
        private const val TAG = "GaitDebug"
        
        // Model files
        private const val AE_CONFIG = "AE-2D-tapered-latent.json"
        private const val RIDGE_CONFIG = "Ridge.json"
        private const val PCA_CONFIG = "PCA-2-all.json"
        
        // Shared output mapping: same breakpoints for all models (0-100 input -> 0-100 output)
        private val OUTPUT_BREAKPOINTS = floatArrayOf(50f, 70f, 85f)
        private val OUTPUT_HEALTH_SCORES = floatArrayOf(0f, 40f, 65f, 85f, 100f)
    }
    
    // Clinical mapping config (loaded from JSON)
    private data class ClinicalMapping(
        val lowerIsBetter: Boolean,
        val breakpoints: FloatArray,
        val healthScores: FloatArray
    )
    
    // AE model
    private var aeInterpreter: Interpreter? = null
    private var aeScalerMean: FloatArray? = null
    private var aeScalerScale: FloatArray? = null
    private var aeScoreP1: Float = 0f
    private var aeScoreP99: Float = 1f
    private var aeClinicalMapping: ClinicalMapping? = null
    private var aeAvailable = false
    private var aeIsLatent = false
    private var aeNormalCentroid: FloatArray? = null
    private var aeInvCovariance: Array<FloatArray>? = null
    private var aeLatentDim: Int = 2
    
    // Ridge model
    private var ridgeCoef: FloatArray? = null
    private var ridgeIntercept: Float = 0f
    private var ridgeScalerMean: FloatArray? = null
    private var ridgeScalerScale: FloatArray? = null
    private var ridgeScoreMin: Float = 0f      // From score_range
    private var ridgeScoreMax: Float = 100f    // From score_range
    private var ridgeThreshold: Float = 75f    // Classification threshold
    private var ridgeClinicalMapping: ClinicalMapping? = null
    private var ridgeAvailable = false
    
    // PCA model (latent distance to normal centroid)
    private var pcaComponents: Array<FloatArray>? = null
    private var pcaScalerMean: FloatArray? = null
    private var pcaScalerScale: FloatArray? = null
    private var pcaNormalCentroid: FloatArray? = null
    private var pcaScoreP1: Float = 0f
    private var pcaScoreP99: Float = 1f
    private var pcaClinicalMapping: ClinicalMapping? = null
    private var pcaAvailable = false
    
    private var isInitialized = false
    
    /**
     * Initialize all models from JSON configs.
     */
    fun initialize(): Boolean {
        try {
            // Load AE model
            loadAEModel()
            
            // Load Ridge model
            loadRidgeModel()
            
            // Load PCA model
            loadPCAModel()
            
            isInitialized = aeAvailable || ridgeAvailable || pcaAvailable
            Log.d(TAG, "Scorer initialized. Available: AE=$aeAvailable, Ridge=$ridgeAvailable, PCA=$pcaAvailable")
            return isInitialized
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize scorer", e)
            return false
        }
    }
    
    private fun loadAEModel() {
        try {
            Log.d(TAG, "Loading AE model from $AE_CONFIG...")
            val configJson = loadJsonAsset(AE_CONFIG)
            if (configJson == null) {
                Log.e(TAG, "AE: Could not load config JSON")
                return
            }
            val config = JSONObject(configJson)

            // Load scaler
            val scaler = config.getJSONObject("scaler")
            aeScalerMean = jsonArrayToFloatArray(scaler.getJSONArray("mean"))
            aeScalerScale = jsonArrayToFloatArray(scaler.getJSONArray("scale"))
            Log.d(TAG, "AE: Loaded scaler mean=${aeScalerMean?.size}, scale=${aeScalerScale?.size}")

            // Load score mapping
            if (config.has("score_mapping")) {
                val scoreMapping = config.getJSONObject("score_mapping")
                aeScoreP1 = scoreMapping.getDouble("p1").toFloat()
                aeScoreP99 = scoreMapping.getDouble("p99").toFloat()
                Log.d(TAG, "AE: score_mapping p1=$aeScoreP1, p99=$aeScoreP99")
            }

            // Load clinical mapping (optional; latent may use score_mapping)
            aeClinicalMapping = loadClinicalMapping(config)
            Log.d(TAG, "AE: clinical mapping loaded=${aeClinicalMapping != null}")

            // Latent model: has inv_covariance, normal_centroid; use encoder TFLite
            aeIsLatent = config.has("inv_covariance") && config.has("normal_centroid")
            val tfliteFile = if (aeIsLatent && config.has("encoder_tflite")) {
                config.getString("encoder_tflite")
            } else {
                config.optString("tflite_model", "").takeIf { it.isNotEmpty() }
            }

            if (aeIsLatent) {
                aeLatentDim = config.optInt("latent_dim", 2)
                aeNormalCentroid = jsonArrayToFloatArray(config.getJSONArray("normal_centroid"))
                val invCovJson = config.getJSONArray("inv_covariance")
                aeInvCovariance = Array(invCovJson.length()) { i ->
                    jsonArrayToFloatArray(invCovJson.getJSONArray(i))
                }
                Log.d(TAG, "AE: latent mode, centroid=${aeNormalCentroid?.size}, inv_cov=${aeInvCovariance?.size}x${aeInvCovariance?.getOrNull(0)?.size}")
            }

            val modelBuffer = tfliteFile?.let { loadModelFile(it) }
            if (modelBuffer != null && aeClinicalMapping != null) {
                aeInterpreter = Interpreter(modelBuffer)
                aeAvailable = true
                val interp = aeInterpreter!!
                Log.d(TAG, "AE TFLite loaded. Input: ${interp.getInputTensor(0).shape().contentToString()}, Output: ${interp.getOutputTensor(0).shape().contentToString()}")
                Log.d(TAG, "Loaded AE model: ${config.optString("model_name")} (latent=$aeIsLatent)")
            } else {
                Log.e(TAG, "AE: Could not load TFLite model file: $tfliteFile")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load AE model: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun loadRidgeModel() {
        try {
            val configJson = loadJsonAsset(RIDGE_CONFIG) ?: return
            val config = JSONObject(configJson)
            
            // Load scaler
            val scaler = config.getJSONObject("scaler")
            ridgeScalerMean = jsonArrayToFloatArray(scaler.getJSONArray("mean"))
            ridgeScalerScale = jsonArrayToFloatArray(scaler.getJSONArray("scale"))
            
            // Load coefficients from ridge object (single source of truth)
            val ridge = config.getJSONObject("ridge")
            ridgeCoef = jsonArrayToFloatArray(ridge.getJSONArray("coefficients"))
            ridgeIntercept = ridge.getDouble("intercept").toFloat()
            
            // Load score range for proper 0-100 mapping
            val scoreRange = config.getJSONObject("score_range")
            ridgeScoreMin = scoreRange.getDouble("min").toFloat()
            ridgeScoreMax = scoreRange.getDouble("max").toFloat()
            ridgeThreshold = config.getDouble("threshold").toFloat()
            
            // Load clinical mapping
            ridgeClinicalMapping = loadClinicalMapping(config)
            Log.d(TAG, "Ridge: clinical mapping loaded=${ridgeClinicalMapping != null}")
            
            Log.d(TAG, "Ridge: range=[$ridgeScoreMin, $ridgeScoreMax], threshold=$ridgeThreshold")
            
            ridgeAvailable = ridgeClinicalMapping != null
            Log.d(TAG, "Loaded Ridge model: ${config.getString("model_name")}, available=$ridgeAvailable")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load Ridge model: ${e.message}")
        }
    }
    
    private fun loadPCAModel() {
        try {
            Log.d(TAG, "Loading PCA model from $PCA_CONFIG...")
            val configJson = loadJsonAsset(PCA_CONFIG)
            if (configJson == null) {
                Log.e(TAG, "PCA: Could not load config JSON")
                return
            }
            val config = JSONObject(configJson)
            
            // Load scaler
            val scaler = config.getJSONObject("scaler")
            pcaScalerMean = jsonArrayToFloatArray(scaler.getJSONArray("mean"))
            pcaScalerScale = jsonArrayToFloatArray(scaler.getJSONArray("scale"))
            Log.d(TAG, "PCA: Loaded scaler mean=${pcaScalerMean?.size}, scale=${pcaScalerScale?.size}")
            
            // Load PCA components (4x16 matrix)
            val pca = config.getJSONObject("pca")
            val componentsJson = pca.getJSONArray("components")
            pcaComponents = Array(componentsJson.length()) { i ->
                jsonArrayToFloatArray(componentsJson.getJSONArray(i))
            }
            Log.d(TAG, "PCA: Loaded components ${pcaComponents?.size}x${pcaComponents?.get(0)?.size}")
            
            // Load score mapping
            val scoreMapping = config.getJSONObject("score_mapping")
            pcaScoreP1 = scoreMapping.getDouble("p1").toFloat()
            pcaScoreP99 = scoreMapping.getDouble("p99").toFloat()
            Log.d(TAG, "PCA: score_mapping p1=$pcaScoreP1, p99=$pcaScoreP99")
            
            // Load clinical mapping
            pcaClinicalMapping = loadClinicalMapping(config)
            Log.d(TAG, "PCA: clinical mapping loaded=${pcaClinicalMapping != null}")
            
            // Load normal centroid (required for latent distance scoring)
            pcaNormalCentroid = jsonArrayToFloatArray(config.getJSONArray("normal_centroid"))
            Log.d(TAG, "PCA: normal_centroid=${pcaNormalCentroid?.size}")
            
            pcaAvailable = pcaClinicalMapping != null && pcaNormalCentroid != null
            Log.d(TAG, "Loaded PCA model: ${config.getString("model_name")}, available=$pcaAvailable")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PCA model: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Load clinical mapping from JSON config.
     * Returns null if not present (falls back to hardcoded defaults).
     */
    private fun loadClinicalMapping(config: JSONObject): ClinicalMapping? {
        return try {
            if (!config.has("clinical_mapping")) return null
            
            val mapping = config.getJSONObject("clinical_mapping")
            val lowerIsBetter = mapping.getBoolean("lower_is_better")
            val breakpointsJson = mapping.getJSONArray("breakpoints")
            val healthScoresJson = mapping.getJSONArray("health_scores")
            
            val breakpoints = FloatArray(breakpointsJson.length()) { 
                breakpointsJson.getDouble(it).toFloat() 
            }
            val healthScores = FloatArray(healthScoresJson.length()) { 
                healthScoresJson.getDouble(it).toFloat() 
            }
            
            ClinicalMapping(lowerIsBetter, breakpoints, healthScores)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load clinical_mapping: ${e.message}")
            null
        }
    }
    
    /**
     * Apply shared output mapping: 0-100 input -> 0-100 output.
     * Same breakpoints [50, 70, 85] for all models. Higher input = healthier.
     */
    private fun applySharedOutputMapping(healthPre: Float): Float {
        val clamped = healthPre.coerceIn(0f, 100f)
        val breakpoints = OUTPUT_BREAKPOINTS
        val healthScores = OUTPUT_HEALTH_SCORES
        for (i in breakpoints.indices) {
            if (clamped <= breakpoints[i]) {
                val prevBreak = if (i == 0) 0f else breakpoints[i - 1]
                val prevHealth = if (i == 0) healthScores[0] else healthScores[i]
                val nextHealth = healthScores[i + 1]
                val t = (clamped - prevBreak) / (breakpoints[i] - prevBreak)
                return prevHealth + t * (nextHealth - prevHealth)
            }
        }
        val lastBreak = breakpoints.last()
        val lastHealth = healthScores[healthScores.size - 2]
        val maxHealth = healthScores.last()
        val lastSpan = if (breakpoints.size >= 2) breakpoints.last() - breakpoints[breakpoints.size - 2] else 15f
        val t = ((clamped - lastBreak) / lastSpan).coerceAtMost(1f)
        return minOf(maxHealth, lastHealth + t * (maxHealth - lastHealth))
    }
    
    /**
     * Apply clinical mapping to convert raw score to 0-100 health score.
     * Uses piecewise linear interpolation between breakpoints.
     */
    private fun applyClinicalMapping(rawScore: Float, mapping: ClinicalMapping): Float {
        val breakpoints = mapping.breakpoints
        val healthScores = mapping.healthScores
        
        // Find which segment the score falls into
        if (mapping.lowerIsBetter) {
            // Lower raw = healthier (MSE-based: AE, PCA)
            // healthScores[0] = score at raw=0, healthScores[n] = score at raw=infinity
            for (i in breakpoints.indices) {
                if (rawScore <= breakpoints[i]) {
                    val prevBreak = if (i == 0) 0f else breakpoints[i - 1]
                    val prevHealth = healthScores[i]
                    val nextHealth = healthScores[i + 1]
                    val t = (rawScore - prevBreak) / (breakpoints[i] - prevBreak)
                    return prevHealth - t * (prevHealth - nextHealth)
                }
            }
            // Beyond last breakpoint - extrapolate to minimum
            val lastBreak = breakpoints.last()
            val lastHealth = healthScores[healthScores.size - 2]
            val minHealth = healthScores.last()
            // Extrapolate: assume same span as last segment for gradual decline
            val lastSpan = if (breakpoints.size >= 2) breakpoints.last() - breakpoints[breakpoints.size - 2] else lastBreak
            val t = ((rawScore - lastBreak) / (lastSpan * 3)).coerceAtMost(1f)  // 3x span to reach minimum
            return maxOf(minHealth, lastHealth - t * (lastHealth - minHealth))
        } else {
            // Higher raw = healthier (Ridge regression)
            // healthScores[0] = score at raw=0, healthScores[n] = score at raw=infinity
            for (i in breakpoints.indices) {
                if (rawScore <= breakpoints[i]) {
                    val prevBreak = if (i == 0) 0f else breakpoints[i - 1]
                    val prevHealth = if (i == 0) healthScores[0] else healthScores[i]
                    val nextHealth = healthScores[i + 1]
                    val t = (rawScore - prevBreak) / (breakpoints[i] - prevBreak)
                    return prevHealth + t * (nextHealth - prevHealth)
                }
            }
            // Beyond last breakpoint - extrapolate to maximum
            val lastBreak = breakpoints.last()
            val lastHealth = healthScores[healthScores.size - 2]
            val maxHealth = healthScores.last()
            val lastSpan = if (breakpoints.size >= 2) breakpoints.last() - breakpoints[breakpoints.size - 2] else 15f
            val t = ((rawScore - lastBreak) / lastSpan).coerceAtMost(1f)
            return minOf(maxHealth, lastHealth + t * (maxHealth - lastHealth))
        }
    }
    
    /**
     * Score gait features using all available models.
     * Returns ScoringResult with individual model scores.
     * 
     * NOTE: For patient DB, use aeScore (the primary score).
     */
    fun score(features: GaitFeatures): ScoringResult {
        if (!isInitialized) {
            Log.w(TAG, "Scorer not initialized")
            return ScoringResult.default()
        }
        
        val featureArray = features.toFeatureArray()
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "=== SCORING DEBUG ===")
            Log.d(TAG, "Raw features (${featureArray.size}):")
            GaitFeatures.FEATURE_COLUMNS.forEachIndexed { i, name ->
                val value = if (i < featureArray.size) featureArray[i] else Float.NaN
                Log.d(TAG, "  [$i] $name = $value")
            }
        }
        
        val nanCount = featureArray.count { it.isNaN() }
        if (nanCount > 0) {
            Log.w(TAG, "WARNING: $nanCount NaN values in input features!")
        }
        
        val aeScore = if (aeAvailable) computeAEScore(featureArray) else Float.NaN
        val ridgeScore = if (ridgeAvailable) computeRidgeScore(featureArray) else Float.NaN
        val pcaScore = if (pcaAvailable) computePCAScore(featureArray) else Float.NaN
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "=== FINAL SCORES ===")
            Log.d(TAG, "  AE: $aeScore (available=$aeAvailable)")
            Log.d(TAG, "  Ridge: $ridgeScore (available=$ridgeAvailable)")
            Log.d(TAG, "  PCA: $pcaScore (available=$pcaAvailable)")
        }
        
        return ScoringResult(
            aeScore = aeScore,           // THIS IS THE PRIMARY SCORE FOR DB
            ridgeScore = ridgeScore,
            pcaScore = pcaScore
        )
    }
    
    /**
     * Compute AE score. Latent: Mahalanobis distance → 0-100. Recon: MSE → 0-100.
     * 0 = severe impairment, 100 = healthy
     */
    private fun computeAEScore(features: FloatArray): Float {
        if (BuildConfig.DEBUG) Log.d(TAG, "--- AE SCORING ---")

        val model = aeInterpreter ?: run {
            Log.e(TAG, "AE FAIL: interpreter is null")
            return Float.NaN
        }
        val mean = aeScalerMean ?: run {
            Log.e(TAG, "AE FAIL: scaler mean is null")
            return Float.NaN
        }
        val scale = aeScalerScale ?: run {
            Log.e(TAG, "AE FAIL: scaler scale is null")
            return Float.NaN
        }

        val numFeatures = 16
        val normalized = scaleFeatures(features, mean, scale)
        val inputBuffer = Array(1) { FloatArray(numFeatures) }
        for (i in 0 until minOf(normalized.size, numFeatures)) {
            inputBuffer[0][i] = normalized[i]
        }

        try {
            if (aeIsLatent) {
                val centroid = aeNormalCentroid ?: return Float.NaN
                val invCov = aeInvCovariance ?: return Float.NaN
                val outputBuffer = Array(1) { FloatArray(aeLatentDim) }
                model.run(inputBuffer, outputBuffer)
                val z = outputBuffer[0]
                val dist = mahalanobisDistance(z, centroid, invCov)
                val rawScore = -dist  // higher = healthier
                val span = aeScoreP99 - aeScoreP1
                val healthPre = if (span != 0f) ((rawScore - aeScoreP1) / span * 100f).coerceIn(0f, 100f) else 50f
                val healthScore = applySharedOutputMapping(healthPre)
                if (BuildConfig.DEBUG) Log.d(TAG, "AE: latent dist=$dist, raw=$rawScore -> healthPre=$healthPre -> healthScore=$healthScore")
                return healthScore
            } else {
                val outputBuffer = Array(1) { FloatArray(numFeatures) }
                model.run(inputBuffer, outputBuffer)
                var mse = 0f
                for (i in 0 until numFeatures) {
                    val diff = inputBuffer[0][i] - outputBuffer[0][i]
                    mse += diff * diff
                }
                mse /= numFeatures
                val rawScore = -mse  // higher = healthier
                val span = aeScoreP99 - aeScoreP1
                val healthPre = if (span != 0f) ((rawScore - aeScoreP1) / span * 100f).coerceIn(0f, 100f) else 50f
                val healthScore = applySharedOutputMapping(healthPre)
                if (BuildConfig.DEBUG) Log.d(TAG, "AE: MSE=$mse -> healthPre=$healthPre -> healthScore=$healthScore")
                return healthScore
            }
        } catch (e: Exception) {
            Log.e(TAG, "AE EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            return Float.NaN
        }
    }

    /** Mahalanobis distance: sqrt((z-mu)^T @ inv_cov @ (z-mu)) */
    private fun mahalanobisDistance(z: FloatArray, mu: FloatArray, invCov: Array<FloatArray>): Float {
        val d = z.size
        val diff = FloatArray(d) { z[it] - mu[it] }
        val invCovDiff = FloatArray(d)
        for (i in 0 until d) {
            var sum = 0f
            for (j in 0 until d) {
                sum += invCov[i][j] * diff[j]
            }
            invCovDiff[i] = sum
        }
        var quad = 0f
        for (i in 0 until d) {
            quad += diff[i] * invCovDiff[i]
        }
        return sqrt(quad.coerceAtLeast(0f))
    }
    
    /**
     * Compute Ridge score (severity prediction → 0-100).
     * Ridge outputs a raw score where higher = healthier.
     * We map to 0-100 using the score_range from training.
     */
    private fun computeRidgeScore(features: FloatArray): Float {
        if (BuildConfig.DEBUG) Log.d(TAG, "--- RIDGE SCORING ---")
        
        val coef = ridgeCoef ?: return Float.NaN
        val mean = ridgeScalerMean ?: return Float.NaN
        val scale = ridgeScalerScale ?: return Float.NaN
        
        try {
            // Normalize with Ridge's scaler
            val normalized = scaleFeatures(features, mean, scale)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Ridge: normalized ${normalized.size} features")
                Log.d(TAG, "Ridge: first 4 normalized: [${normalized.take(4).joinToString()}]")
            }
            
            // Linear prediction: raw = dot(x, coef) + intercept
            var rawScore = ridgeIntercept
            for (i in normalized.indices.take(coef.size)) {
                rawScore += normalized[i] * coef[i]
            }
            
            // Ridge raw = severity (0-3 scale). Use documented formula: (1 - severity/3)*100
            val healthPre = ((1f - rawScore / 3f) * 100f).coerceIn(0f, 100f)
            val healthScore = applySharedOutputMapping(healthPre)
            if (BuildConfig.DEBUG) Log.d(TAG, "Ridge: raw=$rawScore -> healthPre=$healthPre -> healthScore=$healthScore")
            return healthScore
            
        } catch (e: Exception) {
            Log.e(TAG, "Ridge prediction error", e)
            return Float.NaN
        }
    }
    
    /**
     * Compute PCA score (latent distance to normal centroid → 0-100).
     * Project to PC space, Euclidean distance to centroid, score = -dist (higher = healthier).
     */
    private fun computePCAScore(features: FloatArray): Float {
        if (BuildConfig.DEBUG) Log.d(TAG, "--- PCA SCORING ---")
        
        val components = pcaComponents ?: return Float.NaN
        val mean = pcaScalerMean ?: return Float.NaN
        val scale = pcaScalerScale ?: return Float.NaN
        val centroid = pcaNormalCentroid ?: return Float.NaN
        
        try {
            val normalized = scaleFeatures(features, mean, scale)
            val nComponents = components.size
            val projected = FloatArray(nComponents)
            for (i in 0 until nComponents) {
                for (j in normalized.indices.take(components[i].size)) {
                    projected[i] += normalized[j] * components[i][j]
                }
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "PCA: projected (${nComponents}D): [${projected.joinToString()}]")
            
            var distSq = 0f
            for (i in 0 until nComponents) {
                val d = projected[i] - centroid[i]
                distSq += d * d
            }
            val dist = sqrt(distSq.coerceAtLeast(0f))
            val rawScore = -dist
            if (BuildConfig.DEBUG) Log.d(TAG, "PCA: dist=$dist, rawScore=$rawScore, p1=$pcaScoreP1, p99=$pcaScoreP99")
            
            val span = pcaScoreP99 - pcaScoreP1
            val healthPre = if (span != 0f) ((rawScore - pcaScoreP1) / span * 100f).coerceIn(0f, 100f) else 50f
            val healthScore = applySharedOutputMapping(healthPre)
            if (BuildConfig.DEBUG) Log.d(TAG, "PCA: healthPre=$healthPre -> healthScore=$healthScore")
            return healthScore
        } catch (e: Exception) {
            Log.e(TAG, "PCA EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            return Float.NaN
        }
    }
    
    /**
     * Apply StandardScaler: (x - mean) / scale
     */
    private fun scaleFeatures(features: FloatArray, mean: FloatArray, scale: FloatArray): FloatArray {
        val n = minOf(features.size, mean.size, scale.size)
        val scaled = FloatArray(features.size)
        
        for (i in 0 until n) {
            val feat = if (features[i].isNaN()) 0f else features[i]
            scaled[i] = if (scale[i] > 1e-8f) {
                (feat - mean[i]) / scale[i]
            } else {
                feat - mean[i]
            }
        }
        
        return scaled
    }
    
    // Helper functions
    
    private fun loadJsonAsset(filename: String): String? {
        return try {
            val raw = context.assets.open(filename).bufferedReader().use { it.readText() }
            // Sanitize JSON: Android's JSONObject can't parse Infinity/-Infinity/NaN
            raw.replace("-Infinity", "-999999")
               .replace("Infinity", "999999")
               .replace(": NaN", ": null")
        } catch (e: Exception) {
            Log.d(TAG, "Could not load $filename")
            null
        }
    }
    
    private fun jsonArrayToFloatArray(jsonArray: org.json.JSONArray): FloatArray {
        return FloatArray(jsonArray.length()) { i ->
            jsonArray.getDouble(i).toFloat()
        }
    }
    
    private fun loadModelFile(filename: String): MappedByteBuffer? {
        return try {
            val assetFileDescriptor = context.assets.openFd(filename)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.d(TAG, "Could not load model $filename")
            null
        }
    }
    
    /**
     * Release resources.
     */
    fun release() {
        aeInterpreter?.close()
        aeInterpreter = null
        isInitialized = false
    }
}

/**
 * Scoring results from all 3 models.
 * 
 * All scores are 0-100 where 100 = healthy, 0 = severely impaired.
 * 
 * For patient database: USE aeScore
 */
@Parcelize
data class ScoringResult(
    val aeScore: Float,      // Autoencoder - PRIMARY SCORE FOR DB
    val ridgeScore: Float,   // Ridge regression
    val pcaScore: Float      // PCA reconstruction error
) : Parcelable {
    /**
     * Get the score to save to patient database.
     * This is the AE score.
     */
    fun getScoreForDatabase(): Double {
        return if (aeScore.isNaN()) 0.0 else aeScore.toDouble()
    }
    
    /**
     * Get average of available scores (for display only).
     */
    fun getAverageScore(): Float {
        val scores = listOf(aeScore, ridgeScore, pcaScore).filter { !it.isNaN() }
        return if (scores.isNotEmpty()) scores.average().toFloat() else 50f
    }
    
    companion object {
        fun default() = ScoringResult(
            aeScore = Float.NaN,
            ridgeScore = Float.NaN,
            pcaScore = Float.NaN
        )
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScoringResult
        return aeScore == other.aeScore
    }

    override fun hashCode(): Int = aeScore.hashCode()
}
