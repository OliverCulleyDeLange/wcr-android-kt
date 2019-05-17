package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.reactivex.Completable
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.Route
import uk.co.oliverdelange.wcr_android_kt.db.RouteDao
import uk.co.oliverdelange.wcr_android_kt.mapper.fromRouteDto
import uk.co.oliverdelange.wcr_android_kt.mapper.toRouteDto
import javax.inject.Inject

class RouteRepository @Inject constructor(private val routeDao: RouteDao) {

    fun get(routeId: String): Route {
        return routeDao.get(routeId)
    }

    fun save(route: uk.co.oliverdelange.wcr_android_kt.model.Route): Completable {
        val routeDTO = toRouteDto(route)
        return saveToLocalDb(routeDTO)
    }

    private fun saveToLocalDb(routeDTO: Route): Completable {
        return Completable.fromAction {
            Timber.d("Saving route to local db: %s", routeDTO.name)
            routeDao.insert(routeDTO)
        }
    }

    fun searchOnName(query: String?): LiveData<List<uk.co.oliverdelange.wcr_android_kt.model.Route>> {
        val searchOnName = routeDao.searchOnName("%$query%")
        return Transformations.map(searchOnName) { routes ->
            routes.map { route ->
                fromRouteDto(route)
            }
        }
    }
}