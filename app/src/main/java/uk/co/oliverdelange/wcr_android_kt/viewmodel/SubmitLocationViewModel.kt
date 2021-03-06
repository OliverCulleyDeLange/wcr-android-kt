package uk.co.oliverdelange.wcr_android_kt.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.Single
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.usecases.SubmitLocationUseCase
import javax.inject.Inject

class SubmitLocationViewModel @Inject constructor(private val submitLocationUseCase: SubmitLocationUseCase) : ViewModel() {

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

    fun submit(parentId: String?): Single<String> {
        val locationName = locationName.value
        val lat = locationLatLng.value?.latitude
        val lng = locationLatLng.value?.longitude
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        return if (locationName != null && lat != null && lng != null && userId != null) {
            val location = Location(name = locationName, latlng = LatLng(lat, lng), type = locationType, parentLocationId = parentId, uploaderId = userId)
            submitLocationUseCase.submitLocation(location)
        } else {
            val err = RuntimeException("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            Timber.e(err)
            Single.error(err)
        }
    }
}
