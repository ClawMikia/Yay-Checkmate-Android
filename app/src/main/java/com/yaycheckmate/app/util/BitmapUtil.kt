package com.yaycheckmate.app.util

import android.graphics.Bitmap
import android.graphics.Rect

object BitmapUtil {

    fun cropSafe(source: Bitmap, rect: Rect): Bitmap {
        val left = rect.left.coerceIn(0, source.width - 1)
        val top = rect.top.coerceIn(0, source.height - 1)
        val right = rect.right.coerceIn(left + 1, source.width)
        val bottom = rect.bottom.coerceIn(top + 1, source.height)
        val w = right - left
        val h = bottom - top
        return Bitmap.createBitmap(source, left, top, w, h)
    }

    fun centerCropFraction(source: Bitmap, fraction: Float = 0.72f): Bitmap {
        val f = fraction.coerceIn(0.2f, 1f)
        val cw = (source.width * f).toInt().coerceAtLeast(1)
        val ch = (source.height * f).toInt().coerceAtLeast(1)
        val left = (source.width - cw) / 2
        val top = (source.height - ch) / 2
        return Bitmap.createBitmap(source, left, top, cw, ch)
    }
}
