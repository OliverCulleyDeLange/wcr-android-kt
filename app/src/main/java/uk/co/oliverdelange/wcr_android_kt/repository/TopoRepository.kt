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

    fun save(topo: Topo): Single<String> {
        Timber.d("Saving topo %s", topo.name)
        val topoDTO = toTopoDto(topo)
        return saveToLocalDb(topoDTO)
    }

    private fun saveToLocalDb(topoDTO: uk.co.oliverdelange.wcr_android_kt.db.dto.local.Topo): Single<String> {
        return Single.fromCallable {
            topoDao.insert(topoDTO)
            Timber.d("Saved topo to local db: %s", topoDTO)
            topoDTO.id
        }
    }

    fun loadToposForLocation(locationId: String): LiveData<List<TopoAndRoutes>> {
        Timber.d("Loading topos for location with id: %s", locationId)
        val liveTopoAndRoutesDto = topoDao.loadTopoAndRoutes(locationId)
        return Transformations.map(liveTopoAndRoutesDto) {
            it?.let { fromTopoAndRouteDto(it) }
        }
    }

    @WorkerThread
    fun getToposForLocation(locationId: String): List<TopoAndRoutes>? {
        Timber.d("Getting topos for location with id: %s", locationId)
        val topoAndRoutes = topoDao.getTopoAndRoutes(locationId)
        return topoAndRoutes?.let { fromTopoAndRouteDto(it) }
    }

    @WorkerThread
    fun getToposForCrag(cragId: String): List<TopoAndRoutes> {
        Timber.d("Getting topos for crag with id: %s", cragId)
        val toposAndRoutes = mutableListOf<TopoAndRoutes>()
        val sectorsForCrag = locationDao.getWithParentId(cragId)
        if (sectorsForCrag != null) {
            for (sector in sectorsForCrag) {
                val topoAndRoutesDto = topoDao.getTopoAndRoutes(sector.id)
                topoAndRoutesDto?.let {
                    toposAndRoutes.addAll(fromTopoAndRouteDto(it))
                }
            }
        }
        return toposAndRoutes
    }

    fun search(query: String): LiveData<List<Topo>> {
        Timber.d("Search topos: %s", query)
        val liveSearchData = topoDao.searchOnName("%$query%")
        return Transformations.map(liveSearchData) { topos ->
            topos?.map { topo ->
                fromTopoDto(topo)
            }
        }
    }
}