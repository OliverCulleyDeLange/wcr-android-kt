package uk.co.oliverdelange.wcr_android_kt.mapper

import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.db.Topo as TopoDTO
import uk.co.oliverdelange.wcr_android_kt.db.TopoAndRoutes as TopoAndRoutesDto

fun toTopoDto(topo: Topo): TopoDTO {
    return TopoDTO(
            topo.name,
            topo.locationId,
            topo.name,
            topo.image
    )
}

fun fromTopoDto(topo: TopoDTO): Topo {
    return Topo(
            topo.id,
            topo.locationId,
            topo.name,
            topo.image
    )
}


fun fromTopoAndRouteDto(topoAndRouteDtos: List<TopoAndRoutesDto>): List<TopoAndRoutes> {
    return topoAndRouteDtos.map { topoAndRouteDto ->
        TopoAndRoutes(
                fromTopoDto(topoAndRouteDto.topo),
                topoAndRouteDto.routes.map { route ->
                    fromRouteDto(route)
                }
        )
    }
}