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
    val grade = when {
        route.grade.startsWith("V") -> Grade.from(VGrade.valueOf(route.grade))
        route.grade.startsWith("f") -> Grade.from(FontGrade.valueOf(route.grade))
        route.grade.contains(" ") -> {
            val grades = route.grade.split(" ")
            val adj = TradAdjectivalGrade.valueOf(grades[0])
            val tech = TradTechnicalGrade.valueOf(grades[1])
            Grade.from(adj, tech)
        }
        else -> Grade.from(SportGrade.valueOf(route.grade))
    }
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