package uk.co.oliverdelange.wcr_android_kt.db.dao.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.BaseDao
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.BaseEntity

private fun <T : BaseEntity> uploadToFirebase(collection: String, entity: T): Single<T> {
    val firebase = FirebaseFirestore.getInstance()
    return Single.create<T> { emitter ->
        val classname = entity.javaClass.simpleName
        Timber.v("Saving $classname to firestore. id: ${entity.id}")
        val addTask = firebase.collection(collection)
                .document(entity.id)
                .set(entity)
        try {
            Tasks.await(addTask)
            Timber.v("$classname saved to firestore. id: ${entity.id}")
            emitter.onSuccess(entity)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add $classname to firestore: ${entity.id}")
            emitter.onError(e)
        }
    }
}

fun <T : BaseEntity> uploadThingsToFirebase(collection: String, dao: BaseDao<T>, syncStartTime: Long): (List<T>) -> Completable {
    return { things ->
        Timber.d("$collection yet to be uploaded: ${things.map { it.id }}")
        val saveToFirestore = things.map {
            it.uploaderId = FirebaseAuth.getInstance().currentUser?.uid ?: "UNKNOWN"
            it.uploadedAt = syncStartTime
            uploadToFirebase(collection, it).toObservable()
        }
        if (saveToFirestore.isEmpty()) {
            Completable.complete()
        } else {
            //TODO Test: All uploaded items are marked as uploaded even if some fail
            Observable.mergeArrayDelayError(*saveToFirestore.toTypedArray()) // This will swallow errors if there are more than one. Problem?
                    .flatMapCompletable {
                        Timber.d("Marking ${it.id} as uploaded")
                        dao.updateUploadedAt(it.id, it.uploadedAt)
                        // Future note: we don't need to update the uploaderId because its all in firebase.
                    }
        }
    }
}