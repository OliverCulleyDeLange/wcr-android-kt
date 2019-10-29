package uk.co.oliverdelange.wcr_android_kt.view

import android.view.View
import android.view.animation.DecelerateInterpolator
import com.takusemba.spotlight.OnSpotlightStateChangedListener
import com.takusemba.spotlight.OnTargetStateChangedListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.RoundedRectangle
import com.takusemba.spotlight.target.CustomTarget
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.view.map.MapsActivity
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel


/*
    1. MapCrag      : Crags
    2. MapSector    : Select Crag -> Sectors
    3. Topos        : Select Sector, Expand bottom sheet -> Topos
    4. Routes       : Select route
    5. FAB          : Hide bottom sheet, display fab -> Submit Crag / Sector / Topo (logged in)
    6. Search       : Search for an item
    7. Sign in / Register

 */
fun launchTutorial(activity: MapsActivity, vm: MapViewModel?): Spotlight? {
    val searchView = activity.findViewById<View>(R.id.search_query_section)
    val mapView = activity.findViewById<View>(R.id.map)

    val targets = listOf(
            CustomTarget.Builder(activity)
                    .setPoint(mapView)
                    .setShape(RoundedRectangle(mapView.height / 3.toFloat(), mapView.width.toFloat() * 0.9f, 25f))
                    .setOverlay(R.layout.layout_tutorial_map_crag)
                    .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
                        override fun onStarted(target: CustomTarget) {
                            Timber.d("Crag tutorial started")
                            vm?.onTutorialStart()
                        }

                        override fun onEnded(target: CustomTarget) {
                            Timber.d("Crag tutorial ended")
                            vm?.onCragTutorialFinish()
                        }
                    })
                    .build(),
            CustomTarget.Builder(activity)
                    .setPoint(mapView)
                    .setShape(RoundedRectangle(mapView.height / 3.toFloat(), mapView.width.toFloat() * 0.9f, 25f))
                    .setOverlay(R.layout.layout_tutorial_map_sector)
                    .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
                        override fun onStarted(target: CustomTarget) {
                            Timber.d("Sector tutorial started")
                        }

                        override fun onEnded(target: CustomTarget) {
                            Timber.d("Sector tutorial ended")
                        }
                    })
                    .build(),
            CustomTarget.Builder(activity)
                    .setPoint(searchView)
                    .setShape(RoundedRectangle(searchView.height.toFloat(), searchView.width.toFloat(), 10f))
                    .setOverlay(R.layout.layout_tutorial_search)
                    .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
                        override fun onStarted(target: CustomTarget) {
                            Timber.d("Search tutorial started")
                            vm?.onTutorialStart()
                        }

                        override fun onEnded(target: CustomTarget) {
                            Timber.d("Search tutorial ended")
                        }
                    })
                    .build()
    )

    return Spotlight.with(activity)
            .setOverlayColor(R.color.bg_tutorial)
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
            }).also {
                it.start()
            }
}