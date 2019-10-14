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
    val paths = mutableMapOf(Pair(0, path))
    val routes = mutableMapOf<Int, Route>()
    private var currX: Float = 0f
    private var currY: Float = 0f
    private var drawMode = false
    private var selectedRoute: Int = -1

    fun setDrawing(newDrawMode: Boolean?) {
        drawMode = newDrawMode ?: false
        enableTouch = newDrawMode?.not() ?: true
    }

    fun refresh() {
        Timber.d("Refreshing topo drawings")
        invalidate()
    }

    fun undoAction() {
        if (paths.size > selectedRoute) {
            paths[selectedRoute]?.undoAction()
            invalidate()
        }
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
        routes.forEach { (routeFragmentId, route) ->
            paths[routeFragmentId]?.let { routePath ->
                val routePoints = routePath.actionStack.flatten().toSet()
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
        if (path.actionStack.isEmpty()) {
            path.moveTo(x, y)
            // Hack to get the white circle for initial tap
            path.lineTo(x + 1, y)
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
        path.endAction()
    }

    fun onTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

//        Timber.d("${MotionEvent.actionToString(event.action)} at $x,$y")
        if (drawMode) {
            when (event.actionMasked) {
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
//                MotionEvent.ACTION_POINTER_DOWN -> {
//                }
//                MotionEvent.ACTION_POINTER_UP -> {
//                }
                else -> {
                    Timber.d("Unhandled touch event: ${MotionEvent.actionToString(event.action)} at $x,$y")
                }
            }
        }
        return true
    }

    init {
        setOnTouchListener { _, event ->
            onTouch(event)
        }
    }

    inner class PathCapture : Path() {
        private var actionIndex: Int = 0
        val actionStack = mutableListOf<MutableList<Pair<Float, Float>>>()

        fun capture(pair: Pair<Float, Float>) {
            if (actionIndex >= actionStack.size) {
                actionStack.add(actionIndex, mutableListOf())
            }
            val action = actionStack[actionIndex]
            action.add(pair)
        }

        fun undoAction() {
            if (actionStack.size >= actionIndex) {
                actionStack.removeAt(actionIndex - 1)
            }
            actionIndex--
        }

        fun endAction() {
            actionIndex++
        }

        override fun reset() {
            super.reset()
            actionStack.clear()
        }

        override fun moveTo(x: Float, y: Float) {
            super.moveTo(x, y)
            val imagePoint = transformCoordTouchToBitmap(x, y, true)
            capture(Pair(imagePoint.x / drawable.intrinsicWidth,
                    imagePoint.y / drawable.intrinsicHeight))
        }

        override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
            super.quadTo(x1, y1, x2, y2)
            val imagePoint = transformCoordTouchToBitmap(x2, y2, true)
            capture(Pair(imagePoint.x / drawable.intrinsicWidth,
                    imagePoint.y / drawable.intrinsicHeight))
        }

        override fun lineTo(x: Float, y: Float) {
            super.lineTo(x, y)
            val imagePoint = transformCoordTouchToBitmap(x, y, true)
            capture(Pair(imagePoint.x / drawable.intrinsicWidth,
                    imagePoint.y / drawable.intrinsicHeight))
        }
    }
}