package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.Route
import uk.co.oliverdelange.wcr_android_kt.db.RouteDao
import uk.co.oliverdelange.wcr_android_kt.mapper.toRouteDto
import uk.co.oliverdelange.wcr_android_kt.service.SyncRoutesWorker
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class RouteRepository @Inject constructor(val routeDao: RouteDao,
                                          val firebaseFirestore: FirebaseFirestore,
                                          val appExecutors: AppExecutors) {

    fun save(route: uk.co.oliverdelange.wcr_android_kt.model.Route): Completable {
        val routeDTO = toRouteDto(route)
        WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<SyncRoutesWorker>()
                .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build())
        return saveToLocalDb(routeDTO)
    }

    private fun saveToLocalDb(routeDTO: Route): Completable {
        return Completable.fromAction {
            Timber.d("Saving route to local db: %s", routeDTO.name)
            routeDao.insert(routeDTO)
        }
    }


//    private fun saveToRemoteDb(route: Route) {
//        Timber.d("Saving Route to firestore: ${route.name}")
//        topoFirestoreDocument.collection("routes")
//                .document(route.id)
//                .set(route)
//                .addOnSuccessListener {
//                    Timber.d("Route saved to firestore: ${route.name}")
//                }
//                .addOnFailureListener {
//                    Timber.e(it, "failed to add location to firestore")
//                }
//    }

}