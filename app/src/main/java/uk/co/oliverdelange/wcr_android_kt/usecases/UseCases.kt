package uk.co.oliverdelange.wcr_android_kt.usecases

import io.reactivex.Single
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import javax.inject.Inject

class SubmitLocationUseCase @Inject constructor(private val locationRepository: LocationRepository) {
    fun submitLocation(location: Location): Single<String> {
        return locationRepository.save(location)
    }
}