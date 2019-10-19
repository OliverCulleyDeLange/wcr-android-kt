package uk.co.oliverdelange.wcr_android_kt.map

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.view.map.MAP_ANIMATION_DURATION

fun GoogleMap.animate(latlng: LatLng, zoom: Float) {
    this.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom), MAP_ANIMATION_DURATION, null)
}

fun GoogleMap.animate(latLngBounds: LatLngBounds, onFinished: () -> Unit) {
    this.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100), MAP_ANIMATION_DURATION, object : GoogleMap.CancelableCallback {
        override fun onFinish() {
            Timber.d("Map animation finished")
            onFinished()
        }

        override fun onCancel() {
            Timber.d("Map animation cancelled")
        }
    })
}
