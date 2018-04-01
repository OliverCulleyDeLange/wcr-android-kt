package uk.co.oliverdelange.wcr_android_kt.db

import android.arch.persistence.room.*
import uk.co.oliverdelange.wcr_android_kt.model.Location

@Database(entities = [(Location::class)], version = 1)
@TypeConverters(WcrTypeConverters::class)
abstract class WcrDb : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(location: Location)
}
