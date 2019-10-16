package uk.co.oliverdelange.wcr_android_kt.db

import androidx.room.TypeConverter
import uk.co.oliverdelange.wcr_android_kt.model.*

class WcrTypeConverters {

    @TypeConverter
    fun strListToString(strList: List<String>?): String? {
        if (strList?.any { it.contains(",") } == true) throw RuntimeException("String list can't contain items containing a comma")
        return strList?.joinToString(",")
    }

    @TypeConverter
    fun stringToStrList(string: String?): List<String>? {
        return string?.let { string.split(",") }
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

    @TypeConverter
    fun stringToSyncType(data: String?): SyncType? {
        return if (enumContains<GradeColour>(data)) SyncType.valueOf(data!!) else null
    }

    inline fun <reified T : Enum<T>> enumContains(name: String?): Boolean {
        return enumValues<T>().any { it.name == name }
    }
}
