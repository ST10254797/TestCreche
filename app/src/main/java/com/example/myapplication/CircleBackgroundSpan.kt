package com.example.myapplication

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class CircleBackgroundSpan(
    private val backgroundColor: Int,
    private val radius: Float = 40f
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val oldColor = paint.color
        val oldStyle = paint.style

        paint.color = backgroundColor
        paint.style = Paint.Style.FILL

        val centerX = ((left + right) / 2).toFloat()
        val centerY = ((top + bottom) / 2).toFloat()

        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.color = oldColor
        paint.style = oldStyle
    }
}
