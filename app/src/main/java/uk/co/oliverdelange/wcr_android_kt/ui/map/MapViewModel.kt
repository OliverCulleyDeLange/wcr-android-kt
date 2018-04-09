package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import android.view.View
import com.google.android.gms.maps.GoogleMap
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapViewModel @Inject constructor(locationRepository: LocationRepository) : ViewModel() {

    val showFab = ObservableBoolean(true)
    val mapType: MutableLiveData<Int> = MutableLiveData<Int>().also {
        it.value = GoogleMap.MAP_TYPE_NORMAL
    }
    val mapLabel: LiveData<String> = Transformations.map(mapType) {
        if (it == 1) "SAT" else "MAP"
    }
    val mapMode: MutableLiveData<MapMode> = MutableLiveData<MapMode>().also {
        it.value = DEFAULT
    }

    val crags: LiveData<List<Location>> = locationRepository.loadCrags()
    val selectedLocation: MutableLiveData<Location?> = MutableLiveData<Location?>().also {
        it.value = null
    }

    val bottomSheetTitle: LiveData<String> = Transformations.map(selectedLocation) {
        it?.name ?: "Select a crag or search"
    }

    fun submit(view: View) {
        when (mapMode.value) {
            DEFAULT -> mapMode.value = SUBMIT_CRAG
            CRAG -> mapMode.value = SUBMIT_SECTOR
            SECTOR, TOPO -> mapMode.value = SUBMIT_TOPO
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
        selectedLocation.value = location
        mapMode.value = CRAG
    }

    fun back() {
        when (mapMode.value) {
            CRAG -> {
                mapMode.value = DEFAULT
                selectedLocation.value = null
            }
            SUBMIT_CRAG -> mapMode.value = DEFAULT
            SECTOR -> mapMode.value = CRAG
            SUBMIT_SECTOR -> mapMode.value = CRAG
            TOPO -> mapMode.value = SECTOR
            SUBMIT_TOPO -> mapMode.value = SECTOR
        }
    }
}

enum class MapMode {
    DEFAULT, CRAG, SECTOR, TOPO, SUBMIT_CRAG, SUBMIT_SECTOR, SUBMIT_TOPO
}