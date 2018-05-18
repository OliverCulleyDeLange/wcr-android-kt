package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import timber.log.Timber

const val POINT_CAPTURE_TOLERANCE = 50f
const val DRAW_TOLERANCE = 5f

class PaintableTouchImageView : TouchImageView {
    constructor(c: Context) : super(c)
    constructor(c: Context, att: AttributeSet) : super(c, att) {
        setOnTouchListener { v, event ->
            onTouch(event)
        }
    }

    constructor(c: Context, att: AttributeSet, defStyle: Int) : super(c, att, defStyle)

    var drawnOnBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val path: Path = Path()
    private val bitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
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
    private var currX: Float = 0f
    private var currY: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Timber.d("TopoImage size changed: w:$w, h:$h")
        drawnOnBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(drawnOnBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(drawnOnBitmap, 0f, 0f, bitmapPaint)
        canvas.drawPath(path, paint)
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
//        canvas?.drawPath(path, paint)
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
}
