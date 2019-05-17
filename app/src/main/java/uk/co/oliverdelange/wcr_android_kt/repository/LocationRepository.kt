package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.mapper.fromLocationDto
import uk.co.oliverdelange.wcr_android_kt.mapper.toLocationDto
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class LocationRepository @Inject constructor(val locationDao: LocationDao,
                                             val appExecutors: AppExecutors,
                                             val firebaseFirestore: FirebaseFirestore) {

    fun save(location: Location): Single<String> {
        Timber.d("Saving %s: %s", location.type, location.name)
        val locationDTO = toLocationDto(location)
        return saveToLocalDb(locationDTO)
    }

    private fun saveToLocalDb(location: uk.co.oliverdelange.wcr_android_kt.db.Location): Single<String> {
        return Single.fromCallable {
            locationDao.insert(location)
            Timber.d("Saved location to local db")
            location.id
        }
    }

    fun load(selectedLocationId: String): LiveData<Location> {
        Timber.d("Loading location from id: %s", selectedLocationId)
        val liveLocationDTO = locationDao.load(selectedLocationId)
        return Transformations.map(liveLocationDTO) {
            fromLocationDto(it)
        }
    }

    fun get(locationId: String): Location? {
        Timber.d("Getting Location from id: %s", locationId)
        val location = locationDao.get(locationId)
        return location?.let { fromLocationDto(it) }
    }

    fun loadCrags(): LiveData<List<Location>> {
        Timber.d("Loading crags")
        val liveLocationDTOs = locationDao.loadByType(LocationType.CRAG.toString())
        return Transformations.map(liveLocationDTOs) { locations ->
            locations.map { location ->
                fromLocationDto(location)
            }
        }
    }

    fun loadSectorsFor(cragId: String): LiveData<List<Location>> {
        Timber.d("Loading sectors for cragId: %s", cragId)
        val liveLocationDTOs = locationDao.loadWithParentId(cragId)
        return Transformations.map(liveLocationDTOs) { locations ->
            locations.map { location ->
                fromLocationDto(location)
            }
        }
    }

    fun updateLocationRouteInfo(toposAndRoutes: List<TopoAndRoutes>, locationId: String) {
        Timber.d("Updating route info for location with id %s", locationId)
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
        Timber.d("Search locations: %s", query)
        val liveLocationDTOs = locationDao.searchOnName("%$query%")
        return Transformations.map(liveLocationDTOs) { locations ->
            locations.map { location ->
                fromLocationDto(location)
            }
        }
    }
}