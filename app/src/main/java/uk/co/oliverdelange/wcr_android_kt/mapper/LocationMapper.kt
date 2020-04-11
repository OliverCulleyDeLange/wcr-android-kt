package uk.co.oliverdelange.wcr_android_kt.mapper

import com.google.android.gms.maps.model.LatLng
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.util.randomAlphaNumeric
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.LocationEntity as LocationDTO

fun toLocationDto(location: Location): LocationDTO {
    return LocationDTO(location.id ?: "${location.name}_${randomAlphaNumeric(8)}",
            location.parentLocationId,

            location.name,
            location.latlng.latitude,
            location.latlng.longitude,
            location.type.toString(),
            uploaderId = location.uploaderId
    )
}

fun fromLocationDto(location: LocationDTO): Location {
    return Location(location.id,
            location.parentLocationId,

            location.name,
            LatLng(
                    location.lat,
                    location.lng),
            LocationType.valueOf(location.type),
            location.uploaderId
    )
}