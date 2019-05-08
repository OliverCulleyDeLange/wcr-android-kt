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
import uk.co.oliverdelange.wcr_android_kt.db.Topo
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb

class SyncToposWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    @SuppressLint("WrongThread")
    override fun createWork(): Single<Result> {
        Timber.d("Syncing local topos to firestore")
        val localDb = WcrDb.getInstance(applicationContext)
        val remoteDb = FirebaseFirestore.getInstance()
        return localDb
                .topoDao()
                .loadWithUploaded(false)
                .flatMapCompletable { topos ->
                    Timber.d("Topos yet to be uploaded: ${topos.map { it.id }}")
                    val saveToposToFirestore = topos.map { saveTopoToRemoteDb(remoteDb, it).toObservable() }
                    Observable.mergeArrayDelayError(*saveToposToFirestore.toTypedArray())
                            .flatMapCompletable {
                                Timber.d("Marking Topo $it as uploaded")
                                localDb.topoDao().markAsUploaded(it)
                            }
                }
                .toSingleDefault(Result.success())
    }

    private fun saveTopoToRemoteDb(firebaseFirestore: FirebaseFirestore, topo: Topo): Single<String> {
        return Single.create<String> { emitter ->
            Timber.d("Saving topo to firestore : ${topo.id}")
            val setTask = firebaseFirestore.collection("topos")
                    .document(topo.id)
                    .set(topo)
            try {
                Tasks.await(setTask)
                Timber.d("Topo saved to firestore: ${topo.id} ")
                emitter.onSuccess(topo.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add topo to firestore: ${topo.id}")
                emitter.onError(e)
            }
        }
    }
}