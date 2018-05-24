package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.Route

const val DRAW_TOLERANCE = 5f

class PaintableTopoImageView(c: Context, att: AttributeSet) : TouchImageView(c, att) {

    private var path = PathCapture()
    val paths: MutableMap<Int, PathCapture> = mutableMapOf(Pair(0, path))
    val routes = mutableMapOf<Int, Route>()
    private var currX: Float = 0f
    private var currY: Float = 0f

    private var selectedRoute: Int = -1

    fun refresh() {
        Timber.d("Refreshing topo drawings")
        invalidate()
    }

    fun controlPath(routeFragmentId: Int, route: Route) {
        if (!paths.containsKey(routeFragmentId)) paths[routeFragmentId] = PathCapture()
        paths[routeFragmentId]?.let { path = it }
        routes[routeFragmentId] = route
        selectedRoute = routeFragmentId
        Timber.d("Controlling topo route path for Fragment with ID $routeFragmentId")
        invalidate()
    }

    fun removePath(id: Int?) {
        paths.remove(id)
        routes.remove(id)
        Timber.d("Removing topo route path $id")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.concat(matrix)
        routes.forEach { routeFragmentId, route ->
            val routePath = paths[routeFragmentId]
            routePath?.let { routePath ->
                val routePoints = routePath.capture.toSet()
                val scaledRoutePath = scalePath(routePoints)
                when (route.grade?.colour) {
                    GradeColour.GREEN -> canvas.drawPath(scaledRoutePath, greenRoutePaint)
                    GradeColour.ORANGE -> canvas.drawPath(scaledRoutePath, orangeRoutePaint)
                    GradeColour.RED -> canvas.drawPath(scaledRoutePath, redRoutePaint)
                    GradeColour.BLACK -> canvas.drawPath(scaledRoutePath, blackRoutePaint)
                }
                if (selectedRoute == routeFragmentId) canvas.drawPath(scaledRoutePath, selectedRoutePaint)
            }
        }
    }

    private fun scalePath(routePoints: Set<Pair<Float, Float>>): Path {
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
            val imagePoint = transformCoordTouchToBitmap(x, y, true)
            capture.add(Pair(imagePoint.x / drawable.intrinsicWidth,
                    imagePoint.y / drawable.intrinsicHeight))
        }

        override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
            super.quadTo(x1, y1, x2, y2)
            val imagePoint = transformCoordTouchToBitmap(x2, y2, true)
            capture.add(Pair(imagePoint.x / drawable.intrinsicWidth,
                    imagePoint.y / drawable.intrinsicHeight))
        }

        override fun lineTo(x: Float, y: Float) {
            super.lineTo(x, y)
            val imagePoint = transformCoordTouchToBitmap(x, y, true)
            capture.add(Pair(imagePoint.x / drawable.intrinsicWidth,
                    imagePoint.y / drawable.intrinsicHeight))
        }
    }
}