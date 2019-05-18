package uk.co.oliverdelange.wcr_android_kt.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.LocationDao
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.RouteDao
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.SyncDao
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.TopoDao
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Location
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Route
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Sync
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Topo

@Database(entities = [
    (Location::class),
    (Topo::class),
    (Route::class),
    (Sync::class)
], version = 1)
@TypeConverters(WcrTypeConverters::class)
abstract class WcrDb : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun topoDao(): TopoDao
    abstract fun routeDao(): RouteDao
    abstract fun syncDao(): SyncDao

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
                        preload(getInstance(context))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe()
                    }
                })
                .build()
    }
}