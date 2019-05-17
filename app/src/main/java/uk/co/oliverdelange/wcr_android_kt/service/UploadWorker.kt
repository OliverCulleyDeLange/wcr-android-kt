package uk.co.oliverdelange.wcr_android_kt.service

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.BaseDao
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.BaseEntity
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb

fun <T : BaseEntity> uploadToFirebase(collection: String, entity: T): Single<T> {
    val firebaseFirestore = FirebaseFirestore.getInstance()
    return Single.create<T> { emitter ->
        val classname = entity.javaClass.simpleName
        Timber.d("Saving $classname to firestore : ${entity.id}")
        val setTask = firebaseFirestore.collection(collection)
                .document(entity.id)
                .set(entity)
        try {
            //TODO Write blog post on how to do this!
            Tasks.await(setTask)
            Timber.d("$classname saved to firestore: ${entity.id} ")
            emitter.onSuccess(entity)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add $classname to firestore: ${entity.id}")
            emitter.onError(e)
        }
    }
}

private fun <T : BaseEntity> uploadThings(collection: String, dao: BaseDao<T>, syncStartTime: Long): (List<T>) -> Completable {
    return { things ->
        Timber.d("$collection yet to be uploaded: ${things.map { it.id }}")
        val saveToFirestore = things.map {
            it.uploadedAt = syncStartTime
            uploadToFirebase(collection, it).toObservable()
        }
        Observable.mergeArrayDelayError(*saveToFirestore.toTypedArray())
                .flatMapCompletable {
                    Timber.v("Marking Location $it as uploaded")
                    dao.updateUploadedAt(it.id, it.uploadedAt)
                }
    }
}

class UploadWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Syncing local things to firestore")
        val localDb = WcrDb.getInstance(applicationContext)
        val syncStartTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

        val uploadLocations = localDb.locationDao().loadYetToBeUploaded()
                .flatMapCompletable(uploadThings("locations", localDb.locationDao(), syncStartTime))
        val uploadTopos = localDb.topoDao().loadYetToBeUploaded()
                .flatMapCompletable(uploadThings("topos", localDb.topoDao(), syncStartTime))
        val uploadRoutes = localDb.routeDao().loadYetToBeUploaded()
                .flatMapCompletable(uploadThings("routes", localDb.routeDao(), syncStartTime))

        return Completable.mergeDelayError(listOf(uploadLocations, uploadTopos, uploadRoutes))
                .toSingleDefault(Result.success())
    }
}