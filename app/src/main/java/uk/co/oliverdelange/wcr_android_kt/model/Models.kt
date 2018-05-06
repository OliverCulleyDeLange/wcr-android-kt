package uk.co.oliverdelange.wcr_android_kt.model

import android.arch.persistence.room.*
import android.support.annotation.DrawableRes
import com.google.android.gms.maps.model.LatLng
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.map.Icon

@Entity
data class Location(@PrimaryKey(autoGenerate = true) val id: Long? = null,
                    val parentId: Long? = null,
                    var name: String,
                    var lat: Double,
                    var lng: Double,
                    val type: LocationType) {
    var greens: Int = 0
    var oranges: Int = 0
    var reds: Int = 0
    var blacks: Int = 0
    var boulders: Int = 0
    var sports: Int = 0
    var trads: Int = 0

    @Ignore
    val latlng: LatLng = LatLng(lat, lng)
}

class TopoAndRoutes {
    @Embedded
    lateinit var topo: Topo
    @Relation(parentColumn = "id", entityColumn = "topoId")
    lateinit var routes: List<Route>
}

@Entity
data class Topo(@PrimaryKey(autoGenerate = true) var id: Long? = null,
                var locationId: Long,
                var name: String,
                var image: String)

@Entity(foreignKeys = [(
        ForeignKey(entity = Topo::class, parentColumns = arrayOf("id"), childColumns = arrayOf("topoId"))
        )])
data class Route(@PrimaryKey var id: Long? = null,
                 var topoId: Long? = null,
                 var name: String? = null,
                 @Embedded(prefix = "grade_") var grade: Grade? = null,
                 var type: RouteType? = null,
                 var description: String? = null)

@Entity
data class Grade(@PrimaryKey var string: String,
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

