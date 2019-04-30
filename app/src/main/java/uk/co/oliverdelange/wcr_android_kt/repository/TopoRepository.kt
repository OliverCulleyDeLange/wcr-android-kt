package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.db.RouteDao
import uk.co.oliverdelange.wcr_android_kt.db.TopoDao
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class TopoRepository @Inject constructor(val topoDao: TopoDao,
                                         val routeDao: RouteDao,
                                         val locationDao: LocationDao,
                                         val firebaseFirestore: FirebaseFirestore,
                                         val appExecutors: AppExecutors) {

    fun save(topo: Topo, routes: Collection<Route>): MutableLiveData<Pair<Long, List<Long>>> {
        Timber.d("Saving topo to firestore")
        val result = MutableLiveData<Pair<Long, List<Long>>>()

        firebaseFirestore
                .collection("locations")
                .document(topo.locationId)
                .collection("topos")
                .document(topo.name)
                .set(topo)
                .addOnSuccessListener {
                    Timber.d("Topo saved to firestore: ${topo.name}")
                    appExecutors.diskIO().execute {
                        Timber.d("Saving topo to local db: %s", topo)
                        val topoId = topoDao.insert(topo)
                        val routeIds = mutableListOf<Long>()
                        for (route in routes) {
                            route.topoId = topoId
                            Timber.d("Saving route to db: %s", route)
                            routeIds.add(routeDao.insert(route))
                        }
                        // Saved topo and all routes so notify observer
                        appExecutors.mainThread().execute { result.value = Pair(topoId, routeIds) }
                    }
                }
                .addOnFailureListener {
                    // TODO DRY
                    if (it is FirebaseFirestoreException && it.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Timber.d("User tried to submit when not logged in")
                        // TODO show message
                    } else {
                        Timber.e(it, "failed to add location to firestore")
                    }
                }

        return result
    }

    fun loadToposForLocation(locationId: String): LiveData<List<TopoAndRoutes>> {
        Timber.d("Loading topos for location with id: %s", locationId)
        return topoDao.loadTopoAndRoutes(locationId)
    }

    @WorkerThread
    fun getToposForLocation(locationId: String): List<TopoAndRoutes> {
        Timber.d("Getting topos for location with id: %s", locationId)
        return topoDao.getTopoAndRoutes(locationId)
    }

    @WorkerThread
    fun getToposForCrag(cragId: String): List<TopoAndRoutes> {
        Timber.d("Getting topos for crag with id: %s", cragId)
        val toposAndRoutes = ArrayList<TopoAndRoutes>()
        val sectorsForCrag = locationDao.getWithParentId(cragId)
        for (sector in sectorsForCrag) {
            sector.name?.let { sectorName -> toposAndRoutes.addAll(topoDao.getTopoAndRoutes(sectorName)) }
        }
        return toposAndRoutes
    }

    fun search(query: String): LiveData<List<Topo>> {
        Timber.d("Search topos: %s", query)
        return topoDao.searchOnName("%$query%")
    }
}