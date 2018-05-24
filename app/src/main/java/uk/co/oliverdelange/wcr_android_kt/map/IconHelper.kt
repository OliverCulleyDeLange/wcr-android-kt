package uk.co.oliverdelange.wcr_android_kt.map

import android.content.Context
import android.graphics.Bitmap
import android.support.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ui.IconGenerator
import uk.co.oliverdelange.wcr_android_kt.R


class IconHelper(val context: Context) {

    val iconGenerator: IconGenerator = IconGenerator(context)

    fun setMarkerIcon(marker: Marker, icon: Icon, text: String) {
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(getIcon(text, icon)))
    }

    fun setMarkerIcon(marker: Marker, icon: Icon) {
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(getIcon(icon)))
    }

    fun getIcon(text: String, icon: Icon): Bitmap {
        iconGenerator.setTextAppearance(icon.iconTextStyle)
        iconGenerator.setBackground(context.getDrawable(icon.iconDrawable))
        return iconGenerator.makeIcon(text)
    }

    fun getIcon(icon: Icon): Bitmap {
        iconGenerator.setTextAppearance(icon.iconTextStyle)
        iconGenerator.setBackground(context.getDrawable(icon.iconNoTextDrawable))
        return iconGenerator.makeIcon()
    }
}

enum class Icon constructor(@DrawableRes val iconDrawable: Int, @DrawableRes val iconNoTextDrawable: Int, @DrawableRes val iconTextStyle: Int) {
    CRAG(R.drawable.location_marker_crag, R.drawable.location_marker_crag_no_text, R.style.Wcr_GeneratedCragIconText),
    CRAG_DIMMED(R.drawable.location_marker_crag_dimmed, R.drawable.location_marker_crag_dimmed_no_text, R.style.Wcr_GeneratedCragIconTextDimmed),
    SECTOR(R.drawable.location_marker_sector, R.drawable.location_marker_sector_no_text, R.style.Wcr_GeneratedSectorIconText),
    SECTOR_DIMMED(R.drawable.location_marker_sector_dimmed, R.drawable.location_marker_sector_dimmed_no_text, R.style.Wcr_GeneratedSectorIconTextDimmed),
    SECTOR_SELECTED(R.drawable.location_marker_sector_selected, R.drawable.location_marker_sector_selected_no_text, R.style.Wcr_GeneratedSectorIconText)
}