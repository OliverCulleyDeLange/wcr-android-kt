package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import uk.co.oliverdelange.wcr_android_kt.model.Route

//TODO Open source this extension?
class TopoImageView(c: Context, a: AttributeSet) : TouchImageView(c, a) {

    val routes: MutableList<Route> = mutableListOf()
    val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(matrix)
        routes.forEach {
            path.reset()
            //FIXME Logic on draw - prepopulate paths somewhere
            canvas.drawPath(getRoutePath(path, it.path, drawable.intrinsicWidth, drawable.intrinsicHeight), paint)
        }
        canvas.restore()
        printMatrixInfo()
    }

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
}

fun getRoutePath(rtn: Path, path: Set<Pair<Float, Float>>?, width: Int, height: Int): Path {
    if (path != null && path.size > 1) {
        val iterator = path.iterator()
        val firstPoint = iterator.next()
        rtn.moveTo((firstPoint.first * width), firstPoint.second * height)
        while (iterator.hasNext()) {
            val next = iterator.next()
            rtn.lineTo(next.first * width, next.second * height)
        }
    }
    return rtn
}