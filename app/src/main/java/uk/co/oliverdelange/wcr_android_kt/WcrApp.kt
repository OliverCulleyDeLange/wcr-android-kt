package uk.co.oliverdelange.wcr_android_kt

import android.app.Activity
import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import uk.co.oliverdelange.wcr_android_kt.di.AppInjector
import javax.inject.Inject

open class WcrApp : Application(), HasActivityInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()
        MediaManager.init(this, mapOf("cloud_name" to "he5sr1yd9"))
        AppInjector.init(this)
    }

    override fun activityInjector(): DispatchingAndroidInjector<Activity> {
        return dispatchingAndroidInjector
    }
}
