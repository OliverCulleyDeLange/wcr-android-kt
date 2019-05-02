package uk.co.oliverdelange.wcr_android_kt.mapper

import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.db.Route as RouteDTO

fun toRouteDto(route: Route): RouteDTO {
    return RouteDTO(
            route.name ?: "",
            route.topoId ?: "",
            route.name ?: "",
            route.grade?.string ?: "",
            route.type?.name ?: "",
            route.description ?: "",
            coordsSetToString(route.path) ?: ""
    )
}

fun fromRouteDto(route: RouteDTO): Route {
    val grade = Grade.from(route.grade)
    val routeType = RouteType.valueOf(route.type)
    return Route(
            route.id,
            route.topoId,
            route.name,
            grade,
            routeType,
            route.description,
            stringToCoordsSet(route.path)
    )
}