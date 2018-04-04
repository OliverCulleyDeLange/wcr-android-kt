package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.view.View
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitViewModel @Inject constructor(private val locationRepository: LocationRepository) : ViewModel() {

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
            }
        }
    }

    fun submit(view: View) {
        val cragName = cragName.value
        val lat = cragLatLng.value?.latitude
        val lng = cragLatLng.value?.longitude
        if (cragName != null && lat != null && lng != null) {
            val crag = Location(cragName, lat, lng, LocationType.CRAG)
            locationRepository.save(crag)
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
        }
    }
}
