package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.view.View
import com.google.android.gms.maps.GoogleMap

class MapViewModel : ViewModel() {

    val mapType: MutableLiveData<Int> = MutableLiveData<Int>().also {
        it.value = GoogleMap.MAP_TYPE_NORMAL
    }
    val mapLabel: LiveData<String> = Transformations.map(mapType) {
        if (it == 1) "SAT" else "MAP"
    }
    val mapMode: MutableLiveData<MapMode> = MutableLiveData<MapMode>().also {
        it.value = MapMode.DEFAULT
    }

    fun submit(view: View) {
        mapMode.value = MapMode.SUBMIT
    }

    fun toggleMap(view: View) {
        if (GoogleMap.MAP_TYPE_NORMAL == mapType.value) {
            mapType.value = GoogleMap.MAP_TYPE_SATELLITE
        } else {
            mapType.value = GoogleMap.MAP_TYPE_NORMAL
        }
    }
}

enum class MapMode {
    DEFAULT, CRAG, SECTOR, TOPO, SUBMIT
}