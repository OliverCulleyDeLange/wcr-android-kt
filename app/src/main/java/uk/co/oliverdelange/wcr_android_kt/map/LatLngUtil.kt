package uk.co.oliverdelange.wcr_android_kt.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil

private val DEFAULT_BOUNDS = LatLngBounds.builder()
        .include(LatLng(58.547148, -7.824919))
        .include(LatLng(50.373360, 1.083633))
        .build()
private const val PADDING_PERCENTAGE = 15.0


fun getBoundsForLatLngs(latLngs: Collection<LatLng>): LatLngBounds {
    var mostW: LatLng? = null
    var mostE: LatLng? = null
    fun updateMostWesterly(latLng: LatLng) {
        if (mostW == null || latLng.longitude < mostW!!.longitude) {
            mostW = latLng
        }
    }

    fun updateMostEasterly(latLng: LatLng) {
        if (mostE == null || latLng.longitude > mostE!!.longitude) {
            mostE = latLng
        }
    }
    if (latLngs.isEmpty()) return DEFAULT_BOUNDS
    val builder = LatLngBounds.builder()
    for (latLng in latLngs) {
        updateMostEasterly(latLng)
        updateMostWesterly(latLng)
        builder.include(latLng)
    }
    // This just adds x% padding on the east and west side of the bounds.
    if (mostW != null && mostE != null) {
        val distanceBetweenFurthest = SphericalUtil.computeDistanceBetween(mostE!!, mostW!!)
        // If there is only one marker, the distanceBetweenFurthest should be 0, so give a minimum zoom of 1km.
        val paddingDistance: Double = if (distanceBetweenFurthest > 1) distanceBetweenFurthest / 100 * PADDING_PERCENTAGE else 500.0
        val east = SphericalUtil.computeOffset(mostE, paddingDistance, 90.0)
        val west = SphericalUtil.computeOffset(mostW, paddingDistance, 270.0)
        builder.include(east)
        builder.include(west)
    }
    return builder.build()
}
