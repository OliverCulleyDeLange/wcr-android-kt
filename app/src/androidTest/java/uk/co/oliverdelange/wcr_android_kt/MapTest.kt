package uk.co.oliverdelange.wcr_android_kt

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapTest {

    @Rule
    @JvmField
    val mActivityRule = ActivityTestRule(MapsActivity::class.java)

    @Test
    fun checkMapToggleLabel() {
        onView(withId(R.id.map_toggle))
                .check(matches(withText("SAT")))

        onView(withId(R.id.map_toggle))
                .perform(click())

        onView(withId(R.id.map_toggle))
                .check(matches(withText("MAP")))
    }
}
