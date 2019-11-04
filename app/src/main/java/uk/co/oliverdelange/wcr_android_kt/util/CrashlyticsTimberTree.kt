package uk.co.oliverdelange.wcr_android_kt.util

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE) return
        Crashlytics.log("$priority  $message")
    }
}
