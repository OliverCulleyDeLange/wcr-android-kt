package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class LocationRepository @Inject constructor(val locationDao: LocationDao,
                                             val appExecutors: AppExecutors) {

    fun save(location: Location): LiveData<Long> {
        val result = MutableLiveData<Long>()
        appExecutors.diskIO().execute {
            val locationId = locationDao.insert(location)
            appExecutors.mainThread().execute({
                Timber.d("Saved location to room: $locationId")
                result.value = locationId
                //TODO Create network bound resource to tidy up this logic.
                val parseLocation = ParseLocation(locationId,
                        location.parentId,
                        location.name,
                        location.lat,
                        location.lng,
                        location.type.name)
                parseLocation.saveInBackground {
                    if (it != null) {
                        Timber.e(it, "Failed to save ParseLocation")
                    } else {
                        Timber.d("ParseLocation saved: ${parseLocation.id}")
                    }
                }
            })
        }
        return result
    }

    fun load(selectedLocationId: Long): LiveData<Location> {
        return locationDao.load(selectedLocationId)
    }

    fun get(locationId: Long): Location? {
        return locationDao.get(locationId)
    }

    fun loadCrags(): LiveData<List<Location>> {
        return locationDao.load(LocationType.CRAG)
    }

    fun loadSectorsFor(cragId: Long): LiveData<List<Location>> {
        return locationDao.loadWithParentId(cragId)
    }

    fun updateLocationRouteInfo(toposAndRoutes: List<TopoAndRoutes>, locationId: Long) {
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
        locationDao.updateRouteInfo(locationId, boulders, sports, trads, greens, oranges, reds, blacks)
    }

    fun search(query: String): LiveData<List<Location>> {
        return locationDao.searchOnName("%$query%")
    }
}