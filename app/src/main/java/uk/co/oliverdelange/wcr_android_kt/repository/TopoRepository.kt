package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.WorkerThread
import org.json.JSONArray
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.db.RouteDao
import uk.co.oliverdelange.wcr_android_kt.db.TopoDao
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class TopoRepository @Inject constructor(val topoDao: TopoDao,
                                         val routeDao: RouteDao,
                                         val locationDao: LocationDao,
                                         val appExecutors: AppExecutors) {

    fun save(topo: Topo, routes: Collection<Route>): MutableLiveData<Pair<Long, List<Long>>> {
        val result = MutableLiveData<Pair<Long, List<Long>>>()
        appExecutors.diskIO().execute {
            Timber.d("Saving topo: %s", topo)
            val topoId = topoDao.insert(topo)
            val routeIds = mutableListOf<Long>()
            for (route in routes) {
                route.topoId = topoId
                Timber.d("Saving route: %s", route)
                val routeId = routeDao.insert(route)
                routeIds.add(routeId)
                route.id = routeId
            }
            // Saved topo and all routes so notify observer
            appExecutors.mainThread().execute({
                result.value = Pair(topoId, routeIds)
                //TODO Create network bound resource to tidy up this logic.
                val parseRoutes = routes.map {
                    ParseRoute(it.id,
                            it.name,
                            it.grade?.string,
                            it.type?.name,
                            it.description,
                            JSONArray(it.path))
                }
                val parseTopo = ParseTopo(topoId, topo.name, topo.image, parseRoutes)
                parseTopo.saveInBackground {
                    if (it != null) {
                        Timber.e(it, "Failed to save ParseLocation")
                    } else {
                        Timber.d("ParseLocation saved: ${parseTopo.id}:${parseTopo.name} with routes: ${parseRoutes.map { "${it.id}:${it.name}, " }}")
                    }
                }
            })
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
        val sectorsForCrag = locationDao.getWithParentId(cragId)
        for (sector in sectorsForCrag) {
            sector.id?.let { sectorId -> toposAndRoutes.addAll(topoDao.getTopoAndRoutes(sectorId)) }
        }
        return toposAndRoutes
    }

    fun search(query: String): LiveData<List<Topo>> {
        return topoDao.searchOnName("%$query%")
    }
}