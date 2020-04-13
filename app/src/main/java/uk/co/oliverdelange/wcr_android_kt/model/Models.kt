package uk.co.oliverdelange.wcr_android_kt.model

import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.map.Icon

data class Location(val id: String? = null,
                    val parentLocationId: String? = null,
                    var name: String,
                    val latlng: LatLng,
                    val type: LocationType,
                    val uploaderId: String)

data class TopoAndRoutes(
        var topo: Topo,
        var routes: List<Route>
)

data class Topo(val id: String? = null,
                var locationId: String,
                var name: String,
                var image: String)

data class Route(val id: String? = null,
                 var topoId: String? = null,
                 var name: String? = null,
                 var grade: Grade? = null,
                 var type: RouteType? = null,
                 var description: String? = null,
                 var path: List<PathSegment>? = null)

class PathSegment(points: Collection<Pair<Float, Float>> = listOf()) {
    private val _points: MutableList<Pair<Float, Float>> = points.toMutableList()
    val points: List<Pair<Float, Float>>
        get() = _points.toList()

    fun addPoint(pair: Pair<Float, Float>) = _points.add(pair)
}

data class Grade(var string: String, var type: GradeType, var colour: GradeColour)

enum class LocationType(var icon: Icon) {
    CRAG(Icon.CRAG), SECTOR(Icon.SECTOR)
}

enum class RouteType constructor(@DrawableRes val icon: Int) {
    TRAD(R.drawable.ic_cam), SPORT(R.drawable.ic_quick_draw), BOULDERING(R.drawable.ic_boulder)
}

enum class GradeType {
    V, FONT, SPORT, TRAD
}

enum class GradeColour {
    GREEN, ORANGE, RED, BLACK
}

enum class SyncType {
    UPLOAD, DOWNLOAD
}
