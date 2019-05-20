package uk.co.oliverdelange.wcr_android_kt.binding

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.View.*
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*
import uk.co.oliverdelange.wcr_android_kt.ui.submit.MAX_TOPO_SIZE_PX

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
fun setmapModeTextColour(textView: TextView, mapMode: MapMode?) {
    val context = textView.context
    when (mapMode) {
        CRAG_MODE -> textView.setTextColor(ContextCompat.getColor(context, R.color.crag_accent))
        SECTOR_MODE, TOPO_MODE -> textView.setTextColor(ContextCompat.getColor(context, R.color.sector_accent))
        else -> textView.setTextColor(ContextCompat.getColor(context, R.color.text_grey_dark))
    }
}

@BindingAdapter("imageUrl", "placeholder")
fun loadImageUrl(view: ImageView, imageUrl: String, placeholder: Drawable) {
    Picasso.get()
            .load(imageUrl)
            .placeholder(placeholder)
            .into(view)
}

@BindingAdapter("imageUri")
fun loadTopoSubmissionImageFromUri(view: ImageView, uri: Uri?) {
    Picasso.get()
            .load(uri)
            .resize(MAX_TOPO_SIZE_PX, MAX_TOPO_SIZE_PX)
            .centerInside()
            .into(view)
}

@BindingAdapter("imageUri", "placeholder")
fun loadImageUri(view: ImageView, uri: String?, placeholder: Drawable) {
    Picasso.get()
            .load(Uri.parse(uri))
            .placeholder(placeholder)
            .into(view)
}

@BindingAdapter("android:src")
fun setImageResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
}

@BindingAdapter("android:textColor")
fun setTextGradeColour(textView: TextView, gradeColour: GradeColour) {
    @ColorInt val colour: Int = when (gradeColour) {
        GradeColour.GREEN -> R.color.map_dragbar_climb_grades_green
        GradeColour.ORANGE -> R.color.map_dragbar_climb_grades_orange
        GradeColour.RED -> R.color.map_dragbar_climb_grades_red
        GradeColour.BLACK -> R.color.map_dragbar_climb_grades_black
    }
    textView.setTextColor(ContextCompat.getColor(textView.context, colour))
}

@BindingAdapter("android:text")
fun setTextGradeColour(textView: TextView, int: Int) {
    textView.text = int.toString()
}