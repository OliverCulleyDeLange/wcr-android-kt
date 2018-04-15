package uk.co.oliverdelange.wcr_android_kt.db

import android.arch.persistence.room.TypeConverter
import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.GradeType
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.model.RouteType

class WcrTypeConverters {

    @TypeConverter
    fun enumTypeToString(enum: Enum<*>): String {
        return enum.name
    }

    @TypeConverter
    fun stringToLocationType(data: String): LocationType {
        return LocationType.valueOf(data)
    }

    @TypeConverter
    fun stringToRouteType(data: String): RouteType {
        return RouteType.valueOf(data)
    }

    @TypeConverter
    fun stringToGradeType(data: String): GradeType {
        return GradeType.valueOf(data)
    }

    @TypeConverter
    fun stringToGradeColour(data: String): GradeColour {
        return GradeColour.valueOf(data)
    }
}
