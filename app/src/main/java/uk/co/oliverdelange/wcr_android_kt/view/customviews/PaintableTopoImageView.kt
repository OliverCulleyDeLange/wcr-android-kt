package uk.co.oliverdelange.wcr_android_kt.view.customviews

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

const val DRAW_TOLERANCE = 5f

class PaintableTopoImageView(c: Context, att: AttributeSet) : TouchImageView(c, att) {

//    private val paths = mutableMapOf<Int, PathCapture>()
//    private val routes = mutableMapOf<Int, Route>()
//    private var selectedRoute: Int = -1
//    private var currX: Float = 0f
//    private var currY: Float = 0f
    private var drawMode = false

    fun setDrawing(newDrawMode: Boolean?) {
        drawMode = newDrawMode ?: false
        enableTouch = newDrawMode?.not() ?: true
    }

//    fun refresh() {
//        Timber.v("Refreshing topo drawings")
//        invalidate()
//    }

//    fun undoAction() {
//        if (paths.containsKey(selectedRoute)) {
//            currentPath().undoAction()
//            invalidate()
//        }
//    }

//    fun controlPath(routeFragmentId: Int, route: Route) {
//        selectedRoute = routeFragmentId
//        routes[routeFragmentId] = route
//        Timber.d("Controlling topo route path for Fragment with ID $routeFragmentId")
//        invalidate()
//    }

//    fun removePath(id: Int?) {
//        paths.remove(id)
//        routes.remove(id)
//        Timber.d("Removing topo route path $id")
//        invalidate()
//    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.concat(matrix)
//        routes.forEach { (routeFragmentId, route) ->
//            paths[routeFragmentId]?.let { routePath ->
//                val routePoints = routePath.getPoints()
//                val scaledRoutePath = scalePath(routePoints)
//                when (route.grade?.colour) {
//                    GradeColour.GREEN -> canvas.drawPath(scaledRoutePath, greenRoutePaint)
//                    GradeColour.ORANGE -> canvas.drawPath(scaledRoutePath, orangeRoutePaint)
//                    GradeColour.RED -> canvas.drawPath(scaledRoutePath, redRoutePaint)
//                    GradeColour.BLACK -> canvas.drawPath(scaledRoutePath, blackRoutePaint)
//                }
//                if (selectedRoute == routeFragmentId) canvas.drawPath(scaledRoutePath, selectedRoutePaint)
//            }
//        }
    }

//    private fun newPathCapture(): PathCapture {
//        return PathCapture { x: Float, y: Float ->
//            val transform = transformCoordTouchToBitmap(x, y, true)
//            Pair(transform.x / drawable.intrinsicWidth,
//                    transform.y / drawable.intrinsicHeight)
//        }
//    }
//
//    private fun currentPath(): PathCapture {
//        if (!paths.containsKey(selectedRoute)) {
//            paths[selectedRoute] = newPathCapture()
//        }
//        return paths[selectedRoute]!!
//    }

//    private fun scalePath(routePoints: Collection<Pair<Float, Float>>): Path {
//        val routePath = Path()
//        if (routePoints.size > 1) {
//            val iterator = routePoints.iterator()
//            val firstPoint = iterator.next()
//            routePath.moveTo(firstPoint.first * drawable.intrinsicWidth, firstPoint.second * drawable.intrinsicHeight)
//            while (iterator.hasNext()) {
//                val next = iterator.next()
//                routePath.lineTo(next.first * drawable.intrinsicWidth, next.second * drawable.intrinsicHeight)
//            }
//        }
//        return routePath
//    }

//    private fun touch_start(x: Float, y: Float) {
//        currentPath().lineTo(x, y)
//        // Hack to get the white circle for initial tap
//        if (currentPath().isEmpty()) currentPath().lineTo(x + 1, y)
//        currX = x
//        currY = y
//    }
//
//    private fun touch_move(x: Float, y: Float) {
//        val dx = abs(x - currX)
//        val dy = abs(y - currY)
//        if (dx >= DRAW_TOLERANCE || dy >= DRAW_TOLERANCE) {
//            currentPath().lineTo((x + currX) / 2, (y + currY) / 2)
//            currX = x
//            currY = y
//        }
//    }
//
//    private fun touch_up() {
//        currentPath().lineTo(currX, currY)
//        currentPath().endAction()
//    }
}