package uk.co.oliverdelange.wcr_android_kt.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
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

@Entity
data class Topo(@PrimaryKey val id: Int,
                val entrantId: Int,
                val image: String,
                val routes: List<Route>)

@Entity
data class Route(@PrimaryKey val id: Int,
                 val name: String,
                 val grade: Grade,
                 val type: RouteType,
                 val description: String)

@Entity
data class Grade(@PrimaryKey val string: String,
                 val type: GradeType,
                 val colour: GradeColour)

enum class LocationType(val icon: Icon) {
    CRAG(Icon.CRAG), SECTOR(Icon.SECTOR)
}

enum class RouteType {
    TRAD, SPORT, BOULDERING
}

enum class GradeType {
    V, FONT, SPORT, TRAD
}

enum class GradeColour {
    GREEN, ORANGE, RED, BLACK
}

