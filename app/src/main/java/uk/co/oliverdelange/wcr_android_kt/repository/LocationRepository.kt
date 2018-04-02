package uk.co.oliverdelange.wcr_android_kt.repository

import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class LocationRepository @Inject constructor(val locationDao: LocationDao, val appExecutors: AppExecutors) {

    fun save(location: Location) {
        appExecutors.diskIO().execute {
            Timber.d("Saving location: %s", location)
            locationDao.save(location)
        }
    }

}