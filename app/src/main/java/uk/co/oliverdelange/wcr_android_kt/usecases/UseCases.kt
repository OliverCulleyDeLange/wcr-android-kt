package uk.co.oliverdelange.wcr_android_kt.usecases

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.reactivex.Single
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.service.UploadLocationsWorker
import javax.inject.Inject

class SubmitLocationUseCase @Inject constructor(private val locationRepository: LocationRepository) {
    fun submitLocation(location: Location): Single<String> {
        val save = locationRepository.save(location)

        //TODO Possible race condition - if task starts before saved to local db, nothing will be uploaded.
        // Need to wait save to local db, then enqueu worker
        WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<UploadLocationsWorker>()
                .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build())
        return save
    }
}