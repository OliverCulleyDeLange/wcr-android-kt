package uk.co.oliverdelange.wcr_android_kt.view

import android.view.View
import android.view.animation.DecelerateInterpolator
import com.takusemba.spotlight.OnSpotlightStateChangedListener
import com.takusemba.spotlight.OnTargetStateChangedListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Padding
import com.takusemba.spotlight.shape.RoundedRectangle
import com.takusemba.spotlight.target.CustomTarget
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.view.map.MapsActivity
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel


/*
    0. Search       : Search for an item
    1. MapCrag      : Crags
    2. MapSector    : Select Crag -> Sectors
    3. DragBarPeek  : Coloured section, Climb types, Title
    3. Topos        : Select Sector, Expand bottom sheet -> Topos
    4. Routes       : Select route
    5. FAB          : Hide bottom sheet, display fab -> Submit Crag / Sector / Topo (logged in)
    6. Sign in / Register
 */

class TutorialManager {

    var currentTutorial: Spotlight? = null

    fun next() {
        currentTutorial?.closeCurrentTarget()
    }

    fun exit() {
        currentTutorial?.closeSpotlight()
    }

    fun launch(activity: MapsActivity, vm: MapViewModel?) {

        val targets = listOf(
                CustomTarget.Builder(activity)
                        .setRectSupplierFromView(R.id.search_query_section)
                        .setShape(RoundedRectangle(Padding(8, 8), 25f))
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
                        .build(),
                CustomTarget.Builder(activity)
                        .setRectSupplierFromView(R.id.map)
                        .setShape(RoundedRectangle(Padding(-50, -400), 25f))
                        .setOverlay(R.layout.layout_tutorial_map_crag)
                        .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Crag tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Crag tutorial ended")
                                vm?.onCragTutorialFinish()
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setRectSupplierFromView(activity.findViewById<View>(R.id.map))
                        .setShape(RoundedRectangle(Padding(-50, -400), 25f))
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
                        .setRectSupplierFromView(R.id.bottom_sheet_peek)
                        .setShape(RoundedRectangle(Padding(0, 0), 10f))
                        .setOverlay(R.layout.layout_tutorial_locationinfo)
                        .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Bottom Sheet Peek tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Bottom Sheet Peek tutorial ended")
                            }
                        })
                        .build()
//                CustomTarget.Builder(activity)
//                        .setRectSupplierFromView(R.id.topo_card_view)
//                        .setShape(RoundedRectangle(Padding(10,10), 10f))
//                        .setOverlay(R.layout.layout_tutorial_topo)
//                        .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
//                            override fun onStarted(target: CustomTarget) {
//                                Timber.d("Topo tutorial started")
//                            }
//
//                            override fun onEnded(target: CustomTarget) {
//                                Timber.d("Topo tutorial ended")
//                            }
//                        })
//                        .build()
        )

        currentTutorial = Spotlight.with(activity)
                .setOverlayColor(R.color.bg_tutorial)
                .setDuration(100L)
                .setAnimation(DecelerateInterpolator(2f))
                .setTargets(ArrayList(targets))
                .setClosedOnTouchedOutside(true)
                .setOnSpotlightStateListener(object : OnSpotlightStateChangedListener {
                    override fun onStarted() {
                        Timber.d("Search Tutorial started")
                    }

                    override fun onEnded() {
                        Timber.d("Search Tutorial ended")
                    }
                }).apply { start() }
    }
}