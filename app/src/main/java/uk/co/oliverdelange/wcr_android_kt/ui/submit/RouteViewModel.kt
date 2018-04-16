package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import uk.co.oliverdelange.wcr_android_kt.model.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteViewModel @Inject constructor() : ViewModel() {

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
}
