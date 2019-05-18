package uk.co.oliverdelange.wcr_android_kt.usecases

import androidx.annotation.WorkerThread
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.service.uploadSync
import javax.inject.Inject

class SubmitLocationUseCase @Inject constructor(
        private val locationRepository: LocationRepository
) {
    fun submitLocation(location: Location): Single<String> {
        val save = locationRepository.save(location)
        uploadSync()
        return save
    }
}

class UpdateRouteInfoUseCase @Inject constructor(
        private val locationRepository: LocationRepository,
        private val topoRepository: TopoRepository
) {
    @WorkerThread
    fun updateRouteInfo(sectorId: String) {
        Timber.d("Updating route info for sector with id: %s", sectorId)
        val sectorRoutes = topoRepository.getToposForLocation(sectorId)
        locationRepository.updateLocationRouteInfo(sectorRoutes, sectorId)

        val sector = locationRepository.get(sectorId)
        sector?.parentLocation?.let { cragId ->
            val cragRoutes = topoRepository.getToposForCrag(cragId)
            locationRepository.updateLocationRouteInfo(cragRoutes, cragId)
        }
    }
}