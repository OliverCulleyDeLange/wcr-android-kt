package uk.co.oliverdelange.wcr_android_kt.db.dao.local

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Maybe
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Location

@Dao
@WorkerThread
interface LocationDao : BaseDao<Location> {
    @Query("SELECT * FROM location where name = :name")
    fun load(name: String): LiveData<Location>

    @Query("SELECT * FROM location where name = :name")
    fun get(name: String): Location?

    @Query("SELECT * FROM location where type = :type")
    fun loadByType(type: String): LiveData<List<Location>>

    @Query("SELECT * FROM location where type = :type AND parentLocation = :parent")
    fun loadWithParentId(parent: String, type: String = "SECTOR"): LiveData<List<Location>>

    @Query("SELECT * FROM location where uploadedAt = -1")
    override fun loadYetToBeUploaded(): Maybe<List<Location>>

    @Query("UPDATE location SET uploadedAt = :uploadedAt where id = :id")
    override fun updateUploadedAt(id: String, uploadedAt: Long): Completable

    @Query("SELECT * FROM location where type = :type AND parentLocation = :parent")
    fun getWithParentId(parent: String, type: String = "SECTOR"): List<Location>

    @Query("UPDATE location SET boulders = :boulders, sports = :sports, trads = :trads, greens = :greens, oranges = :oranges, reds = :reds, blacks = :blacks WHERE name =:name")
    fun updateRouteInfo(name: String, boulders: Int, sports: Int, trads: Int, greens: Int, oranges: Int, reds: Int, blacks: Int)

    @Query("SELECT * FROM location WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Location>>
}