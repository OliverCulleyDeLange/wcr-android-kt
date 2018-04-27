package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.WorkerThread
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.RouteDao
import uk.co.oliverdelange.wcr_android_kt.db.TopoDao
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class TopoRepository @Inject constructor(val topoDao: TopoDao,
                                         val routeDao: RouteDao,
                                         val locationRepository: LocationRepository,
                                         val appExecutors: AppExecutors) {

    fun save(topo: Topo, routes: Collection<Route>): MutableLiveData<Pair<Long, Array<Long>>> {
        val result = MutableLiveData<Pair<Long, Array<Long>>>()
        appExecutors.diskIO().execute {
            Timber.d("Saving topo: %s", topo)
            val topoId = topoDao.save(topo)
            val routeIds = emptyArray<Long>()
            for (route in routes) {
                route.topoId = topoId
                Timber.d("Saving route: %s", route)
                routeIds.plus(routeDao.save(route))
            }
            // Saved topo and all routes so notify observer
            appExecutors.mainThread().execute({ result.value = Pair(topoId, routeIds) })
        }
        return result
    }

    fun loadToposForLocation(locationId: Long): LiveData<List<TopoAndRoutes>> {
        return topoDao.loadTopoAndRoutes(locationId)
    }

    @WorkerThread
    fun getToposForLocation(locationId: Long): List<TopoAndRoutes> {
        return topoDao.getTopoAndRoutes(locationId)
    }

    @WorkerThread
    fun getToposForCrag(cragId: Long): List<TopoAndRoutes> {
        val toposAndRoutes = ArrayList<TopoAndRoutes>()
        val sectorsForCrag = locationRepository.getSectorsFor(cragId)
        for (sector in sectorsForCrag) {
            sector.id?.let { toposAndRoutes.addAll(topoDao.getTopoAndRoutes(it)) }
        }
        return toposAndRoutes
    }
}