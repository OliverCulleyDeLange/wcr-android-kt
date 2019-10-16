package uk.co.oliverdelange.wcr_android_kt.mapper

import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.util.randomAlphaNumeric
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Route as RouteDTO

fun toRouteDto(route: Route): RouteDTO {
    return RouteDTO(route.id ?: "${route.name}_${randomAlphaNumeric(8)}",
            route.topoId ?: "UNKNOWN (Bug)",

            route.name ?: "",
            route.grade?.string ?: "",
            route.grade?.colour?.name ?: "",
            route.type?.name ?: "",
            route.description ?: "",
            coordsSetToString(route.path) ?: ""
    )
}

fun fromRouteDto(route: RouteDTO): Route {
    val grade = Grade.from(route.grade)
    val routeType = RouteType.valueOf(route.type)
    return Route(route.id,
            route.topoId,

            route.name,
            grade,
            routeType,
            route.description,
            stringToCoordsSet(route.path)
    )
}