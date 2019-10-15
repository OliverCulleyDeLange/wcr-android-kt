package uk.co.oliverdelange.wcr_android_kt.usecases

import io.reactivex.Single
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.service.uploadSync
import javax.inject.Inject

class SubmitLocationUseCase @Inject constructor(
        private val locationRepository: LocationRepository
) {
    fun submitLocation(location: Location): Single<Long> {
        val save = locationRepository.save(location)
        uploadSync()
        return save
    }
}