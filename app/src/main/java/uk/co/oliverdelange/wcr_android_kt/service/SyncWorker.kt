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


class SyncLocationsWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    @SuppressLint("WrongThread") //TODO is this right?
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
                            }
                }
                .toSingleDefault(Result.success())
    }

    private fun saveLocationToRemoteDb(firebaseFirestore: FirebaseFirestore, location: Location): Single<String> {
        return Single.create<String> { emitter ->
            Timber.d("${Thread.currentThread().name} Saving location to firestore : ${location.id}")
            val setTask = firebaseFirestore.collection("locations")
                    .document(location.id)
                    .set(location)
            try {
                //TODO Write blog post on how to do this!
                Tasks.await(setTask)
                Timber.d("${Thread.currentThread().name} Location saved to firestore: ${location.id} ")
                emitter.onSuccess(location.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add location to firestore: ${location.id}")
                emitter.onError(e)
                //         TODO Show error if not signed in
                //        if (err is FirebaseFirestoreException && err.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                //            Timber.d("User tried to submit when not logged in")
                //        }
            }
        }
    }
}

// Kotlin test script
//import io.reactivex.*
//import io.reactivex.android.schedulers.AndroidSchedulers
//import io.reactivex.schedulers.Schedulers
//
//fun saveRemote(id: String) : Single<String> {
//    println ("saveRemote $id ${Thread.currentThread().name}... ")
//    return Single.just(id)
//}
//
//fun markAsUploaded(id: String) : Completable {
//    println( "markAsUploaded $id ${Thread.currentThread().name}... ")
//    return Completable.complete()
//}
//
//println( "1 ${Thread.currentThread().name}")
//
//val source = Maybe.just(listOf("1","2","3"))
//val doStuff = source.flatMapCompletable { locations ->
//    println("2 ${Thread.currentThread().name}... ")
//    val observableArray = locations.map { saveRemote(it).toObservable() }.toTypedArray()
//    val uploadedLocationIdStream = Observable.mergeArrayDelayError(*observableArray)
//    uploadedLocationIdStream.flatMapCompletable {
//        println("3 ${Thread.currentThread().name}... ")
//        markAsUploaded(it)
//    }
//}
//
//doStuff
//.subscribeOn(Schedulers.io())
//.subscribe {
//    println ( "4... .. ")
//}
//
//Thread.sleep(3000)