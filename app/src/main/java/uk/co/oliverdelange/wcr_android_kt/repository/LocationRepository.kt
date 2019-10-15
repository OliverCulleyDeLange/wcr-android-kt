package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.LocationDao
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.LocationRouteInfo
import uk.co.oliverdelange.wcr_android_kt.mapper.fromLocationDto
import uk.co.oliverdelange.wcr_android_kt.mapper.toLocationDto
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import javax.inject.Inject

class LocationRepository @Inject constructor(private val locationDao: LocationDao) {

    fun save(location: Location): Single<Long> {
        Timber.d("Saving %s location: %s", location.type, location.name)
        val locationDTO = toLocationDto(location)
        return saveToLocalDb(locationDTO)
    }

    private fun saveToLocalDb(location: uk.co.oliverdelange.wcr_android_kt.db.dto.local.Location)
            : Single<Long> {
        return Single.fromCallable {
            val id = locationDao.insert(location)
            Timber.d("Saved location $id to local db: %s", location)
            id
        }
    }

    fun load(selectedLocationId: Long): LiveData<Location> {
        Timber.d("Loading location from id: %s", selectedLocationId)
        val liveLocationDTO = locationDao.load(selectedLocationId)
        return Transformations.map(liveLocationDTO) {
            fromLocationDto(it)
        }
    }

    fun get(locationId: Long): Location? {
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

    fun loadSectorsFor(cragId: Long): LiveData<List<Location>> {
        Timber.d("Loading sectors for cragId: %s", cragId)
        val liveLocationDTOs = locationDao.loadWithParentId(cragId)
        return Transformations.map(liveLocationDTOs) { locations ->
            locations.map { location ->
                fromLocationDto(location)
            }
        }
    }

    fun loadRouteInfoFor(locationId: Long): LiveData<LocationRouteInfo> {
        Timber.d("Loading route info for location: %s", locationId)
        return locationDao.getRouteInfo(locationId)
    }

    fun search(query: String): LiveData<List<Location>> {
        Timber.d("Searching locations: %s", query)
        val liveLocationDTOs = locationDao.searchOnName("%$query%")
        return Transformations.map(liveLocationDTOs) { locations ->
            locations.map { location ->
                fromLocationDto(location)
            }
        }
    }
}