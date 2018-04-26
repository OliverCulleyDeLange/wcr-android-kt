package uk.co.oliverdelange.wcr_android_kt.binding

import android.databinding.BindingAdapter
import android.view.View
import android.view.View.*
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import com.squareup.picasso.Picasso
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
        DEFAULT_MODE -> textView.setTextColor(resources.getColor(R.color.text_grey_dark))
        CRAG_MODE -> textView.setTextColor(resources.getColor(R.color.crag_accent))
        SECTOR_MODE -> textView.setTextColor(resources.getColor(R.color.sector_accent))
    }
}

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, imageUrl: String) {
    Picasso.with(view.context)
            .load(imageUrl)
            .placeholder(R.drawable.topo_placeholder)
            .into(view)
}

@BindingAdapter("spinner:highlighted")
fun setSpinnerHighlight(spinner: Spinner?, highlighted: Boolean) {
    val spinnerItem = spinner?.selectedView as TextView?
    spinnerItem?.let {
        if (highlighted) spinnerItem.setTextColor(spinnerItem.resources.getColor(R.color.dark_gray))
        else spinnerItem.setTextColor(spinnerItem.resources.getColor(R.color.text_grey_light))
    }
}

@BindingAdapter("android:src")
fun setImageResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
}