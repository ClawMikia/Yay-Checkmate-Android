package com.yaycheckmate.app.ml

import android.graphics.Bitmap

/**
 * Cheap color layout descriptor (RGB histogram) to complement classifier embeddings.
 * Same object / similar lighting tends to correlate even when ImageNet logits drift.
 */
object ColorSignature {

    private const val SIDE = 56
    private const val BINS = 8

    fun fromBitmap(source: Bitmap): FloatArray {
        val small =
            if (source.width == SIDE && source.height == SIDE) source
            else Bitmap.createScaledBitmap(source, SIDE, SIDE, true)
        val vec = FloatArray(3 * BINS)
        val pixels = IntArray(SIDE * SIDE)
        small.getPixels(pixels, 0, SIDE, 0, 0, SIDE, SIDE)
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            vec[(r * BINS) / 256] += 1f
            vec[BINS + (g * BINS) / 256] += 1f
            vec[2 * BINS + (b * BINS) / 256] += 1f
        }
        if (small !== source) small.recycle()
        var sum = 0f
        for (x in vec) sum += x
        if (sum > 0f) {
            for (i in vec.indices) vec[i] /= sum
        }
        return SimilarityEngine.l2Normalize(vec)
    }
}
