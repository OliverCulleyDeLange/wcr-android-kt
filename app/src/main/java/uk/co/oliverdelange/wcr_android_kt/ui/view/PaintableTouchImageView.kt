package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent

const val DRAW_TOLERANCE = 5f

class PaintableTouchImageView(c: Context, att: AttributeSet) : TouchImageView(c, att) {

    private var path = PathCapture()
    val paths: MutableMap<Int, PathCapture> = mutableMapOf(Pair(0, path))
    private var currX: Float = 0f
    private var currY: Float = 0f
    private var paint = Paint().also {
        it.isAntiAlias = true
        it.isDither = true
        it.color = -0x10000
        it.style = Paint.Style.STROKE
        it.strokeJoin = Paint.Join.ROUND
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = 20f
        it.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        it.alpha = 0x80
    }


    fun controlPath(id: Int) {
        if (!paths.containsKey(id)) paths[id] = PathCapture()
        paths[id]?.let { path = it }
    }

    fun removePath(id: Int?) {
        paths.remove(id)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paths.forEach {
            canvas.drawPath(it.value, paint)
        }
    }

    private fun touch_start(x: Float, y: Float) {
        if (path.isEmpty) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        currX = x
        currY = y
    }

    private fun touch_move(x: Float, y: Float) {
        val dx = Math.abs(x - currX)
        val dy = Math.abs(y - currY)
        if (dx >= DRAW_TOLERANCE || dy >= DRAW_TOLERANCE) {
            path.quadTo(currX, currY, (x + currX) / 2, (y + currY) / 2)
            currX = x
            currY = y
        }
    }

    private fun touch_up() {
        path.lineTo(currX, currY)
    }

    fun onTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touch_move(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }
        }
        return true
    }

    fun setPaint(paint: Paint) {
        this.paint = paint
    }

    init {
        setOnTouchListener { v, event ->
            onTouch(event)
        }
    }

    inner class PathCapture : Path() {
        val capture = mutableSetOf<Pair<Float, Float>>()

        override fun reset() {
            super.reset()
            capture.clear()
        }

        override fun moveTo(x: Float, y: Float) {
            super.moveTo(x, y)
            capture.add(Pair(x / width, y / height))
        }

        override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
            super.quadTo(x1, y1, x2, y2)
            capture.add(Pair(x2 / width, y2 / height))
        }

        override fun lineTo(x: Float, y: Float) {
            super.lineTo(x, y)
            capture.add(Pair(x / width, y / height))
        }
    }
}