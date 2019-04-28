package uk.co.oliverdelange.wcr_android_kt

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapsActivity

@RunWith(AndroidJUnit4::class)
class SearchDrawerTest {

    lateinit var searchString: String

    @Rule @JvmField
    val mActivityRule = ActivityTestRule(MapsActivity::class.java)

    @Before
    fun initValidString() {
        // Specify a valid string.
        searchString = "searchme"
    }

    @Test
    fun checkDrawerMenuItems() {
        onView(withId(R.id.search_bar_left_action_container))
                .perform(click())

        onView(withId(R.id.material_drawer_name))
                .check(matches(withText("About")))
    }

    @Test
    fun checkSearchBarHintText() {
        onView(withId(R.id.search_bar_text))
                .check(matches(withHint("Search for crags and climbs")))
    }
}
