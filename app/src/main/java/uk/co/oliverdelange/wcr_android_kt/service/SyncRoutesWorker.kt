package uk.co.oliverdelange.wcr_android_kt.service

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.Route
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb

class SyncRoutesWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Syncing local routes to firestore")
        val localDb = WcrDb.getInstance(applicationContext)
        val remoteDb = FirebaseFirestore.getInstance()
        return localDb
                .routeDao()
                .loadWithUploaded(false)
                .flatMapCompletable { routes ->
                    Timber.d("routes yet to be uploaded: ${routes.map { it.id }}")
                    val saveRoutesToFirestore = routes.map { saveRouteToRemoteDb(remoteDb, it).toObservable() }
                    Observable.mergeArrayDelayError(*saveRoutesToFirestore.toTypedArray())
                            .flatMapCompletable {
                                Timber.d("Marking Route $it as uploaded")
                                localDb.routeDao().markAsUploaded(it)
                            }
                }
                .toSingleDefault(Result.success())
    }

    private fun saveRouteToRemoteDb(firebaseFirestore: FirebaseFirestore, route: Route): Single<String> {
        return Single.create<String> { emitter ->
            Timber.d("Saving route to firestore : ${route.id}")
            val setTask = firebaseFirestore.collection("routes")
                    .document(route.id)
                    .set(route)
            try {
                Tasks.await(setTask)
                Timber.d("Route saved to firestore: ${route.id} ")
                emitter.onSuccess(route.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add route to firestore: ${route.id}")
                emitter.onError(e)
            }
        }
    }
}