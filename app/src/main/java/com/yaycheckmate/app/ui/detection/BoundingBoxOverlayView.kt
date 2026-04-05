package com.yaycheckmate.app.ui.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Draws a normalized detection box (0–1 relative to analyzer frame) on top of the preview.
 */
class BoundingBoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        color = Color.parseColor("#9900FF")
    }

    private var normLeft = 0f
    private var normTop = 0f
    private var normRight = 0f
    private var normBottom = 0f
    private var hasBox = false

    fun clearBox() {
        hasBox = false
        invalidate()
    }

    fun setNormalizedBox(left: Float, top: Float, right: Float, bottom: Float) {
        normLeft = left
        normTop = top
        normRight = right
        normBottom = bottom
        hasBox = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!hasBox) return
        val rect = RectF(
            normLeft * width,
            normTop * height,
            normRight * width,
            normBottom * height,
        )
        canvas.drawRect(rect, paint)
    }
}
