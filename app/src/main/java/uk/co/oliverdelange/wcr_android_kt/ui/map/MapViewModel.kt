package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.lifecycle.*
import android.databinding.ObservableBoolean
import android.support.design.widget.BottomSheetBehavior
import android.view.View
import com.google.android.gms.maps.GoogleMap
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.util.AbsentLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapViewModel @Inject constructor(val locationRepository: LocationRepository,
                                       val topoRepository: TopoRepository) : ViewModel() {

    val showFab = ObservableBoolean(true)
    val mapType: MutableLiveData<Int> = MutableLiveData<Int>().also {
        it.value = GoogleMap.MAP_TYPE_NORMAL
    }
    val mapLabel: LiveData<String> = Transformations.map(mapType) {
        if (it == 1) "SAT" else "MAP"
    }
    val mapMode: MutableLiveData<MapMode> = MutableLiveData<MapMode>().also {
        it.value = MapMode.DEFAULT_MODE
    }

    val selectedLocationId: MutableLiveData<Long?> = MutableLiveData<Long?>().also {
        it.value = null
    }
    val selectedLocation: LiveData<Location?> = Transformations.switchMap(selectedLocationId) {
        if (it != null) {
            locationRepository.load(it)
        } else {
            AbsentLiveData.create()
        }
    }

    val crags: LiveData<List<Location>> = locationRepository.loadCrags()
    val sectors: LiveData<List<Location>> = Transformations.switchMap(selectedLocation) {
        when (it?.type) {
            LocationType.CRAG -> it.id?.let { locationRepository.loadSectorsFor(it) }
            LocationType.SECTOR -> it.parentId?.let { locationRepository.loadSectorsFor(it) }
            null -> AbsentLiveData.create()
        }
    }
    val topos: LiveData<List<TopoAndRoutes>> = Transformations.switchMap(selectedLocation) {
        when (it?.type) {
            LocationType.SECTOR -> it.id?.let { topoRepository.loadToposForLocation(it) }
            LocationType.CRAG -> it.id?.let { cragId -> getToposForCrag(cragId) }
            null -> AbsentLiveData.create()
        }
    }

    private fun getToposForCrag(cragId: Long): LiveData<List<TopoAndRoutes>> {
        val topos: MediatorLiveData<List<TopoAndRoutes>> = MediatorLiveData()
        val loadSectorsForCrag = locationRepository.loadSectorsFor(cragId)
        topos.addSource(loadSectorsForCrag) { sectorsForCrag ->
            topos.removeSource(loadSectorsForCrag)
            sectorsForCrag?.forEach { sector ->
                sector.id?.let { sectorId ->
                    topos.addSource(topoRepository.loadToposForLocation(sectorId)) {
                        it?.let { newToposAndRoutes ->
                            topos.value = newToposAndRoutes.plus(topos.value ?: emptyList())
                        }
                    }
                }
            }
        }
        return topos
    }

    val bottomSheetState: MutableLiveData<Int> = MutableLiveData()
    val bottomSheetTitle: LiveData<String> = Transformations.map(selectedLocation) {
        it?.name ?: "Select a crag or search"
    }

    fun submit(view: View) {
        when (mapMode.value) {
            MapMode.DEFAULT_MODE -> mapMode.value = MapMode.SUBMIT_CRAG_MODE
            MapMode.CRAG_MODE -> mapMode.value = MapMode.SUBMIT_SECTOR_MODE
            MapMode.SECTOR_MODE, MapMode.TOPO_MODE -> mapMode.value = MapMode.SUBMIT_TOPO_MODE
        }
    }

    fun toggleMap(view: View) {
        if (GoogleMap.MAP_TYPE_NORMAL == mapType.value) {
            mapType.value = GoogleMap.MAP_TYPE_SATELLITE
        } else {
            mapType.value = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    fun onCragClick(location: Location) {
        selectedLocationId.value = location.id
        mapMode.value = MapMode.CRAG_MODE
    }

    fun onSectorClick(location: Location) {
        selectedLocationId.value = location.id
        mapMode.value = MapMode.SECTOR_MODE
    }

    fun back() {
        when (mapMode.value) {
            MapMode.DEFAULT_MODE -> bottomSheetState.value = BottomSheetBehavior.STATE_COLLAPSED
            MapMode.CRAG_MODE -> {
                mapMode.value = MapMode.DEFAULT_MODE
                selectedLocationId.value = null
            }
            MapMode.SUBMIT_CRAG_MODE -> mapMode.value = MapMode.DEFAULT_MODE
            MapMode.SECTOR_MODE -> {
                mapMode.value = MapMode.CRAG_MODE
                selectedLocation.value?.parentId?.let { selectedLocationId.value = it }
            }
            MapMode.SUBMIT_SECTOR_MODE -> mapMode.value = MapMode.CRAG_MODE
            MapMode.TOPO_MODE -> mapMode.value = MapMode.SECTOR_MODE
            MapMode.SUBMIT_TOPO_MODE -> mapMode.value = MapMode.SECTOR_MODE
        }
    }
}

enum class MapMode {
    DEFAULT_MODE, CRAG_MODE, SECTOR_MODE, TOPO_MODE, SUBMIT_CRAG_MODE, SUBMIT_SECTOR_MODE, SUBMIT_TOPO_MODE
}