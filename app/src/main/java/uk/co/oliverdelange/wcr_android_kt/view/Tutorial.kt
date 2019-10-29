package uk.co.oliverdelange.wcr_android_kt.view

import android.view.View
import android.view.animation.DecelerateInterpolator
import com.takusemba.spotlight.OnSpotlightStateChangedListener
import com.takusemba.spotlight.OnTargetStateChangedListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.target.SimpleTarget
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.view.map.MapsActivity
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel

/*
    1. Map: Crags
    2. Map: Select Crag -> Sectors
    3. Topos: Select Sector, Expand bottom sheet -> Topos
    4. Routes: Select route
    5. FAB: Hide bottom sheet, display fab -> Submit Crag / Sector / Topo (logged in)
    6. Search: Search for an item
    7. Sign in / Register

 */
fun launchTutorial(activity: MapsActivity, vm: MapViewModel?) {
    val targets = listOf(
            SimpleTarget.Builder(activity)
                    .setPoint(activity.findViewById<View>(R.id.map))
                    .setShape(Circle(500f)) // or RoundedRectangle()
                    .setTitle("the title")
                    .setDescription("the description")
                    .setOverlayPoint(100f, 100f)
                    .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<SimpleTarget> {
                        override fun onStarted(target: SimpleTarget) {
                            vm?.onTutorialStart()
                            Timber.d("Tutorial step 1 started")
                        }

                        override fun onEnded(target: SimpleTarget) {
                            Timber.d("Tutorial step 1 ended")
                        }
                    })
                    .build()
    )

    Spotlight.with(activity)
            .setOverlayColor(R.color.md_blue_900)
            .setDuration(100L)
            .setAnimation(DecelerateInterpolator(2f))
            .setTargets(ArrayList(targets))
            .setClosedOnTouchedOutside(true)
            .setOnSpotlightStateListener(object : OnSpotlightStateChangedListener {
                override fun onStarted() {
                    Timber.d("Tutorial started")
                }

                override fun onEnded() {
                    Timber.d("Tutorial ended")
                }
            })
            .start()
}