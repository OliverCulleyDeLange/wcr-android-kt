package uk.co.oliverdelange.wcr_android_kt.view

import android.view.View
import android.view.animation.DecelerateInterpolator
import com.takusemba.spotlight.OnSpotlightStateChangedListener
import com.takusemba.spotlight.OnTargetStateChangedListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.RoundedRectangle
import com.takusemba.spotlight.target.CustomTarget
import io.reactivex.Completable
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
        val disposable = searchTutorial(activity, vm)
                .andThen(mapTutorial(activity, vm))
                .andThen(bottomSheetTutorial(activity, vm))
                .subscribe {
                    Timber.d("All tutorials complete")
                }
    }

    private fun searchTutorial(activity: MapsActivity, vm: MapViewModel?): Completable {
        return Completable.create {
            val searchView = activity.findViewById<View>(R.id.search_query_section)

            val targets = listOf(
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
                            it.onComplete()
                        }
                    }).apply { start() }
        }
    }

    private fun mapTutorial(activity: MapsActivity, vm: MapViewModel?): Completable {
        return Completable.create {
            val mapView = activity.findViewById<View>(R.id.map)

            val targets = listOf(
                    CustomTarget.Builder(activity)
                            .setPoint(mapView)
                            .setShape(RoundedRectangle(mapView.height / 3.toFloat(), mapView.width.toFloat() * 0.9f, 25f))
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
                            .setPoint(mapView)
                            .setShape(RoundedRectangle(mapView.height / 3.toFloat(), mapView.width.toFloat() * 0.9f, 25f))
                            .setOverlay(R.layout.layout_tutorial_map_sector)
                            .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
                                override fun onStarted(target: CustomTarget) {
                                    Timber.d("Sector tutorial started")
                                }

                                override fun onEnded(target: CustomTarget) {
                                    Timber.d("Sector tutorial ended")
                                    vm?.onSectorTutorialFinish()
                                }
                            })
                            .build()
            )

            currentTutorial = Spotlight.with(activity)
                    .setOverlayColor(R.color.bg_tutorial)
                    .setDuration(100L)
                    .setAnimation(DecelerateInterpolator(2f))
                    .setTargets(ArrayList(targets))
                    .setClosedOnTouchedOutside(true)
                    .setOnSpotlightStateListener(object : OnSpotlightStateChangedListener {
                        override fun onStarted() {
                            Timber.d("Map Tutorial started")
                        }

                        override fun onEnded() {
                            Timber.d("Map Tutorial ended")
                            it.onComplete()
                        }
                    }).apply { start() }
        }
    }

    private fun bottomSheetTutorial(activity: MapsActivity, vm: MapViewModel?): Completable {
        return Completable.create {
            val topoView = activity.findViewById<View>(R.id.topo_card_view)

            val targets = listOf(
                    CustomTarget.Builder(activity)
                            .setPoint(topoView)
                            .setShape(RoundedRectangle(topoView.height.toFloat(), topoView.width.toFloat(), 10f))
                            .setOverlay(R.layout.layout_tutorial_topo)
                            .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<CustomTarget> {
                                override fun onStarted(target: CustomTarget) {
                                    Timber.d("Topo tutorial started")
                                }

                                override fun onEnded(target: CustomTarget) {
                                    Timber.d("Topo tutorial ended")
                                }
                            })
                            .build()

            )

            currentTutorial = Spotlight.with(activity)
                    .setOverlayColor(R.color.bg_tutorial)
                    .setDuration(100L)
                    .setAnimation(DecelerateInterpolator(2f))
                    .setTargets(ArrayList(targets))
                    .setClosedOnTouchedOutside(true)
                    .setOnSpotlightStateListener(object : OnSpotlightStateChangedListener {
                        override fun onStarted() {
                            Timber.d("Tutorial started (topo)")
                        }

                        override fun onEnded() {
                            Timber.d("Tutorial ended (topo)")
                            it.onComplete()
                        }
                    }).apply { start() }
        }
    }
}
