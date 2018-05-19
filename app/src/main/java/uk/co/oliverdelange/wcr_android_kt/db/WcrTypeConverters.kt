package uk.co.oliverdelange.wcr_android_kt.db

import android.arch.persistence.room.TypeConverter
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.GradeType
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.model.RouteType

class WcrTypeConverters {

    @TypeConverter
    fun coordsSetToString(coords: Set<Pair<Float, Float>>?): String? {
        return coords?.let {
            it.joinToString(",", transform = { pair -> "${pair.first}:${pair.second}" })
        }
    }

    @TypeConverter
    fun stringToCoordsSet(coords: String?): Set<Pair<Float, Float>>? {
        return coords?.let {
            try {

                val stringPairs = coords.split(",")
                val pairs = stringPairs.map {
                    val parts = it.split(":").map { it.toFloat() }
                    Pair(parts[0], parts[1])
                }
                pairs.toSet()
            } catch (e: Exception) {
                Timber.e(e, "Error whilst converting topo route path string into Set<Pair<Int, Int>>")
                emptySet()
            }
        }
    }

    @TypeConverter
    fun enumTypeToString(enum: Enum<*>?): String {
        return enum?.name ?: ""
    }

    @TypeConverter
    fun stringToLocationType(data: String): LocationType? {
        return if (enumContains<LocationType>(data)) LocationType.valueOf(data) else null
    }

    @TypeConverter
    fun stringToRouteType(data: String): RouteType? {
        return if (enumContains<RouteType>(data)) RouteType.valueOf(data) else null
    }

    @TypeConverter
    fun stringToGradeType(data: String): GradeType? {
        return if (enumContains<GradeType>(data)) GradeType.valueOf(data) else null
    }

    @TypeConverter
    fun stringToGradeColour(data: String): GradeColour? {
        return if (enumContains<GradeColour>(data)) GradeColour.valueOf(data) else null
    }

    inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
        return enumValues<T>().any { it.name == name }
    }
}
