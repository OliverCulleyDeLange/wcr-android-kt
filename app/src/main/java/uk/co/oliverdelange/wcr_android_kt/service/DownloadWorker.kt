package uk.co.oliverdelange.wcr_android_kt.service

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import io.reactivex.Completable
import io.reactivex.Single
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb
import uk.co.oliverdelange.wcr_android_kt.db.dao.remote.saveFromFirebase
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Location
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Route
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Sync
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Topo
import uk.co.oliverdelange.wcr_android_kt.model.SyncType

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {
    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Downloading remote data to local db")
        val localDb = WcrDb.getInstance(applicationContext)
        val syncStartTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

        return localDb.syncDao()
                .getMostRecentSync(SyncType.DOWNLOAD.name)
                .doAfterSuccess {
                    Timber.d("Most recent sync: ${it.epochSeconds}")
                }.flatMapPublisher {
                    val downloadedLocationIds = saveFromFirebase(it, "locations", Location::class, localDb.locationDao())
                    val downloadedTopoIds = saveFromFirebase(it, "topos", Topo::class, localDb.topoDao())
                    val downloadedRouteIds = saveFromFirebase(it, "routes", Route::class, localDb.routeDao())

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