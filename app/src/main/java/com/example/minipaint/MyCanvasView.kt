package com.example.minipaint

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

private const val STROKE_WIDTH = 12f

class MyCanvasView(context: Context?) : View(context) {

    //Caching what has been drawing before
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)
    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    //Set up the paint with which to draw
    private val paint = Paint().apply {
        color = drawColor
        //Smooth out edges
        isAntiAlias = true
        //Reduce the visual artifacts
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = STROKE_WIDTH
    }

    //The path is being drawn when following the user's touch on the screen
    private var path = Path()
    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    //Cache the last x and y values
    private var currentX = 0f
    private var currentY = 0f

    //No need to draw every pixel, interpolate a path between points for much better performance
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        //Leaving the old Bitmaps around produce memory leak, to fix this, recycle extraBitmap before
        //creating the new one
        if (::extraBitmap.isInitialized) extraBitmap.recycle()

        //Recommended Bitmap.Config.ARGB_8888
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY

    }

    private fun touchMove() {
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)

        if (dx >= touchTolerance || dy >= touchTolerance) {
            //QuadTo() adds a quadratic bezier from the last point,
            //approaching control point (x1,y1) and ending at(x2,y2)
            path.quadTo(
                currentX,
                currentY,
                (motionTouchEventX + currentX) / 2,
                (motionTouchEventY + currentY) / 2
            )
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            //Draw the path in the extra bitmap to cache it
            extraCanvas.drawPath(path, paint)
            invalidate()
        }
    }

    private fun touchUp() {
        //Reset the path so it doesn't get draw again.
        path.reset()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event != null) {
            motionTouchEventX = event.x
            motionTouchEventY = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> touchStart()
                MotionEvent.ACTION_MOVE -> touchMove()
                MotionEvent.ACTION_UP -> touchUp()
            }
        }
        return true
    }

}