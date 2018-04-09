package uk.co.oliverdelange.wcr_android_kt.binding

import android.databinding.BindingAdapter
import android.view.View
import android.view.View.*
import android.widget.TextView
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*

@set:BindingAdapter("show")
var View.visibleOrGone
    get() = visibility == VISIBLE
    set(value) {
        visibility = if (value) VISIBLE else GONE
    }

@set:BindingAdapter("invisible")
var View.invisible
    get() = visibility == INVISIBLE
    set(value) {
        visibility = if (value) INVISIBLE else VISIBLE
    }

@set:BindingAdapter("gone")
var View.gone
    get() = visibility == GONE
    set(value) {
        visibility = if (value) GONE else VISIBLE
    }

@BindingAdapter("mapModeTextColour")
fun setmapModeTextColour(textView: TextView, mapMode: MapMode) {
    val context = textView.context
    val resources = context.resources
    when (mapMode) {
        DEFAULT -> textView.setTextColor(resources.getColor(R.color.text_grey_dark))
        CRAG -> textView.setTextColor(resources.getColor(R.color.crag_accent))
        SECTOR -> textView.setTextColor(resources.getColor(R.color.sector_accent))
    }
}