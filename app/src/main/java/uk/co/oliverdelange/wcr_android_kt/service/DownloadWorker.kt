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
import uk.co.oliverdelange.wcr_android_kt.db.*
import uk.co.oliverdelange.wcr_android_kt.model.SyncType
import kotlin.reflect.KClass

fun <T : Any> getFirebaseThings(mostRecentDownload: MostRecentSync, collection: String, kClass: KClass<T>): Observable<T> {
    val remoteDb = FirebaseFirestore.getInstance()
    return Maybe.create<List<DocumentSnapshot>> { emitter ->
        val task = remoteDb
                .collection(collection)
                .whereGreaterThan("uploadedAt", mostRecentDownload.epochSeconds)
                .get()
        try {
            val data = Tasks.await(task).documents
            emitter.onSuccess(data)
            emitter.onComplete()
        } catch (e: Exception) {
            Timber.e(e, "Error downloading $collection from firestore")
            emitter.onError(e)
        }
    }.flatMapObservable {
        Observable.fromIterable(it.asIterable())
    }.map { doc ->
        doc.toObject(kClass.java)
    }
}

fun <T : BaseEntity> downloadThing(mostRecentDownload: MostRecentSync, collection: String, kClass: KClass<T>, dao: BaseDao<T>): Single<MutableList<String>> {
    Timber.d("Getting all remote $collection since ${mostRecentDownload.epochSeconds}")
    return getFirebaseThings(mostRecentDownload, collection, kClass)
            .concatMapDelayError {
                Timber.v("Inserting ${kClass.java.simpleName} ${it.id}")
                dao.insert(it)
                Observable.just(it)
            }.collect({ mutableListOf<String>() }, { list, it -> list.add(it.id) })
            .doOnSuccess {
                Timber.d("Downloaded ${it.size} ${kClass.java.simpleName} from firestore: ${it}")
            }

}

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {
    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Downloading remote data to local db")
        val localDb = WcrDb.getInstance(applicationContext)
        val locationDao = localDb.locationDao()
        val topoDao = localDb.topoDao()
        val routeDao = localDb.routeDao()
        val syncStartTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

        return localDb.syncDao()
                .getMostRecentSync(SyncType.DOWNLOAD.name)
                .doAfterSuccess {
                    Timber.d("Most recent sync: ${it.epochSeconds}")
                }.flatMapPublisher {
                    val downloadedLocationIds: Single<MutableList<String>> = downloadThing(it, "locations", Location::class, locationDao)
                    val downloadedTopoIds: Single<MutableList<String>> = downloadThing(it, "topos", Topo::class, topoDao)
                    val downloadedRouteIds: Single<MutableList<String>> = downloadThing(it, "routes", Route::class, routeDao)
                    Single.mergeDelayError(downloadedLocationIds, downloadedTopoIds, downloadedRouteIds)
                }.collect({ mutableListOf<String>() }, { list, it ->
                    Timber.v("IDs of downloaded things: $it")
                    list.addAll(it)
                })
                .flatMapCompletable {
                    Timber.d("Sync completed. Saving sync record. ")
                    val sync = Sync(
                            epochSeconds = syncStartTime,
                            syncType = SyncType.DOWNLOAD.name
                    )
                    localDb.syncDao().insert(sync)
                    Completable.complete()
                }.toSingle {
                    Result.success()
                }.doOnError {
                    Timber.e(it, "Error downloading things from remote db")
                }
    }
}