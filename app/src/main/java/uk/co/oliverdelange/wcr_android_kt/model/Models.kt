package uk.co.oliverdelange.wcr_android_kt.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(primaryKeys = ["name", "lat", "lng"])
data class Location(val name: String,
                    val lat: Double,
                    val lng: Double,
                    val type: LocationType,
                    var greens: Int = 0,
                    var oranges: Int = 0,
                    var reds: Int = 0,
                    var blacks: Int = 0,
                    var boulders: Int = 0,
                    var sports: Int = 0,
                    var trads: Int = 0) {
    fun greens(i: Int): Location = apply { greens = i }
    fun oranges(i: Int): Location = apply { oranges = i }
    fun reds(i: Int): Location = apply { reds = i }
    fun blacks(i: Int): Location = apply { blacks = i }
    fun boulders(i: Int): Location = apply { boulders = i }
    fun sports(i: Int): Location = apply { sports = i }
    fun trads(i: Int): Location = apply { trads = i }
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

enum class LocationType {
    CRAG, SECTOR
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

