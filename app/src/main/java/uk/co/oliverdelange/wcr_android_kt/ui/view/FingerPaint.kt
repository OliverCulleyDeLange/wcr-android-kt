package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

const val POINT_CAPTURE_TOLERANCE = 50f
const val DRAW_TOLERANCE = 5f

class PaintingView(c: Context, attributeSet: AttributeSet) : View(c, attributeSet) {
    var drawnOnBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val path: Path = Path()
    private val bitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var currX: Float = 0f
    private var currY: Float = 0f
    //    private var originalBitmap: Bitmap? = null
    private var paint: Paint? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

//        val bmpWidth = originalBitmap?.width
//        val bmpHeight = originalBitmap?.height

//        val widthRatio = w.toFloat() / bmpWidth.toFloat()
//        val heightRatio = h.toFloat() / bmpHeight.toFloat()

//        val width: Int
//        val height: Int
//        if (widthRatio < heightRatio) {
//            width = Math.round(bmpWidth * widthRatio)
//            height = Math.round(bmpHeight * widthRatio)
//        } else {
//            width = Math.round(bmpWidth * heightRatio)
//            height = Math.round(bmpHeight * heightRatio)
//        }

//        drawnOnBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
//        if (originalBitmap == drawnOnBitmap) {
        // Bitmap is still immutable as no scaling has been done
//            drawnOnBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
//        }
        drawnOnBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(drawnOnBitmap)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(drawnOnBitmap, 0f, 0f, bitmapPaint)
        canvas.drawPath(path, paint)
    }

    private fun touch_start(x: Float, y: Float) {
        //showDialog();
        path.reset()
        path.moveTo(x, y)
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
        // commit the path to our offscreen
        canvas?.drawPath(path, paint)
        // kill this so we don't double draw
        path.reset()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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

//    fun setBackgroundBitmap(originalBitmap: Bitmap) {
//        this.originalBitmap = originalBitmap
//    }

    fun setPaint(paint: Paint) {
        this.paint = paint
    }

}
