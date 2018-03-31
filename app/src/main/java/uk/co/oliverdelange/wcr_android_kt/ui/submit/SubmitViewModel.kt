package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.view.View

class SubmitViewModel : ViewModel() {

    val cragName: MutableLiveData<String> = MutableLiveData()
    val cragNameError: MutableLiveData<String> = MutableLiveData()

    fun submit(view: View) {
        if (cragName.value.isNullOrBlank()) cragNameError.value = "Can not be empty"
        else {
            cragNameError.value = null
        }
    }
}
