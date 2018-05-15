package uk.co.oliverdelange.wcr_android_kt.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.content.Context
import android.support.annotation.WorkerThread
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.model.LocationType.CRAG
import uk.co.oliverdelange.wcr_android_kt.model.LocationType.SECTOR
import uk.co.oliverdelange.wcr_android_kt.util.ioThread

@Database(entities = [(Location::class), (Topo::class), (Route::class), (Grade::class)], version = 1)
@TypeConverters(WcrTypeConverters::class)
abstract class WcrDb : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun topoDao(): TopoDao
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: WcrDb? = null

        fun getInstance(context: Context): WcrDb =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(context.applicationContext, WcrDb::class.java, "wcr.db")
                // prepopulate the database after onCreate was called
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // insert the data on the IO Thread
                        ioThread {
                            getInstance(context).locationDao().insertMany(
                                    Location(0, null, "London", 51.507363, -0.127755, CRAG, greens = 3, reds = 1),
                                    Location(1, null, "Derby", 52.923429, -1.471682, CRAG),
                                    Location(2, 0, "The Arch - Biscuit Factory", 51.494365, -0.062352, SECTOR, greens = 3, reds = 1)
                            )
                            getInstance(context).topoDao().insertMany(
                                    Topo(0, 2, "Buttery Biscuit Base", "http://res.cloudinary.com/he5sr1yd9/image/upload/v1525733335/topo/4/Topo.webp"),
                                    Topo(1, 2, "The Dunker", "http://res.cloudinary.com/he5sr1yd9/image/upload/v1525731153/topo/3/Topo1.webp")
                            )
                            getInstance(context).routeDao().insertMany(
                                    Route(0, 0, "A biscuit in the mouth is worth two in the hand", Grade.Companion.from(VGrade.V3), RouteType.BOULDERING, "Eating biscuits is good for you"),
                                    Route(1, 0, "Custard Creams", Grade.Companion.from(FontGrade.fFour), RouteType.BOULDERING, "Mmmmm creamy custard"),
                                    Route(2, 0, "Bourbons", Grade.Companion.from(TradAdjectivalGrade.E1, TradTechnicalGrade.FiveB), RouteType.TRAD, "Traditional Rich Tea or Digestive?"),
                                    Route(3, 1, "Fox's Biscuits", Grade.Companion.from(SportGrade.FourB), RouteType.SPORT, "Excuisite")
                            )
                        }
                    }
                })
                .build()

    }
}

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMany(vararg objs: T): Array<Long>
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

    //https://developer.android.com/training/data-storage/room/accessing-data
    @Query("SELECT * FROM topo WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Topo>>
}

@Dao
interface RouteDao : BaseDao<Route> {
    @Query("SELECT * from route where topoId = :topoId")
    fun loadWithTopoId(topoId: Long): LiveData<List<Route>>

    @Query("SELECT * FROM route WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Route>>
}
