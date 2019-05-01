package uk.co.oliverdelange.wcr_android_kt.mapper

import com.google.android.gms.maps.model.LatLng
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.db.Location as LocationDTO

fun toLocationDto(location: Location): LocationDTO {
    return LocationDTO(location.name,
            location.parentLocation,
            location.name,
            location.latlng.latitude,
            location.latlng.longitude,
            location.type.toString(),
            location.greens,
            location.oranges,
            location.reds,
            location.blacks,
            location.boulders,
            location.sports,
            location.trads
    )
}

fun fromLocationDto(location: LocationDTO): Location {
    return Location(
            location.id,
            location.parentLocation,
            location.name,
            LatLng(
                    location.lat,
                    location.lng),
            LocationType.valueOf(location.type),
            location.greens,
            location.oranges,
            location.reds,
            location.blacks,
            location.boulders,
            location.sports,
            location.trads
    )
}