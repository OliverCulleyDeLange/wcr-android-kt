package uk.co.oliverdelange.wcr_android_kt.db.dao.local

import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Maybe
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.MostRecentSync
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Sync

@Dao
@WorkerThread
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(obj: Sync): Long

    @Query("SELECT MAX(epochSeconds) as epochSeconds FROM sync where syncType = :syncType")
    fun getMostRecentSync(syncType: String): Maybe<MostRecentSync>
}

