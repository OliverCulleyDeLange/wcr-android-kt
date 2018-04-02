package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.view.View
import com.google.android.gms.maps.model.LatLng
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitViewModel @Inject constructor(private val locationRepository: LocationRepository) : ViewModel() {

    val crag = Location(type = LocationType.CRAG)
    val cragName = MutableLiveData<String>()
    val cragNameError = MutableLiveData<String>()
    val cragLatLng = MutableLiveData<LatLng>()

    val submitButtonEnabled = MediatorLiveData<Boolean>().also {
        it.value = false
        it.addSource(cragName) { cragName: String? ->
            if (cragName == null || cragName.isBlank()) {
                it.value = false; cragNameError.value = "Can not be empty"
            } else {
                it.value = true; cragNameError.value = null
                crag.name = cragName
            }
        }
        it.addSource(cragLatLng) { latlng: LatLng? ->
            if (latlng != null) {
                crag.lat = latlng.latitude
                crag.lng = latlng.longitude
            }
        }
    }

    fun submit(view: View) {
        locationRepository.save(crag)
    }
}
