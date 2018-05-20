package uk.co.oliverdelange.wcr_android_kt.ui.view

import android.graphics.*

val baseRoutePaint = Paint().also {
    it.isAntiAlias = true
    it.isDither = true
    it.style = Paint.Style.STROKE
    it.strokeJoin = Paint.Join.ROUND
    it.strokeCap = Paint.Cap.ROUND
    it.strokeWidth = 10f
    it.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
}

val greenRoutePaint = Paint(baseRoutePaint).also {
    it.setARGB(200, 80, 220, 20)
}
val orangeRoutePaint = Paint(baseRoutePaint).also {
    it.setARGB(200, 220, 110, 0)
}
val redRoutePaint = Paint(baseRoutePaint).also {
    it.setARGB(200, 255, 0, 0)
}
val blackRoutePaint = Paint(baseRoutePaint).also {
    it.setARGB(200, 0, 0, 0)
}
private val pathDot = Path().apply {
    addCircle(0f, 0f, 8f, Path.Direction.CW)
}
val selectedRoutePaint = Paint(baseRoutePaint).also {
    it.pathEffect = PathDashPathEffect(pathDot, 50f, 0f, PathDashPathEffect.Style.TRANSLATE)
    it.strokeJoin = Paint.Join.MITER
    it.strokeCap = Paint.Cap.SQUARE
    it.setARGB(255, 255, 255, 255)
}