package uk.co.oliverdelange.wcr_android_kt.service

import uk.co.oliverdelange.wcr_android_kt.model.GradeColour
import uk.co.oliverdelange.wcr_android_kt.model.RouteType
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class WorkerService @Inject constructor(val appExecutors: AppExecutors,
                                        val locationRepository: LocationRepository,
                                        val topoRepository: TopoRepository) {

    fun updateRouteInfo(sectorId: Long) {
        appExecutors.diskIO().execute {
            val sectorRoutes = topoRepository.getToposForLocation(sectorId)
            updateLocationRouteInfo(sectorRoutes, sectorId)

            val sector = locationRepository.get(sectorId)
            sector.parentId?.let { cragId ->
                val cragRoutes = topoRepository.getToposForCrag(cragId)
                updateLocationRouteInfo(cragRoutes, cragId)
            }
        }
    }

    private fun updateLocationRouteInfo(toposAndRoutes: List<TopoAndRoutes>, locationId: Long) {
        var boulders = 0
        var sports = 0
        var trads = 0
        var greens = 0
        var oranges = 0
        var reds = 0
        var blacks = 0
        for (topoAndRoute in toposAndRoutes) {
            for (route in topoAndRoute.routes) {
                when (route.type) {
                    RouteType.BOULDERING -> boulders++
                    RouteType.SPORT -> sports++
                    RouteType.TRAD -> trads++
                }
                when (route.grade?.colour) {
                    GradeColour.GREEN -> greens++
                    GradeColour.ORANGE -> oranges++
                    GradeColour.RED -> reds++
                    GradeColour.BLACK -> blacks++
                }
            }
        }
        locationRepository.updateRouteInfo(locationId, boulders, sports, trads, greens, oranges, reds, blacks)
    }
}