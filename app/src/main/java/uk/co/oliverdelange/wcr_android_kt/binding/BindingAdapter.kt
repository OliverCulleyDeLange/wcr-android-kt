package uk.co.oliverdelange.wcr_android_kt.binding

import android.graphics.Bitmap
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
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.view.customviews.PaintableTopoImageView
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MAX_TOPO_SIZE_PX
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapMode
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapMode.*

@set:BindingAdapter("show")
var View.visibleOrGone
    get() = visibility == VISIBLE
    set(value) {
        Timber.v("Binding 'show'")
        visibility = if (value) VISIBLE else GONE
    }

@set:BindingAdapter("invisible")
var View.invisible
    get() = visibility == INVISIBLE
    set(value) {
        Timber.v("Binding 'invisible'")
        visibility = if (value) INVISIBLE else VISIBLE
    }

@set:BindingAdapter("gone")
var View.gone
    get() = visibility == GONE
    set(value) {
        Timber.v("Binding 'gone'")
        visibility = if (value) GONE else VISIBLE
    }

@BindingAdapter("mapModeTextColour")
fun setmapModeTextColour(textView: TextView, mapMode: MapMode?) {
    Timber.v("Binding 'mapModeTextColour'")
    val context = textView.context
    when (mapMode) {
        CRAG_MODE -> textView.setTextColor(ContextCompat.getColor(context, R.color.crag_accent))
        SECTOR_MODE, TOPO_MODE -> textView.setTextColor(ContextCompat.getColor(context, R.color.sector_accent))
        else -> textView.setTextColor(ContextCompat.getColor(context, R.color.text_grey_dark))
    }
}

@BindingAdapter("imageUrl", "placeholder")
fun loadImageUrl(view: ImageView, imageUrl: String, placeholder: Drawable) {
    Timber.v("Binding 'imageUrl' && 'placeholder'")
    Picasso.get()
            .load(imageUrl)
            .placeholder(placeholder)
            .into(view)
}

@BindingAdapter("imageUri")
fun loadTopoSubmissionImageFromUri(view: ImageView, uri: Uri?) {
    Timber.v("Binding 'imageUri'")
    Picasso.get()
            .load(uri)
            .resize(MAX_TOPO_SIZE_PX, MAX_TOPO_SIZE_PX)
            .centerInside()
            .into(view)
}

@BindingAdapter("imageUri", "placeholder")
fun loadImageUri(view: ImageView, uri: String?, placeholder: Drawable) {
    Timber.v("Binding 'imageUri' && 'placeholder'")
    Picasso.get()
            .load(Uri.parse(uri))
            .placeholder(placeholder)
            .into(view)
}

@BindingAdapter("drawMode")
fun loadDrawMode(view: PaintableTopoImageView, drawMode: Boolean?) {
    Timber.v("Binding 'drawMode'")
    view.setDrawing(drawMode)
}

@BindingAdapter("imageBitmap")
fun setImageBitmap(imageView: ImageView, bitmap: Bitmap?) {
    Timber.v("Binding 'imageBitmap'")
    bitmap?.let {
        imageView.setImageBitmap(it)
    }
}

@BindingAdapter("android:src")
fun setImageResource(imageView: ImageView, resource: Int) {
    Timber.v("Binding 'android:src'")
    imageView.setImageResource(resource)
}

@BindingAdapter("android:textColor")
fun setTextGradeColour(textView: TextView, gradeColour: GradeColour) {
    Timber.v("Binding 'android:textColor'")
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
    Timber.v("Binding 'android:text'")
    textView.text = int.toString()
}