package uk.co.oliverdelange.wcr_android_kt

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.oliverdelange.wcr_android_kt.view.map.MapsActivity

@RunWith(AndroidJUnit4::class)
class SubmitTest {

    @Rule
    @JvmField
    val mActivityRule = ActivityTestRule(MapsActivity::class.java)

    @Test
    fun checkCragNameValidation() {
        onView(withId(R.id.location_name_input_layout))
                .check(doesNotExist())

        onView(withId(R.id.fab))
                .perform(click())

        onView(withId(R.id.location_name_input_layout))
                .check(matches(isDisplayed()))

        onView(withId(R.id.location_name_input))
                .check(matches(isDisplayed()))

        onView(allOf(withParent(withParent(withId(R.id.location_name_input_layout))), withId(R.id.textinput_error)))
                .check(matches(not(isDisplayed())))

        onView(withId(R.id.location_name_input))
                .perform(typeText("    "))
        Espresso.closeSoftKeyboard()


        onView(allOf(withParent(withParent(withId(R.id.location_name_input_layout))), withId(R.id.textinput_error)))
                .check(matches(allOf(
                        isDisplayed(),
                        withText("Can not be empty")
                )))

        onView(withId(R.id.location_name_input))
                .perform(typeText("cragname"))
        Espresso.closeSoftKeyboard()

        onView(withId(R.id.submit)).perform(click())

        onView(allOf(withParent(withParent(withId(R.id.location_name_input_layout))), withId(R.id.textinput_error)))
                .check(matches(not(isDisplayed())))
    }
}
