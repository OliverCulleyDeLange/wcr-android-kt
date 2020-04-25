package uk.co.oliverdelange.wcr_android_kt.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Completable
import io.reactivex.Single
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb
import uk.co.oliverdelange.wcr_android_kt.db.dao.remote.uploadThingsToFirebase
//TODO Test me
class UploadWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Syncing local things to firestore")
        val localDb = WcrDb.getInstance(applicationContext)
        val syncStartTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

        val uploadLocations = localDb.locationDao().loadYetToBeUploaded()
                .flatMapCompletable(uploadThingsToFirebase("locations", localDb.locationDao(), syncStartTime))
        val uploadTopos = localDb.topoDao().loadYetToBeUploaded()
                .flatMapCompletable(uploadThingsToFirebase("topos", localDb.topoDao(), syncStartTime))
        val uploadRoutes = localDb.routeDao().loadYetToBeUploaded()
                .flatMapCompletable(uploadThingsToFirebase("routes", localDb.routeDao(), syncStartTime))

        return Completable.mergeDelayError(listOf(uploadLocations, uploadTopos, uploadRoutes))
                .toSingleDefault(Result.success())
                .onErrorReturn {
                    FirebaseAnalytics.getInstance(applicationContext)
                            .logEvent("wcr_sync_upload_failed", Bundle().apply { putString("error", it.message) })
                    Result.failure()
                }
    }
}