package uk.co.oliverdelange.wcr_android_kt

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapsActivity

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
