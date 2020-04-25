package uk.co.oliverdelange.wcr_android_kt.map

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.Cluster
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.view.map.MAP_ANIMATION_DURATION
import uk.co.oliverdelange.wcr_android_kt.view.map.MAP_PADDING_INSET

fun GoogleMap.animate(latlng: LatLng, onFinished: () -> Unit = {}) {
    this.animateCamera(CameraUpdateFactory.newLatLng(latlng), MAP_ANIMATION_DURATION, cancelableCallback(onFinished))
}

fun GoogleMap.animate(cluster: Cluster<CragClusterItem>,  onFinished: () -> Unit = {}) {
    this.animate(getBoundsForLatLngs(cluster.items.map { it.position }), onFinished)
}

fun GoogleMap.animate(latlng: LatLng, zoom: Float,  onFinished: () -> Unit = {}) {
    this.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom), MAP_ANIMATION_DURATION, cancelableCallback(onFinished))
}

fun GoogleMap.animate(latLngBounds: LatLngBounds, onFinished: () -> Unit = {}) {
    this.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, MAP_PADDING_INSET), MAP_ANIMATION_DURATION, cancelableCallback(onFinished))
}

private fun cancelableCallback(onFinished: () -> Unit): GoogleMap.CancelableCallback {
    return object : GoogleMap.CancelableCallback {
        override fun onFinish() {
            Timber.d("Map animation finished")
            onFinished()
        }

        override fun onCancel() {
            Timber.d("Map animation cancelled")
        }
    }
}
