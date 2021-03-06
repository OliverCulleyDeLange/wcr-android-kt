package uk.co.oliverdelange.wcr_android_kt.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapMode.SUBMIT_CRAG_MODE
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapMode.SUBMIT_SECTOR_MODE
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel

class CragClusterItem(val location: Location) : ClusterItem {

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

class CustomRenderer(val vm: MapViewModel?, context: Context, map: GoogleMap, clusterManager: ClusterManager<CragClusterItem>)
    : DefaultClusterRenderer<CragClusterItem>(context, map, clusterManager) {

    private val iconHelper = IconHelper(context)

    override fun onBeforeClusterItemRendered(mapMarker: CragClusterItem, markerOptions: MarkerOptions) {
    }

    override fun onClusterItemRendered(clusterItem: CragClusterItem, marker: Marker) {
        if (listOf(SUBMIT_CRAG_MODE, SUBMIT_SECTOR_MODE).contains(vm?.mapMode?.value)) {
            iconHelper.setMarkerIcon(marker, Icon.CRAG_DIMMED, clusterItem.location.name)
        } else {
            iconHelper.setMarkerIcon(marker, Icon.CRAG, clusterItem.location.name)
        }
    }

    override fun shouldRenderAsCluster(cluster: Cluster<CragClusterItem>): Boolean {
        return cluster.size > 5
    }
}