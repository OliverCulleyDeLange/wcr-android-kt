package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.reactivex.Completable
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.RouteDao
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Route
import uk.co.oliverdelange.wcr_android_kt.mapper.fromRouteDto
import uk.co.oliverdelange.wcr_android_kt.mapper.toRouteDto
import javax.inject.Inject

class RouteRepository @Inject constructor(private val routeDao: RouteDao) {

    fun get(routeId: Long): Route {
        Timber.d("Getting Route from id: %s", routeId)
        return routeDao.get(routeId)
    }

    fun save(route: uk.co.oliverdelange.wcr_android_kt.model.Route): Completable {
        Timber.d("Saving %s route: %s", route.type, route.name)
        val routeDTO = toRouteDto(route)
        return saveToLocalDb(routeDTO)
    }

    private fun saveToLocalDb(routeDTO: Route): Completable {
        return Completable.fromAction {
            routeDao.insert(routeDTO)
            Timber.d("Saved route to local db: %s", routeDTO.name)
        }
    }

    fun searchOnName(query: String?): LiveData<List<uk.co.oliverdelange.wcr_android_kt.model.Route>> {
        Timber.d("Searching routes: %s", query)
        val searchOnName = routeDao.searchOnName("%$query%")
        return Transformations.map(searchOnName) { routes ->
            routes.map { route ->
                fromRouteDto(route)
            }
        }
    }
}