package uk.co.oliverdelange.wcr_android_kt.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

// I'm not convinced by this method, but i'm giving it a try
// https://proandroiddev.com/navigation-events-in-mvvm-on-android-via-livedata-5c88ef48ee83
open class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val mPending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        // Observe the internal MutableLiveData
        super.observe(owner, Observer {
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(it)
            }
        })
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }
}

interface Event
class ScrollToTopo(val topoId: String) : Event
object ShowDevMenu : Event
object NavigateToSignIn : Event
object NavigateToImageSelectionGallery : Event
object NavigateToCamera : Event
class ShowXClicksToDevMenuToast(val clicks: Int): Event
class SubmissionFailed(val error: String) : Event
class SubmissionSucceeded(val topoId: String): Event
