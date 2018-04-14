package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

    fun submit(parentId: Long?): LiveData<Long> {
        val locationName = locationName.value
        val lat = locationLatLng.value?.latitude
        val lng = locationLatLng.value?.longitude
        if (locationName != null && lat != null && lng != null) {
            val location = Location(name = locationName, lat = lat, lng = lng, type = locationType, parentId = parentId)
            return locationRepository.save(location)
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            return MutableLiveData()
        }
    }
}
