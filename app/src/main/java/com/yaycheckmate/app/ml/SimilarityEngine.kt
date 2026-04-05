package com.yaycheckmate.app.ml

import kotlin.math.sqrt

/**
 * Cosine similarity between L2-normalized vectors equals their dot product.
 * Thresholds are tuned for MobileNet classification logits used as embedding proxies;
 * adjust if you swap in a dedicated feature-vector TFLite model.
 */
object SimilarityEngine {

    /** Below this: treat as noise — no “metal detector” pulse. */
    const val THRESHOLD_LOW: Float = 0.52f

    /** Weak / medium pulse — “possible match”. */
    const val THRESHOLD_MEDIUM: Float = 0.66f

    /** Strong pulse — “object found nearby”. */
    const val THRESHOLD_HIGH: Float = 0.78f

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        if (n == 0) return 0f
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = sqrt(na) * sqrt(nb)
        if (denom <= 1e-6f) return 0f
        return (dot / denom).coerceIn(-1f, 1f)
    }

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (x in vector) sum += x * x
        val norm = sqrt(sum)
        if (norm <= 1e-6f) return vector.clone()
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }

    /**
     * Maps combined similarity into a 0–100 gauge for the UI.
     *
     * **Why not “real” match %:** We use ImageNet classifier activations as a proxy embedding.
     * For the *same* object under a new background / angle, cosine scores often land around
     * **0.15–0.45**, not 0.8+. A naive 0.45–0.92 map therefore sticks at **0%** most of the time.
     * This curve stretches the band where those scores actually occur so the meter reacts.
     */
    fun toDisplayPercent(similarity: Float): Int {
        val lo = 0.06f
        val hi = 0.42f
        val t = (similarity - lo) / (hi - lo)
        return (t * 100f).toInt().coerceIn(0, 100)
    }
}
