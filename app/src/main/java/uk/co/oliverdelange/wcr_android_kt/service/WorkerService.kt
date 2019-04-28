package uk.co.oliverdelange.wcr_android_kt.service

import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class WorkerService @Inject constructor(val appExecutors: AppExecutors,
                                        val locationRepository: LocationRepository,
                                        val topoRepository: TopoRepository) {

    fun updateRouteInfo(sectorId: Long) {
        Timber.d("Updating route info for sector with id: %s", sectorId)
        appExecutors.diskIO().execute {
            val sectorRoutes = topoRepository.getToposForLocation(sectorId)
            locationRepository.updateLocationRouteInfo(sectorRoutes, sectorId)

            val sector = locationRepository.get(sectorId)
            sector?.parentId?.let { cragId ->
                val cragRoutes = topoRepository.getToposForCrag(cragId)
                locationRepository.updateLocationRouteInfo(cragRoutes, cragId)
            }
        }
    }
}