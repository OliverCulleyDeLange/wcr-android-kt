package uk.co.oliverdelange.wcr_android_kt.db.dao.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.BaseDao
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.BaseEntity
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.MostRecentSync
import kotlin.reflect.KClass

private fun <T : BaseEntity> getFromFirebase(mostRecentDownload: MostRecentSync, collection: String, kClass: KClass<T>): Observable<T> {
    Timber.d("Getting all remote $collection since ${mostRecentDownload.epochSeconds} from firebase")
    val firebase = FirebaseFirestore.getInstance()
    return Maybe.create<List<DocumentSnapshot>> { emitter ->
        val task = firebase
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


fun <T : BaseEntity> saveFromFirebase(mostRecentDownload: MostRecentSync,
                                      collection: String,
                                      kClass: KClass<T>,
                                      dao: BaseDao<T>): Single<MutableList<Long>> {
    return getFromFirebase(mostRecentDownload, collection, kClass)
            .concatMapDelayError {
                Timber.v("Inserting into db: ${kClass.java.simpleName} ${it.id}")
                dao.insert(it)
                Observable.just(it)
            }.collect({ mutableListOf<Long>() }, { list, it -> list.add(it.id) })
            .doOnSuccess {
                Timber.d("Downloaded ${it.size} ${kClass.java.simpleName} from firestore: $it")
            }

}