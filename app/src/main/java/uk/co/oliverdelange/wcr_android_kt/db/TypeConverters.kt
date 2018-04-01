package uk.co.oliverdelange.wcr_android_kt.db

import android.arch.persistence.room.TypeConverter
import uk.co.oliverdelange.wcr_android_kt.model.LocationType

class WcrTypeConverters {

    @TypeConverter
    fun stringToLocationType(data: String): LocationType {
        return LocationType.valueOf(data)
    }

    @TypeConverter
    fun locationTypeToString(locationType: LocationType): String {
        return locationType.name
    }
}
