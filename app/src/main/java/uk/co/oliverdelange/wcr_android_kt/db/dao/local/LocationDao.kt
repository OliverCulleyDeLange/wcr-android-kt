package uk.co.oliverdelange.wcr_android_kt.db.dao.local

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Maybe
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Location
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.LocationRouteInfo

@Dao
@WorkerThread
interface LocationDao : BaseDao<Location> {
    @Query("SELECT * FROM location where id = :id")
    fun load(id: String): LiveData<Location?>

    // https://stackoverflow.com/questions/4114940/select-random-rows-in-sqlite
//    @Query("SELECT id FROM location WHERE id IN (SELECT id FROM location ORDER BY RANDOM() LIMIT 1)")
    @Query("SELECT id FROM location where type = :type ORDER BY RANDOM() LIMIT 1")
    fun loadRandomId(type: String = "CRAG"): String?

    @Query("SELECT * FROM location where id = :id")
    fun get(id: String): Location?

    @Query("SELECT * FROM location where type = :type")
    fun loadByType(type: String): LiveData<List<Location>?>

    @Query("SELECT * FROM location where type = :type AND parentLocationId = :parentId")
    fun loadWithParentId(parentId: String, type: String = "SECTOR"): LiveData<List<Location>?>

    @Query("SELECT * FROM location where uploadedAt = -1")
    override fun loadYetToBeUploaded(): Maybe<List<Location>>

    @Query("UPDATE location SET uploadedAt = :uploadedAt where id = :id")
    override fun updateUploadedAt(id: String, uploadedAt: Long): Completable

    @Query("SELECT * FROM location where type = :type AND parentLocationId = :parent")
    fun getWithParentId(parent: String, type: String = "SECTOR"): List<Location>?

    @Query("SELECT * FROM location WHERE name LIKE :search")
    fun searchOnName(search: String): LiveData<List<Location>?>

    @Query("SELECT " +
            "SUM(case when gradeColour = 'GREEN' THEN 1 ELSE 0 END) as greens," +
            "SUM(case when gradeColour = 'ORANGE' THEN 1 ELSE 0 END) as oranges," +
            "SUM(case when gradeColour = 'RED' THEN 1 ELSE 0 END) as reds," +
            "SUM(case when gradeColour = 'BLACK' THEN 1 ELSE 0 END) as blacks," +
            "SUM(case when route.type = 'TRAD' THEN 1 ELSE 0 END) as trads," +
            "SUM(case when route.type = 'SPORT' THEN 1 ELSE 0 END) as sports," +
            "SUM(case when route.type = 'BOULDERING' THEN 1 ELSE 0 END) as boulders " +
            "FROM route " +
            "INNER JOIN topo ON route.topoId = topo.id " +
            "INNER JOIN location ON topo.locationId = location.id " +
            "WHERE location.parentLocationId = :id OR location.id = :id")
    fun getRouteInfo(id: String): LiveData<LocationRouteInfo?>
}