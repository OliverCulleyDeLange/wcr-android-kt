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
import uk.co.oliverdelange.wcr_android_kt.db.Location
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb

//TODO DRY up these sync workers... lots of copy psste code
class SyncLocationsWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Syncing local locations to firestore")
        val localDb = WcrDb.getInstance(applicationContext)
        val remoteDb = FirebaseFirestore.getInstance()
        return localDb
                .locationDao()
                .loadWithUploaded(false)
                .flatMapCompletable { locations ->
                    Timber.d("Locations yet to be uploaded: ${locations.map { it.id }}")
                    val saveLocationsToFirestore = locations.map { saveLocationToRemoteDb(remoteDb, it).toObservable() }
                    Observable.mergeArrayDelayError(*saveLocationsToFirestore.toTypedArray())
                            .flatMapCompletable {
                                Timber.d("Marking Location $it as uploaded")
                                localDb.locationDao().markAsUploaded(it)
                            }
                }
                .toSingleDefault(Result.success())
    }

    private fun saveLocationToRemoteDb(firebaseFirestore: FirebaseFirestore, location: Location): Single<String> {
        return Single.create<String> { emitter ->
            Timber.d("Saving location to firestore : ${location.id}")
            val setTask = firebaseFirestore.collection("locations")
                    .document(location.id)
                    .set(location)
            try {
                //TODO Write blog post on how to do this!
                Tasks.await(setTask)
                Timber.d("Location saved to firestore: ${location.id} ")
                emitter.onSuccess(location.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add location to firestore: ${location.id}")
                emitter.onError(e)
            }
        }
    }
}