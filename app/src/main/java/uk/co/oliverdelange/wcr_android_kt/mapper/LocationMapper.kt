package uk.co.oliverdelange.wcr_android_kt.mapper

import com.google.android.gms.maps.model.LatLng
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Location as LocationDTO

fun toLocationDto(location: Location): LocationDTO {
    return LocationDTO(location.id ?: 0,
            location.firebaseId,
            location.parentLocationId,
            location.parentLocationFirebaseId,
            location.name,
            location.latlng.latitude,
            location.latlng.longitude,
            location.type.toString(),
            uploaderId = location.uploaderId
    )
}

fun fromLocationDto(location: LocationDTO): Location {
    return Location(
            location.id,
            location.firebaseId,
            location.parentLocationId,
            location.parentLocationFirebaseId,
            location.name,
            LatLng(
                    location.lat,
                    location.lng),
            LocationType.valueOf(location.type),
            location.uploaderId
    )
}