package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class LocationRepository @Inject constructor(val locationDao: LocationDao,
                                             val appExecutors: AppExecutors) {

    fun save(location: Location): LiveData<Long> {
        val result = MutableLiveData<Long>()
        appExecutors.diskIO().execute {
            Timber.d("Saving location: %s", location)
            val locationId = locationDao.save(location)
            appExecutors.mainThread().execute({ result.value = locationId })
        }
        return result
    }

    fun load(selectedLocationId: Long): LiveData<Location> {
        return locationDao.load(selectedLocationId)
    }

    fun get(selectedLocationId: Long): Location {
        return locationDao.get(selectedLocationId)
    }

    fun loadCrags(): LiveData<List<Location>> {
        return locationDao.load(LocationType.CRAG)
    }

    fun loadSectorsFor(cragId: Long): LiveData<List<Location>> {
        return locationDao.loadWithParentId(cragId)
    }

    fun getSectorsFor(cragId: Long): List<Location> {
        return locationDao.getWithParentId(cragId)
    }

    fun updateRouteInfo(locationId: Long, boulders: Int, sports: Int, trads: Int, greens: Int, oranges: Int, reds: Int, blacks: Int) {
        locationDao.updateRouteInfo(locationId, boulders, sports, trads, greens, oranges, reds, blacks)
    }
}