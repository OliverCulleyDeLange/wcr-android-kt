package uk.co.oliverdelange.wcr_android_kt.ui.submit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import javax.inject.Inject

//@Singleton
class SubmitLocationViewModel @Inject constructor(private val locationRepository: LocationRepository) : ViewModel() {

    lateinit var locationType: LocationType
    val locationName = MutableLiveData<String>()
    val locationNameError = MutableLiveData<String>()
    val locationLatLng = MutableLiveData<LatLng>()

    val submitButtonEnabled = MediatorLiveData<Boolean>().also {
        it.value = false
        it.addSource(locationName) { locationName: String? ->
            if (locationName == null || locationName.isBlank()) {
                it.value = false; locationNameError.value = "Can not be empty"
            } else {
                it.value = true; locationNameError.value = null
            }
        }
    }

    fun submit(parentId: String?): LiveData<String> {
        val locationName = locationName.value
        val lat = locationLatLng.value?.latitude
        val lng = locationLatLng.value?.longitude
        if (locationName != null && lat != null && lng != null) {
            val location = Location(name = locationName, latlng = LatLng(lat, lng), type = locationType, parentLocation = parentId)
            return locationRepository.save(location)
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            return MutableLiveData()
        }
    }
}
