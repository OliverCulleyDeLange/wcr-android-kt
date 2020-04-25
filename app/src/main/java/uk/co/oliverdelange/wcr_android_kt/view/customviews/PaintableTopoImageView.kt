package uk.co.oliverdelange.wcr_android_kt.view.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.flattened
import uk.co.oliverdelange.wcr_android_kt.viewmodel.RouteViewModel

class PaintableTopoImageView(c: Context, att: AttributeSet) : TouchImageView(c, att) {
    init {
        // This touch listener transforms screen coords to relative drawable coords and outputs
        // to the given onDrawListener if the touchImageView is disabled
        setOnTouchListener { _, event ->
            val transform = transformCoordTouchToBitmap(event.x, event.y, true)
            val relX = transform.x / drawable.intrinsicWidth
            val relY = transform.y / drawable.intrinsicHeight

            if(!enableTouch) onDrawListener(relX, relY, event) else false
        }
    }

    private var routeVms: Collection<RouteViewModel>? = null
    private var onDrawListener: (x: Float, y: Float, event: MotionEvent) -> Boolean = { _, _, _ -> false }

    fun setDrawing(newDrawMode: Boolean?) {
        enableTouch = newDrawMode?.not() ?: true
    }
/**
 * Sets a listener that receives x,y coordinates as a percentage of the size of the image
 * eg: image size 100 x 10, x:50, y:8 would output x:0.5, y:0.8
 * */
    fun setOnDrawListener(listener: (x: Float, y: Float, event: MotionEvent) -> Boolean) {
        onDrawListener = listener
    }

    fun update(new: List<RouteViewModel>?) {
        routeVms = new
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.concat(matrix)
        routeVms?.forEach { vm ->
            val routePoints: Collection<Pair<Float, Float>> = vm.route.path?.flattened()
                    ?: emptyList()
            val scaledRoutePath = scalePath(routePoints)
            when (vm.route.grade?.colour) {
                GradeColour.GREEN -> canvas.drawPath(scaledRoutePath, greenRoutePaint)
                GradeColour.ORANGE -> canvas.drawPath(scaledRoutePath, orangeRoutePaint)
                GradeColour.RED -> canvas.drawPath(scaledRoutePath, redRoutePaint)
                GradeColour.BLACK -> canvas.drawPath(scaledRoutePath, blackRoutePaint)
            }
            if (vm.isActive) canvas.drawPath(scaledRoutePath, selectedRoutePaint)
        }
    }

    private fun scalePath(routePoints: Collection<Pair<Float, Float>>): Path {
        val routePath = Path()
        if (routePoints.size > 1) {
            val iterator = routePoints.iterator()
            val firstPoint = iterator.next()
            routePath.moveTo(firstPoint.first * drawable.intrinsicWidth, firstPoint.second * drawable.intrinsicHeight)
            while (iterator.hasNext()) {
                val next = iterator.next()
                routePath.lineTo(next.first * drawable.intrinsicWidth, next.second * drawable.intrinsicHeight)
            }
        }
        return routePath
    }
}