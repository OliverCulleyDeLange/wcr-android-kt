package uk.co.oliverdelange.wcr_android_kt.service

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.Location
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb

class SyncLocationsWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {
    override fun getBackgroundScheduler(): Scheduler {
        return Schedulers.io()
    }

    override fun createWork(): Single<Result> {
        Timber.d("${Thread.currentThread().name} Syncing local locations to firestore")
        val localDb = WcrDb.getInstance(applicationContext)
        val remoteDb = FirebaseFirestore.getInstance()
        return localDb
                .locationDao()
                .loadWithUploaded(false)
                .flatMapCompletable { locations ->
                    Timber.d("${Thread.currentThread().name} Locations yet to be uploaded: ${locations.map { it.id }}")
                    val saveLocationsToFirestore = locations.map { saveLocationToRemoteDb(remoteDb, it).toObservable() }
                    Observable.mergeArrayDelayError(*saveLocationsToFirestore.toTypedArray())
                            .flatMapCompletable {
                                Timber.d("${Thread.currentThread().name} Marking Location $it as uploaded")
                                localDb.locationDao().markAsUploaded(it)
                                        .subscribeOn(Schedulers.io())
                            }
                }
                .toSingleDefault(Result.success())
    }

    private fun saveLocationToRemoteDb(firebaseFirestore: FirebaseFirestore, location: Location): Single<String> {
        return Single.create<String> { emitter ->
            //            Timber.d("Saving location to firestore : ${location.id}")
            firebaseFirestore.collection("locations")
                    .document(location.id)
                    .set(location)
                    .addOnSuccessListener {
                        Timber.d("${Thread.currentThread().name} Location saved to firestore: ${location.id} ")
                        emitter.onSuccess(location.id)
                    }
                    .addOnFailureListener { err ->
                        Timber.e(err, "Failed to add location to firestore: ${location.id}")
                        emitter.onError(err)
                        //         TODO Show error if not signed in
                        //        if (err is FirebaseFirestoreException && err.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        //            Timber.d("User tried to submit when not logged in")
                        //        }
                    }
        }
    }
}