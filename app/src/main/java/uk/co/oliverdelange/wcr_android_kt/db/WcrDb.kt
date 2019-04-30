package uk.co.oliverdelange.wcr_android_kt.db

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
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
                                    Location("L0 London", null, "L0 London", 51.507363, -0.127755, CRAG),
                                    Location("L1 Derby", null, "L1 Derby", 52.923429, -1.471682, CRAG),
                                    Location("L2 The Arch - Biscuit Factory", "L0 London", "L2 The Arch - Biscuit Factory", 51.494365, -0.062352, SECTOR)
                            )
                            getInstance(context).topoDao().insertMany(
                                    Topo(0, "L2 The Arch - Biscuit Factory", "T0 Buttery Biscuit Base", "http://via.placeholder.com/640x480"),
                                    Topo(1, "L2 The Arch - Biscuit Factory", "T1 The Dunker", "http://via.placeholder.com/480x640"),
                                    Topo(2, "L2 The Arch - Biscuit Factory", "T2 Rich T", "http://via.placeholder.com/1280x480"),
                                    Topo(3, "L2 The Arch - Biscuit Factory", "T3 Caramel Digestif", "http://via.placeholder.com/1280x480")
                            )
                            getInstance(context).routeDao().insertMany(
                                    Route(0, 0, "0GREEN ROUTE", Grade.Companion.from(VGrade.V0), RouteType.BOULDERING, "Eating biscuits is good for you",
                                            setOf(Pair(0.25f, 0.25f), Pair(0.25f, 0.90f))),
                                    Route(1, 0, "0ORANGE ROUTE", Grade.Companion.from(FontGrade.fFourP), RouteType.BOULDERING, "Mmmmm creamy custard",
                                            setOf(Pair(0.35f, 0.25f), Pair(0.35f, 0.90f))),
                                    Route(2, 0, "0RED ROUTE", Grade.Companion.from(TradAdjectivalGrade.E1, TradTechnicalGrade.FiveB), RouteType.TRAD, "Traditional Rich Tea or Digestive?",
                                            setOf(Pair(0.45f, 0.25f), Pair(0.45f, 0.90f))),
                                    Route(3, 0, "0BLACK ROUTE", Grade.Companion.from(SportGrade.EightA), RouteType.SPORT, "Excuisite",
                                            setOf(Pair(0.55f, 0.25f), Pair(0.55f, 0.90f))),
                                    Route(4, 1, "1A biscuit based name that is really long so we know if things look okay when there are really long names", Grade.Companion.from(SportGrade.FourB), RouteType.SPORT, "Lol...",
                                            setOf(Pair(0.65f, 0.25f), Pair(0.65f, 0.90f))),
                                    Route(5, 2, "2GREEN ROUTE", Grade.Companion.from(VGrade.V0), RouteType.BOULDERING, "Eating biscuits is good for you",
                                            setOf(Pair(0.25f, 0.25f), Pair(0.25f, 0.90f))),
                                    Route(6, 2, "2ORANGE ROUTE", Grade.Companion.from(FontGrade.fFourP), RouteType.BOULDERING, "Mmmmm creamy custard",
                                            setOf(Pair(0.35f, 0.25f), Pair(0.35f, 0.90f))),
                                    Route(7, 2, "2RED ROUTE", Grade.Companion.from(TradAdjectivalGrade.E1, TradTechnicalGrade.FiveB), RouteType.TRAD, "Traditional Rich Tea or Digestive?",
                                            setOf(Pair(0.45f, 0.25f), Pair(0.45f, 0.90f))),
                                    Route(8, 2, "2BLACK ROUTE", Grade.Companion.from(SportGrade.EightA), RouteType.SPORT, "Excuisite",
                                            setOf(Pair(0.55f, 0.25f), Pair(0.55f, 0.90f))),
                                    Route(9, 3, "3A biscuit based name that is really long so we know if things look okay when there are really long names", Grade.Companion.from(SportGrade.FourB), RouteType.SPORT, "Lol...",
                                            setOf(Pair(0.75f, 0.25f), Pair(0.75f, 0.90f)))
                            )
                        }
                    }
                })
                .build()

    }
}

/*
* https://developer.android.com/training/data-storage/room/accessing-data#convenience-insert
* Insert can only return row-id from autogenerated primary key
* */
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMany(vararg objs: T): Array<Long>
}

@Dao
interface LocationDao : BaseDao<Location> {
    @Query("SELECT * FROM location where name = :name")
    fun load(name: String): LiveData<Location>

    @WorkerThread
    @Query("SELECT * FROM location where name = :name")
    fun get(name: String): Location?

    @Query("SELECT * FROM location where type = :type")
    fun load(type: LocationType): LiveData<List<Location>>

    @Query("SELECT * FROM location where type = :type AND parentLocation = :parent")
    fun loadWithParentId(parent: String, type: LocationType = SECTOR): LiveData<List<Location>>

    @WorkerThread
    @Query("SELECT * FROM location where type = :type AND parentLocation = :parent")
    fun getWithParentId(parent: String, type: LocationType = SECTOR): List<Location>

    @Query("UPDATE location SET boulders = :boulders, sports = :sports, trads = :trads, greens = :greens, oranges = :oranges, reds = :reds, blacks = :blacks WHERE name =:name")
    fun updateRouteInfo(name: String, boulders: Int, sports: Int, trads: Int, greens: Int, oranges: Int, reds: Int, blacks: Int)

    @Query("SELECT * FROM location WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Location>>
}

@Dao
interface TopoDao : BaseDao<Topo> {
    @WorkerThread
    @Query("SELECT * from topo where id = :id")
    fun get(id: Long): Topo

    @Query("SELECT * from topo where locationId = :locationId")
    fun loadTopoAndRoutes(locationId: String): LiveData<List<TopoAndRoutes>>

    @WorkerThread
    @Query("SELECT * from topo where locationId = :locationId")
    fun getTopoAndRoutes(locationId: String): List<TopoAndRoutes>

    //https://developer.android.com/training/data-storage/room/accessing-data
    @Query("SELECT * FROM topo WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Topo>>
}

@Dao
interface RouteDao : BaseDao<Route> {
    @WorkerThread
    @Query("SELECT * from route where id = :id")
    fun get(id: Long): Route

    @Query("SELECT * from route where topoId = :topoId")
    fun loadWithTopoId(topoId: Long): LiveData<List<Route>>

    @Query("SELECT * FROM route WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Route>>
}
