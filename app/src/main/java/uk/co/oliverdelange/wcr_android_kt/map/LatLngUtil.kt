package uk.co.oliverdelange.wcr_android_kt.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

private val DEFAULT_BOUNDS = LatLngBounds.builder()
        .include(LatLng(58.547148, -7.824919))
        .include(LatLng(50.373360, 1.083633))
        .build()

//TODO Test me
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
    return builder.build()
}
