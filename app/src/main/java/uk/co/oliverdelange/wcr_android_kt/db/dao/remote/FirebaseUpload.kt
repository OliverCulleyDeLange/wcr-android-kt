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

private fun <T : BaseEntity> uploadToFirebase(collection: String, entity: T): Single<Pair<String, T>> {
    val firebase = FirebaseFirestore.getInstance()
    return Single.create<Pair<String, T>> { emitter ->
        val classname = entity.javaClass.simpleName
        Timber.d("Saving $classname to firestore : ${entity.id}")
        val addTask = firebase.collection(collection).add(entity) //Autogenerates the firebase ID
        try {
            //TODO Write blog post on how to do this!
            val result = Tasks.await(addTask)
            Timber.v("$classname saved to firestore. id: ${entity.id} firestore id: ${result.id}")
            emitter.onSuccess(Pair(result.id, entity))
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
        Observable.mergeArrayDelayError(*saveToFirestore.toTypedArray())
                .flatMapCompletable {
                    val firebaseID = it.first
                    val uploadedItem = it.second
                    Timber.v("Setting ${uploadedItem.id}'s firebase ID: $firebaseID")
                    // TODO Set firebase ID
                    Timber.v("Marking ${uploadedItem.id} as uploaded")
                    dao.updateUploadedAt(uploadedItem.id, uploadedItem.uploadedAt)
                    // Future note: we don't need to update the uploaderId because its all in firebase.
                }
    }
}