package uk.co.oliverdelange.wcr_android_kt.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.support.annotation.WorkerThread
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.model.LocationType.SECTOR

@Database(entities = [(Location::class), (Topo::class), (Route::class), (Grade::class)], version = 1)
@TypeConverters(WcrTypeConverters::class)
abstract class WcrDb : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun topoDao(): TopoDao
    abstract fun routeDao(): RouteDao
}

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(obj: T): Long
}

@Dao
interface LocationDao : BaseDao<Location> {
    @Query("SELECT * FROM location where id = :id")
    fun load(id: Long): LiveData<Location>

    @WorkerThread
    @Query("SELECT * FROM location where id = :id")
    fun get(id: Long): Location?

    @Query("SELECT * FROM location where type = :type")
    fun load(type: LocationType): LiveData<List<Location>>

    @Query("SELECT * FROM location where type = :type AND parentId = :id")
    fun loadWithParentId(id: Long, type: LocationType = SECTOR): LiveData<List<Location>>

    @WorkerThread
    @Query("SELECT * FROM location where type = :type AND parentId = :id")
    fun getWithParentId(id: Long, type: LocationType = SECTOR): List<Location>

    @Query("UPDATE location SET boulders = :boulders, sports = :sports, trads = :trads, greens = :greens, oranges = :oranges, reds = :reds, blacks = :blacks WHERE id =:id")
    fun updateRouteInfo(id: Long, boulders: Int, sports: Int, trads: Int, greens: Int, oranges: Int, reds: Int, blacks: Int)

    @Query("SELECT * FROM location WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Location>>
}

@Dao
interface TopoDao : BaseDao<Topo> {
    @Query("SELECT * from topo where locationId = :locationId")
    fun loadTopoAndRoutes(locationId: Long): LiveData<List<TopoAndRoutes>>

    @WorkerThread
    @Query("SELECT * from topo where locationId = :locationId")
    fun getTopoAndRoutes(locationId: Long): List<TopoAndRoutes>
}

@Dao
interface RouteDao : BaseDao<Route> {
    @Query("SELECT * from route where topoId = :topoId")
    fun loadWithTopoId(topoId: Long): LiveData<List<Route>>
}
