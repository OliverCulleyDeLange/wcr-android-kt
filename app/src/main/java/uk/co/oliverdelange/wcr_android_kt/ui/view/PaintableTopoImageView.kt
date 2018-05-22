package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
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
        invalidate()
    }

    fun controlPath(routeFragmentId: Int, route: Route) {
        if (!paths.containsKey(routeFragmentId)) paths[routeFragmentId] = PathCapture()
        paths[routeFragmentId]?.let { path = it }
        routes[routeFragmentId] = route
        selectedRoute = routeFragmentId
        invalidate()
    }

    fun removePath(id: Int?) {
        paths.remove(id)
        routes.remove(id)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        routes.forEach { routeFragmentId, route ->
            val routePath = paths[routeFragmentId]
            when (route.grade?.colour) {
                GradeColour.GREEN -> canvas.drawPath(routePath, greenRoutePaint)
                GradeColour.ORANGE -> canvas.drawPath(routePath, orangeRoutePaint)
                GradeColour.RED -> canvas.drawPath(routePath, redRoutePaint)
                GradeColour.BLACK -> canvas.drawPath(routePath, blackRoutePaint)
            }
            if (selectedRoute == routeFragmentId) canvas.drawPath(routePath, selectedRoutePaint)
        }
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
            capture.add(Pair(x / width, y / height))
        }

        override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
            super.quadTo(x1, y1, x2, y2)
            capture.add(Pair(x2 / width, y2 / height))
        }

        override fun lineTo(x: Float, y: Float) {
            super.lineTo(x, y)
            capture.add(Pair(x / width, y / height))
        }
    }
}