package uk.co.oliverdelange.wcr_android_kt.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import uk.co.oliverdelange.wcr_android_kt.model.Location

class CragClusterItem(private val location: Location) : ClusterItem {

    override fun getSnippet(): String {
        return location.name
    }

    override fun getTitle(): String {
        return location.name
    }

    override fun getPosition(): LatLng {
        return location.latlng
    }

}