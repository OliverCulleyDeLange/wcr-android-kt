package uk.co.oliverdelange.wcr_android_kt.db.dao.local

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.reactivex.Completable
import io.reactivex.Maybe
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Topo
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.TopoAndRoutes

@Dao
@WorkerThread
interface TopoDao : BaseDao<Topo> {
    @Query("SELECT * from topo where id = :id")
    fun get(id: String): Topo

    @Transaction
    @Query("SELECT * from topo where locationId = :locationId")
    fun loadTopoAndRoutes(locationId: String): LiveData<List<TopoAndRoutes>?>

    @Transaction
    @Query("SELECT * from topo where locationId = :locationId")
    fun getTopoAndRoutes(locationId: String): List<TopoAndRoutes>?

    @Query("SELECT * FROM topo where uploadedAt= -1")
    override fun loadYetToBeUploaded(): Maybe<List<Topo>>

    @Query("UPDATE topo SET uploadedAt = :uploadedAt where id = :id")
    override fun updateUploadedAt(id: String, uploadedAt: Long): Completable

    //https://developer.android.com/training/data-storage/room/accessing-data
    @Query("SELECT * FROM topo WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Topo>?>
}