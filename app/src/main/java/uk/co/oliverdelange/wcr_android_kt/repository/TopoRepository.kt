package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.RouteDao
import uk.co.oliverdelange.wcr_android_kt.db.TopoDao
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.RouteType
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class TopoRepository @Inject constructor(val topoDao: TopoDao, val routeDao: RouteDao, val appExecutors: AppExecutors) {

    fun save(topo: Topo, routes: Collection<Route>): MutableLiveData<Pair<Long, Array<Long>>> {
        val result = MutableLiveData<Pair<Long, Array<Long>>>()
        appExecutors.diskIO().execute {
            Timber.d("Saving topo: %s", topo)
            val topoId = topoDao.save(topo)
            val routeIds = emptyArray<Long>()
            for (route in routes) {
                route.topoId = topoId
                route.type = RouteType.BOULDERING //TODO Only tmp
                Timber.d("Saving route: %s", route)
                routeIds.plus(routeDao.save(route))
            }
            // Saved topo and all routes so notify observer
            appExecutors.mainThread().execute({ result.value = Pair(topoId, routeIds) })
        }
        return result
    }

    fun getToposForLocation(locationId: Long): LiveData<List<TopoAndRoutes>> {
        return topoDao.loadTopoAndRoutes(locationId)
    }
}