package uk.co.oliverdelange.wcr_android_kt.service

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.Location
import uk.co.oliverdelange.wcr_android_kt.db.Sync
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb
import uk.co.oliverdelange.wcr_android_kt.model.SyncType

class DownloadLocationsWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {
    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Downloading remote locations to local db")
        val localDb = WcrDb.getInstance(applicationContext)
        val locationDao = localDb.locationDao()
        val remoteDb = FirebaseFirestore.getInstance()
        val syncStartTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

        return localDb.syncDao()
                .getMostRecentSync(SyncType.DOWNLOAD.name)
                .flatMap { mostRecentDownload ->
                    Timber.d("Getting all remote locations since ${mostRecentDownload.epochSeconds}")
                    Maybe.create<List<DocumentSnapshot>> { emitter ->
                        val getLocationsTask = remoteDb
                                .collection("locations")
                                .whereGreaterThan("uploadedAt", mostRecentDownload.epochSeconds)
                                .get()

                        try {
                            val remoteLocations = Tasks.await(getLocationsTask).documents
                            emitter.onSuccess(remoteLocations)
                            emitter.onComplete()
                        } catch (e: Exception) {
                            Timber.e(e, "Error downloading locations from firestore")
                            emitter.onError(e)
                        }
                    }
                }.flatMapObservable {
                    Observable.fromIterable(it.asIterable())
                }.map { doc ->
                    val location: Location? = doc.toObject(Location::class.java)
                    location
                }.filter {
                    it != null
                }.concatMapDelayError {
                    locationDao.insert(it)
                    Observable.just(it)
                }.collect({ mutableListOf<String>() }, { list, it -> list.add(it.id) })
                .flatMapCompletable {
                    Timber.d("Downloaded ${it.size} locations from firestore: ${it}")
                    val sync = Sync(
                            epochSeconds = syncStartTime,
                            syncType = SyncType.DOWNLOAD.name,
                            successIds = "" //TODO
//                            successIds = it
                    )
                    localDb.syncDao().insert(sync)
                    Completable.complete()
                }.toSingle { Result.success() }
                .doOnError {
                    Timber.e(it, "Error downloading locations from remote db")
                }
    }
}