package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.LocationDao
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.TopoDao
import uk.co.oliverdelange.wcr_android_kt.mapper.fromTopoAndRouteDto
import uk.co.oliverdelange.wcr_android_kt.mapper.fromTopoDto
import uk.co.oliverdelange.wcr_android_kt.mapper.toTopoDto
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import javax.inject.Inject

class TopoRepository @Inject constructor(val topoDao: TopoDao,
                                         val locationDao: LocationDao) {

    fun save(topo: Topo): Single<Long> {
        Timber.d("Saving topo %s", topo.name)
        val topoDTO = toTopoDto(topo)
        return saveToLocalDb(topoDTO)
    }

    private fun saveToLocalDb(topoDTO: uk.co.oliverdelange.wcr_android_kt.db.dto.local.Topo): Single<Long> {
        return Single.fromCallable {
            val id = topoDao.insert(topoDTO)
            Timber.d("Saved topo $id to local db: %s", topoDTO)
            id
        }
    }

    fun loadToposForLocation(locationId: Long): LiveData<List<TopoAndRoutes>> {
        Timber.d("Loading topos for location with id: %s", locationId)
        val liveTopoAndRoutesDto = topoDao.loadTopoAndRoutes(locationId)
        return Transformations.map(liveTopoAndRoutesDto) { fromTopoAndRouteDto(it) }
    }

    @WorkerThread
    fun getToposForLocation(locationId: Long): List<TopoAndRoutes> {
        Timber.d("Getting topos for location with id: %s", locationId)
        val topoAndRoutes = topoDao.getTopoAndRoutes(locationId)
        return fromTopoAndRouteDto(topoAndRoutes)
    }

    @WorkerThread
    fun getToposForCrag(cragId: Long): List<TopoAndRoutes> {
        Timber.d("Getting topos for crag with id: %s", cragId)
        val toposAndRoutes = ArrayList<TopoAndRoutes>()
        val sectorsForCrag = locationDao.getWithParentId(cragId)
        for (sector in sectorsForCrag) {
            val topoAndRoutesDto = topoDao.getTopoAndRoutes(sector.id)
            toposAndRoutes.addAll(fromTopoAndRouteDto(topoAndRoutesDto))
        }
        return toposAndRoutes
    }

    fun search(query: String): LiveData<List<Topo>> {
        Timber.d("Search topos: %s", query)
        val liveSearchData = topoDao.searchOnName("%$query%")
        return Transformations.map(liveSearchData) { topos ->
            topos.map { topo ->
                fromTopoDto(topo)
            }
        }
    }
}