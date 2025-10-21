package com.wqh.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class MyOverlayView(context: Context) : View(context) {
    private var y1: Float? = null
    private var y2: Float? = null

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f
//        isAntiAlias = true //抗锯齿
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        y1?.let {
            canvas.drawLine(0f, it, width.toFloat(), it, paint)
        }

        y2?.let {
            canvas.drawLine(0f, it, width.toFloat(), it, paint)
        }
        //canvas.drawLine(0f, 0f, 0f, height.toFloat(), paint) //左线
    }

    fun redraw(y1: Float?, y2: Float?) {
        if (this.y1 == y1 && this.y2 == y2) {
            return
        }
        this.y1 = y1
        this.y2 = y2
        invalidate()
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
