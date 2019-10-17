package uk.co.oliverdelange.wcr_android_kt.db.dao.local

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Maybe
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Route

@Dao
@WorkerThread
interface RouteDao : BaseDao<Route> {
    @Query("SELECT * from route where id = :id")
    fun get(id: String): Route?

    @Query("SELECT * from route where topoId = :topoId")
    fun loadWithTopoId(topoId: String): LiveData<List<Route>?>

    @Query("SELECT * FROM route where uploadedAt= -1")
    override fun loadYetToBeUploaded(): Maybe<List<Route>>

    @Query("UPDATE route SET uploadedAt = :uploadedAt where id = :id")
    override fun updateUploadedAt(id: String, uploadedAt: Long): Completable

    @Query("SELECT * FROM route WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Route>?>
}