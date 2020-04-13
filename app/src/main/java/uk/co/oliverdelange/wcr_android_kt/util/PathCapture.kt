package uk.co.oliverdelange.wcr_android_kt.util

//class PathCapture(private val transformCoords: (Float, Float) -> Pair<Float, Float>) {
//    private var _actionIndex: Int = 0
//    private val _actionStack = mutableListOf<PathSegment>()
//
//    fun getPoints() = _actionStack.flatMap{it.points}
//    fun isEmpty() = _actionStack.isEmpty()
//
//    fun undoAction() {
//        if (_actionStack.size >= _actionIndex && _actionIndex > 0) {
//            _actionStack.removeAt(_actionIndex - 1)
//            _actionIndex--
//        }
//    }
//
//    fun endAction() {
//        _actionIndex++
//    }
//
//    fun lineTo(x: Float, y: Float) {
//        if (_actionIndex >= _actionStack.size) {
//            _actionStack.add(_actionIndex, PathSegment())
//        }
//        _actionStack[_actionIndex].addPoint(transformCoords(x, y))
//    }
//}