package uk.co.oliverdelange.wcr_android_kt.view

import android.app.Activity
import android.view.animation.DecelerateInterpolator
import com.takusemba.spotlight.OnSpotlightStateChangedListener
import com.takusemba.spotlight.OnTargetStateChangedListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.target.SimpleTarget
import com.takusemba.spotlight.target.Target
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R

fun launchTutorial(activity: Activity) {
    val targets = ArrayList<Target>().apply {
        add(SimpleTarget.Builder(activity)
                .setPoint(500f, 500f)
                .setShape(Circle(200f)) // or RoundedRectangle()
                .setTitle("the title")
                .setDescription("the description")
                .setOverlayPoint(100f, 100f)
                .setOnSpotlightStartedListener(object : OnTargetStateChangedListener<SimpleTarget> {
                    override fun onStarted(target: SimpleTarget) {
                        Timber.d("Tutorial step 1 started")
                    }

                    override fun onEnded(target: SimpleTarget) {
                        Timber.d("Tutorial step 1 ended")
                    }
                })
                .build())
    }
    Spotlight.with(activity)
            .setOverlayColor(R.color.md_blue_900)
            .setDuration(300L)
            .setAnimation(DecelerateInterpolator(2f))
            .setTargets(targets)
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