package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap

class MapViewModel : ViewModel() {

    val mapType: MutableLiveData<Int> = MutableLiveData()
    val mapMode: MutableLiveData<MapMode> = MutableLiveData()

    fun init() {
        mapType.value = GoogleMap.MAP_TYPE_NORMAL
        mapMode.value = MapMode.DEFAULT
    }
}

enum class MapMode {
    DEFAULT, CRAG, SECTOR, TOPO, SUBMIT
}