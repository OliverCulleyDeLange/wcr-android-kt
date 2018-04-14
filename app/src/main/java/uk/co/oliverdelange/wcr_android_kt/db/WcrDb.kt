package uk.co.oliverdelange.wcr_android_kt.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.model.LocationType.SECTOR

@Database(entities = [(Location::class)], version = 1)
@TypeConverters(WcrTypeConverters::class)
abstract class WcrDb : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(location: Location): Long

    @Query("SELECT * FROM location where id = :id")
    fun load(id: Long): LiveData<Location>

    @Query("SELECT * FROM location where type = :type")
    fun load(type: LocationType): LiveData<List<Location>>

    @Query("SELECT * FROM location where type = :type AND parentId = :id")
    fun loadWithParentId(id: Long, type: LocationType = SECTOR): LiveData<List<Location>>
}
