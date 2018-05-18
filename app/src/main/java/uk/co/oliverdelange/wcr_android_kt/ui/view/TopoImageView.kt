package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import uk.co.oliverdelange.wcr_android_kt.model.Route

class TopoImageView(c: Context, a: AttributeSet) : TouchImageView(c, a) {
    val routes: MutableList<Route> = mutableListOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        routes.forEach {
            //FIXME Logic on draw - prepopulate paths somewhere
            canvas.drawPath(it.getRoutePath(), paint)
        }
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