package uk.co.oliverdelange.wcr_android_kt.util

import android.graphics.Path

class PathCapture(private val transformCoords: (Float, Float) -> Pair<Float, Float>) : Path() {
    private var actionIndex: Int = 0
    val actionStack = mutableListOf<MutableList<Pair<Float, Float>>>()

    fun undoAction() {
        if (actionStack.size >= actionIndex && actionIndex > 0) {
            actionStack.removeAt(actionIndex - 1)
            actionIndex--
        }
    }

    fun endAction() {
        actionIndex++
    }

    private fun capture(pair: Pair<Float, Float>) {
        if (actionIndex >= actionStack.size) {
            actionStack.add(actionIndex, mutableListOf())
        }
        val action = actionStack[actionIndex]
        action.add(pair)
    }

    override fun reset() {
        super.reset()
        actionStack.clear()
    }

    override fun moveTo(x: Float, y: Float) {
        super.moveTo(x, y)
        capture(transformCoords(x, y))
    }

    override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        super.quadTo(x1, y1, x2, y2)
        capture(transformCoords(x2, y2))
    }

    override fun lineTo(x: Float, y: Float) {
        super.lineTo(x, y)
        capture(transformCoords(x, y))
    }
}