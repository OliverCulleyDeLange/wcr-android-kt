package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.view.View
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitViewModel @Inject constructor(private val locationRepository: LocationRepository) : ViewModel() {

    val cragName: MutableLiveData<String> = MutableLiveData()
    val cragNameError: MutableLiveData<String> = MutableLiveData()

    fun submit(view: View) {
        val cragnameValue = cragName.value
        if (cragnameValue == null || cragnameValue.isEmpty()) {
            cragNameError.value = "Can not be empty"
        } else {
            cragNameError.value = null
            submitCrag(cragnameValue)
        }
    }

    private fun submitCrag(cragName: String) {
        locationRepository.save(Location(cragName, 52.0, -2.0, LocationType.CRAG))
    }
}
