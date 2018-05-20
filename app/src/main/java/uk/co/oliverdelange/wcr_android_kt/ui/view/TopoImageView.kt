package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.Route

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
                GradeColour.GREEN -> canvas.drawPath(routePath, greenRoutePaint)
                GradeColour.ORANGE -> canvas.drawPath(routePath, orangeRoutePaint)
                GradeColour.RED -> canvas.drawPath(routePath, redRoutePaint)
                GradeColour.BLACK -> canvas.drawPath(routePath, blackRoutePaint)
            }
            if (selectedRoute?.id == it.id) canvas.drawPath(routePath, selectedRoutePaint)
        }
        printMatrixInfo()
    }


}