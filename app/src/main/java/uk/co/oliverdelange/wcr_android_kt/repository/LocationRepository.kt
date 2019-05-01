package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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

    fun save(location: Location): LiveData<String> {
        val locationDTO = toLocationDto(location)
        Timber.d("Saving %s: %s", locationDTO.type, locationDTO.name)
        val result = MutableLiveData<String>()
        appExecutors.networkIO().execute {
            firebaseFirestore.collection("locations")
//                    .document(location.name)
                    .add(locationDTO)
                    .addOnSuccessListener {
                        Timber.d("Location saved to firestore: $it")
                        appExecutors.diskIO().execute {
                            Timber.d("Saving location to local db")
                            val locationRowId = locationDao.insert(locationDTO)
                            Timber.d("Saved location - its row-id id $locationRowId")
                            appExecutors.mainThread().execute { result.value = locationDTO.id }
                        }
                    }
                    .addOnFailureListener {
                        if (it is FirebaseFirestoreException && it.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Timber.d("User tried to submit when not logged in")
                            // TODO show message
                        } else {
                            Timber.e(it, "failed to add location to firestore")
                        }
                    }
        }
        return result
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