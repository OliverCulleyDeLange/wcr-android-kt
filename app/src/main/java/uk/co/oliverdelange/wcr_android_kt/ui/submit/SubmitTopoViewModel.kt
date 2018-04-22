package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.RouteType
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitTopoViewModel @Inject constructor(private val topoRepository: TopoRepository) : ViewModel() {

    val topoName = MutableLiveData<String>()
    val topoNameError = MutableLiveData<String>()

    val routes = MutableLiveData<MutableMap<Int, Route>>().also { it.value = mutableMapOf() }

    fun routeNameChanged(fragmentId: Int, text: CharSequence) {
        routes.value?.get(fragmentId)?.let {
            it.name = text.toString()
        }
    }

    fun routeDescriptionChanged(fragmentId: Int, text: CharSequence) {
        routes.value?.get(fragmentId)?.let {
            it.description = text.toString()
        }
    }

    fun routeTypeChanged(fragmentId: Int, position: Int) {
        routes.value?.get(fragmentId)?.let {
            it.type = RouteType.values()[position]
        }
    }

    val submitButtonEnabled = MediatorLiveData<Boolean>().also {
        it.value = false
        it.addSource(topoName) { locationName: String? ->
            if (locationName == null || locationName.isBlank()) {
                it.value = false; topoNameError.value = "Can not be empty"
            } else {
                it.value = true; topoNameError.value = null
            }
        }
    }

    fun submit(sectorId: Long): MutableLiveData<Pair<Long, Array<Long>>> {
        val locationName = topoName.value
        if (locationName != null) {
            val topo = Topo(name = locationName, locationId = sectorId)
            return topoRepository.save(topo, routes.value?.values ?: emptyList())
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            return MutableLiveData()
        }
    }
}
