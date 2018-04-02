package uk.co.oliverdelange.wcr_android_kt

import com.facebook.stetho.Stetho
import dagger.android.HasActivityInjector
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.di.AppInjector

class WcrAppDebug : WcrApp(), HasActivityInjector {
    override fun onCreate() {
        super.onCreate()
        AppInjector.init(this)
        Timber.plant(Timber.DebugTree())
        Stetho.initializeWithDefaults(this)
    }
}
