package uk.co.oliverdelange.wcr_android_kt.view

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.takusemba.spotlight.OnSpotlightStateChangedListener
import com.takusemba.spotlight.OnTargetStateChangedListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.Padding
import com.takusemba.spotlight.shape.RoundedRectangle
import com.takusemba.spotlight.target.CustomTarget
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.view.map.MapsActivity
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel


/*
    Y 0. Search       : Search for an item
    Y 1. MapCrag      : Crags
    Y 2. MapSector    : Select Crag -> Sectors
    Y 3. DragBarPeek  : Coloured section, Climb types, Title
    Y 3. Topos/Routes : Expand bottom sheet -> Topos & routes
      5. Sign in / Register
      6. FAB          : Hide bottom sheet, display fab -> Submit Crag / Sector / Topo (logged in)
      7. Submission   :
 */

class TutorialManager {

    var spotlight: Spotlight? = null

    fun next() {
        spotlight?.closeCurrentTarget()
    }

    fun exit() {
        spotlight?.closeSpotlight()
    }

    fun launch(activity: MapsActivity, vm: MapViewModel?) {

        val targets = listOf(
                CustomTarget.Builder(activity)
                        .setDuration(300L)
                        .setRectSupplierFromView(R.id.search_query_section)
                        .setShape(RoundedRectangle(Padding(8, 8), 25f))
                        .setOverlay(R.layout.layout_tutorial_search)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
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
                        .setDuration(300L)
                        .setRectSupplierFromView(R.id.map)
                        .setShape(RoundedRectangle(Padding(-50, -400), 25f))
                        .setOverlay(R.layout.layout_tutorial_map_crag)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
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
                        .setDuration(300L)
                        .setRectSupplierFromView(activity.findViewById<View>(R.id.map))
                        .setShape(RoundedRectangle(Padding(-50, -400), 25f))
                        .setOverlay(R.layout.layout_tutorial_map_sector)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Sector tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Sector tutorial ended")
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setDuration(300L)
                        .setRectSupplierFromView(R.id.bottom_sheet_peek)
                        .setShape(RoundedRectangle(Padding(10, 10), 10f))
                        .setOverlay(R.layout.layout_tutorial_locationinfo)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Location info tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Location info ended")
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setDuration(300L)
                        .setRectSupplierFromView(R.id.climb_grades_group)
                        .setShape(RoundedRectangle(Padding(10, 10), 10f))
                        .setOverlay(R.layout.layout_tutorial_locationinfo_grades)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Grades tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Grades tutorial ended")
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setDuration(300L)
                        .setRectSupplierFromView(R.id.climb_types_group)
                        .setShape(RoundedRectangle(Padding(10, 10), 10f))
                        .setOverlay(R.layout.layout_tutorial_locationinfo_climbtypes)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Climb types tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Climb types tutorial ended")
                                vm?.onLocationInfoTutorialComplete()
                                vm?.bottomSheetState?.observe(activity, object : Observer<Int> {
                                    override fun onChanged(t: Int?) {
                                        if (vm.bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED) {
                                            spotlight?.startNextTarget()
                                            vm.bottomSheetState.removeObserver(this)
                                        }
                                    }
                                })
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setRectSupplierFromView(R.id.topo_card_view)
                        .setShape(RoundedRectangle(Padding(10, 10), 10f))
                        .setOverlay(R.layout.layout_tutorial_topo)
                        .setAutoStart(false)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Topo tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Topo tutorial ended")
                                vm?.onTopoTutorialComplete()
//                                activity.floating_search_view.openMenu(true)
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setRectSupplierFromView(R.id.search_bar_left_action_container)
                        .setShape(RoundedRectangle(Padding(0, 0), 10f))
                        .setOverlay(R.layout.layout_tutorial_signin)
//                        .setAutoStart(false)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Signin tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Signin tutorial ended")
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setRectSupplierFromView(R.id.fab)
                        .setShape(Circle(100))
                        .setOverlay(R.layout.layout_tutorial_fab)
//                        .setAutoStart(false)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Fab tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Fab tutorial ended")
                            }
                        })
                        .build(),
                CustomTarget.Builder(activity)
                        .setRect(0, 0, 0, 0)
                        .setShape(RoundedRectangle(Padding(0, 0), 00f))
                        .setOverlay(R.layout.layout_tutorial_submit)
//                        .setAutoStart(false)
                        .setTargetListener(object : OnTargetStateChangedListener<CustomTarget> {
                            override fun onStarted(target: CustomTarget) {
                                Timber.d("Submit tutorial started")
                            }

                            override fun onEnded(target: CustomTarget) {
                                Timber.d("Submit tutorial ended")
                            }
                        })
                        .build()
        )

        spotlight = Spotlight.with(activity)
                .setOverlayColor(R.color.bg_tutorial)
                .setDuration(500L)
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