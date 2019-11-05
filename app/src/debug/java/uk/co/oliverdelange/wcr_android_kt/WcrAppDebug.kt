package uk.co.oliverdelange.wcr_android_kt

import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.soloader.SoLoader
import dagger.android.HasActivityInjector
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.di.AppInjector


class WcrAppDebug : WcrApp(), HasActivityInjector {
    override fun onCreate() {
        super.onCreate()
        AppInjector.init(this)
        Timber.plant(CustomDebugTree())
        SoLoader.init(this, false)

        val client = AndroidFlipperClient.getInstance(this)
        client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
        client.addPlugin(DatabasesFlipperPlugin(this))
        client.start()
    }
}

class CustomDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return ":::WCR:::" + super.createStackElementTag(element)
    }
}