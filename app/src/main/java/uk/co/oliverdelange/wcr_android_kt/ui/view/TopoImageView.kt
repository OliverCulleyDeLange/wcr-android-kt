package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.Route


//TODO Open source this extension?
class TopoImageView(c: Context, a: AttributeSet) : TouchImageView(c, a) {

    var routes: List<Route> = emptyList()
        set(value) {
            field = value
            selectedRoute = routes[0]
        }

    var selectedRoute: Route? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.concat(matrix)
        routes.forEach {
            //FIXME Logic on draw - prepopulate paths somewhere
            val routePath = Path()
            val routePoints = it.path
            if (routePoints != null && routePoints.size > 1) {
                val iterator = routePoints.iterator()
                val firstPoint = iterator.next()
                routePath.moveTo(firstPoint.first * drawable.intrinsicWidth, firstPoint.second * drawable.intrinsicHeight)
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    routePath.lineTo(next.first * drawable.intrinsicWidth, next.second * drawable.intrinsicHeight)
                }
            }
            when (it.grade?.colour) {
                GradeColour.GREEN -> paint.setARGB(100, 80, 220, 20)
                GradeColour.ORANGE -> paint.setARGB(100, 220, 110, 0)
                GradeColour.RED -> paint.setARGB(100, 255, 0, 0)
                GradeColour.BLACK -> paint.setARGB(100, 0, 0, 0)
            }
            paint.pathEffect = null
            if (selectedRoute?.id == it.id) paint.pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
            canvas.drawPath(routePath, paint)
        }
        printMatrixInfo()
    }

    private var paint = Paint().also {
        it.isAntiAlias = true
        it.isDither = true
        it.style = Paint.Style.STROKE
        it.strokeJoin = Paint.Join.ROUND
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = 10f
        it.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }
}