package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class MapViewModel : ViewModel() {

    val mapType: MutableLiveData<Int> = MutableLiveData()
}