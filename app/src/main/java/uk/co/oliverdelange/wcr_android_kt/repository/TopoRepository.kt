package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.db.TopoDao
import uk.co.oliverdelange.wcr_android_kt.mapper.fromTopoAndRouteDto
import uk.co.oliverdelange.wcr_android_kt.mapper.fromTopoDto
import uk.co.oliverdelange.wcr_android_kt.mapper.toTopoDto
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.service.SyncToposWorker
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class TopoRepository @Inject constructor(val topoDao: TopoDao,
                                         val locationDao: LocationDao,
                                         val firebaseFirestore: FirebaseFirestore,
                                         val appExecutors: AppExecutors) {

    fun save(topo: Topo): MutableLiveData<String> {
        val topoDTO = toTopoDto(topo)
        WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<SyncToposWorker>()
                .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build())
        return saveToLocalDb(topoDTO)
    }

    private fun saveToLocalDb(topoDTO: uk.co.oliverdelange.wcr_android_kt.db.Topo): MutableLiveData<String> {
        val result = MutableLiveData<String>()
        appExecutors.diskIO().execute {
            Timber.d("Saving topo to local db: %s", topoDTO)
            topoDao.insert(topoDTO)
            appExecutors.mainThread().execute { result.value = topoDTO.id }
        }
        return result
    }

    private fun saveToRemoteDb(topoDTO: uk.co.oliverdelange.wcr_android_kt.db.Topo) {
        Timber.d("Saving topo to firestore: ${topoDTO.name}")
        appExecutors.networkIO().execute {
            val topoFirestoreDocument = firebaseFirestore
                    .collection("locations")
                    .document(topoDTO.locationId)
                    .collection("topos")
                    .document(topoDTO.id)

            topoFirestoreDocument.set(topoDTO)
                    .addOnSuccessListener {
                        Timber.d("Topo saved to firestore: ${topoDTO.name}")
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
        }
    }

    fun loadToposForLocation(locationId: String): LiveData<List<TopoAndRoutes>> {
        Timber.d("Loading topos for location with id: %s", locationId)
        val liveTopoAndRoutesDto = topoDao.loadTopoAndRoutes(locationId)
        return Transformations.map(liveTopoAndRoutesDto) { fromTopoAndRouteDto(it) }
    }

    @WorkerThread
    fun getToposForLocation(locationId: String): List<TopoAndRoutes> {
        Timber.d("Getting topos for location with id: %s", locationId)
        val topoAndRoutes = topoDao.getTopoAndRoutes(locationId)
        return fromTopoAndRouteDto(topoAndRoutes)
    }

    @WorkerThread
    fun getToposForCrag(cragId: String): List<TopoAndRoutes> {
        Timber.d("Getting topos for crag with id: %s", cragId)
        val toposAndRoutes = ArrayList<TopoAndRoutes>()
        val sectorsForCrag = locationDao.getWithParentId(cragId)
        for (sector in sectorsForCrag) {
            val topoAndRoutesDto = topoDao.getTopoAndRoutes(sector.id)
            toposAndRoutes.addAll(fromTopoAndRouteDto(topoAndRoutesDto))
        }
        return toposAndRoutes
    }

    fun search(query: String): LiveData<List<Topo>> {
        Timber.d("Search topos: %s", query)
        val liveSearchData = topoDao.searchOnName("%$query%")
        return Transformations.map(liveSearchData) { topos ->
            topos.map { topo ->
                fromTopoDto(topo)
            }
        }
    }
}