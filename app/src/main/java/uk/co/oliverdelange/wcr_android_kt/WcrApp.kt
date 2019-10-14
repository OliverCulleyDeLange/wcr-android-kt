package uk.co.oliverdelange.wcr_android_kt

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import uk.co.oliverdelange.wcr_android_kt.di.AppInjector
import javax.inject.Inject

const val PREFS_KEY = "wcr"
const val PREF_USE_V_GRADE_FOR_BOULDERING = "PREF_USE_V_GRADE_FOR_BOULDERING"
const val PREF_BOTTOM_SHEET_OPENED = "PREF_BOTTOM_SHEET_OPENED"

open class WcrApp : Application(), HasActivityInjector {


    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        AppInjector.init(this)
        AndroidThreeTen.init(this)
    }

    override fun activityInjector(): DispatchingAndroidInjector<Activity> {
        return dispatchingAndroidInjector
    }
}


