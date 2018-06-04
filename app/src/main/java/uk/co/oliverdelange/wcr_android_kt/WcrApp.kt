package uk.co.oliverdelange.wcr_android_kt

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.cloudinary.android.MediaManager
import com.parse.Parse
import com.parse.ParseObject
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import uk.co.oliverdelange.wcr_android_kt.di.AppInjector
import uk.co.oliverdelange.wcr_android_kt.model.ParseLocation
import uk.co.oliverdelange.wcr_android_kt.model.ParseRoute
import uk.co.oliverdelange.wcr_android_kt.model.ParseTopo
import javax.inject.Inject


const val USE_V_GRADE_FOR_BOULDERING = "USE_V_GRADE_FOR_BOULDERING"

open class WcrApp : Application(), HasActivityInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("wcr", Context.MODE_PRIVATE)
        MediaManager.init(this, mapOf("cloud_name" to "he5sr1yd9"))
        AppInjector.init(this)
        ParseObject.registerSubclass(ParseLocation::class.java)
        ParseObject.registerSubclass(ParseTopo::class.java)
        ParseObject.registerSubclass(ParseRoute::class.java)
        Parse.initialize(Parse.Configuration.Builder(this)
                .applicationId("myAppId")
                .server("http://10.0.2.2:1337/parse/")
                .build())
    }

    override fun activityInjector(): DispatchingAndroidInjector<Activity> {
        return dispatchingAndroidInjector
    }
}


