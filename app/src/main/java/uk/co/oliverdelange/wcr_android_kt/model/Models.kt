package uk.co.oliverdelange.wcr_android_kt.model

import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.map.Icon

data class Location(val id: Long? = null,
                    val firebaseId: DocumentReference? = null,
                    val parentLocation: Long? = null,
                    var name: String,
                    val latlng: LatLng,
                    val type: LocationType,
                    val uploaderId: String)

// Doesn't need a domain object yet
//data class LocationRouteInfo(var greens: Int = 0,
//                    var oranges: Int = 0,
//                    var reds: Int = 0,
//                    var blacks: Int = 0,
//                    var boulders: Int = 0,
//                    var sports: Int = 0,
//                    var trads: Int = 0)

data class TopoAndRoutes(
        var topo: Topo,
        var routes: List<Route>
)

data class Topo(val id: Long? = null,
                val firebaseId: DocumentReference? = null,
                var locationId: Long,
                var name: String,
                var image: String)

data class Route(val id: Long? = null,
                 val firebaseId: DocumentReference? = null,
                 var topoId: Long? = null,
                 var name: String? = null,
                 var grade: Grade? = null,
                 var type: RouteType? = null,
                 var description: String? = null,
                 var path: List<List<Pair<Float, Float>>>? = null)


data class Grade(var string: String,
                 var type: GradeType,
                 var colour: GradeColour) {
    companion object {
        fun from(vGrade: VGrade): Grade {
            return Grade(vGrade.textRepresentation, GradeType.V, vGrade.colour)
        }

        fun from(fontGrade: FontGrade): Grade {
            return Grade(fontGrade.textRepresentation, GradeType.FONT, fontGrade.colour)
        }

        fun from(sportGrade: SportGrade): Grade {
            return Grade(sportGrade.textRepresentation, GradeType.SPORT, sportGrade.colour)
        }

        fun from(tradAdjectivalGrade: TradAdjectivalGrade, tradTechnicalGrade: TradTechnicalGrade): Grade {
            return Grade(tradAdjectivalGrade.textRepresentation + " " + tradTechnicalGrade.textRepresentation,
                    GradeType.TRAD,
                    tradAdjectivalGrade.colour
            )
        }

        fun from(textRepresentation: String): Grade? {
            Timber.v("Converting $textRepresentation into a grade")
            return when {
                textRepresentation.startsWith("V") -> from(VGrade.values().first { it.textRepresentation == textRepresentation })
                textRepresentation.startsWith("f") -> from(FontGrade.values().first { it.textRepresentation == textRepresentation })
                textRepresentation.contains(" ") -> {
                    val grades = textRepresentation.split(" ")
                    val adj = TradAdjectivalGrade.values().first { it.textRepresentation == grades[0] }
                    val tech = TradTechnicalGrade.values().first { it.textRepresentation == grades[1] }
                    from(adj, tech)
                }
                else -> from(SportGrade.values().first { it.textRepresentation == textRepresentation })
            }
        }
    }
}

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
