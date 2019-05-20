package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.Route

class TopoImageView(c: Context, a: AttributeSet) : TouchImageView(c, a) {

    //TODO Better way of refreshing paths once image loaded?
    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        refreshPaths()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        refreshPaths()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        refreshPaths()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        refreshPaths()
    }

    var routes: List<Route> = emptyList()
        set(value) {
            field = value
            selectedRoute = if (routes.isNotEmpty()) routes[0] else null
        }

    val paths = mutableMapOf<String, Path>()

    var selectedRoute: Route? = null
        set(value) {
            field = value
            invalidate()
        }

    private fun refreshPaths() {
        paths.clear()
        routes.forEach {
            val routePoints = it.path
            val routeId = it.id
            if (routePoints != null && routePoints.size > 1 && routeId != null) {
                paths[routeId] = scalePath(routePoints)
            }
        }
    }

    private fun scalePath(routePoints: Set<Pair<Float, Float>>): Path {
        val routePath = Path()
        val iterator = routePoints.iterator()
        val firstPoint = iterator.next()
        routePath.moveTo(firstPoint.first * drawable.intrinsicWidth, firstPoint.second * drawable.intrinsicHeight)
        while (iterator.hasNext()) {
            val next = iterator.next()
            routePath.lineTo(next.first * drawable.intrinsicWidth, next.second * drawable.intrinsicHeight)
        }
        return routePath
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.concat(matrix)
        routes.forEach {
            if (paths.containsKey(it.id)) {
                paths[it.id]?.let { routePath ->
                    when (it.grade?.colour) {
                        GradeColour.GREEN -> canvas.drawPath(routePath, greenRoutePaint)
                        GradeColour.ORANGE -> canvas.drawPath(routePath, orangeRoutePaint)
                        GradeColour.RED -> canvas.drawPath(routePath, redRoutePaint)
                        GradeColour.BLACK -> canvas.drawPath(routePath, blackRoutePaint)
                    }
                    if (selectedRoute?.id == it.id) canvas.drawPath(routePath, selectedRoutePaint)
                }
            }
        }
    }
}