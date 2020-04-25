package uk.co.oliverdelange.wcr_android_kt.usecases

import io.reactivex.Single
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.sync.uploadSync
import javax.inject.Inject
//TODO Test me

class SubmitLocationUseCase @Inject constructor(
        private val locationRepository: LocationRepository
) {
    fun submitLocation(location: Location): Single<String> {
        val save = locationRepository.save(location)
        uploadSync()
        return save
    }
}