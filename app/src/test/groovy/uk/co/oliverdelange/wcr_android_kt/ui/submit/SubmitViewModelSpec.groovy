package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

class SubmitViewModelSpec extends Specification {

    @Rule
    InstantTaskExecutorRule rule = new InstantTaskExecutorRule()

    SubmitViewModel submitViewModel
    View view = Mock(View)

    def setup() {
        submitViewModel = new SubmitViewModel()
    }

    @Unroll
    def "submit() should set error message to [#error] if name is [#name]"() {
        given: "user has not entered crag name"
        submitViewModel.cragName.value = name
        submitViewModel.cragNameError.value = null

        when: "user clicks submit"
        submitViewModel.submit(view)

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
