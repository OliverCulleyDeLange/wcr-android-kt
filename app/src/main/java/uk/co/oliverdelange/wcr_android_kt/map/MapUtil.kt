package uk.co.oliverdelange.wcr_android_kt.map

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import uk.co.oliverdelange.wcr_android_kt.view.map.MAP_ANIMATION_DURATION

fun GoogleMap.animate(latlng: LatLng, zoom: Float) {
    this.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom), MAP_ANIMATION_DURATION, null)
}

fun GoogleMap.animate(latLngBounds: LatLngBounds) {
    this.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100), MAP_ANIMATION_DURATION, null)
}
