package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import de.jodamob.kotlin.testrunner.OpenedPackages
import de.jodamob.kotlin.testrunner.SpotlinTestRunner
import org.junit.Rule
import org.junit.runner.RunWith
import spock.lang.Specification
import spock.lang.Unroll
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository

@RunWith(SpotlinTestRunner)
@OpenedPackages("uk.co.oliverdelange")
class SubmitViewModelSpec extends Specification {

    @Rule
    InstantTaskExecutorRule rule = new InstantTaskExecutorRule()

    SubmitViewModel submitViewModel

    LocationRepository mockLocationRepository = Mock(LocationRepository)
    View dummyView = new View(null)

    def setup() {
        submitViewModel = new SubmitViewModel(mockLocationRepository)
    }

    @Unroll
    def "submit() should set error message to [#error] if name is [#name]"() {
        given: "user has not entered crag name"
        submitViewModel.cragName.value = name
        submitViewModel.cragNameError.value = null

        when: "user clicks submit"
        submitViewModel.submit(dummyView)

        then: "crag name error value is set"
        submitViewModel.cragNameError.value == error

        where:
        name   | error
        "Olly" | null
        null   | "Can not be empty"
        ""     | "Can not be empty"
        " "    | "Can not be empty"
    }
}
