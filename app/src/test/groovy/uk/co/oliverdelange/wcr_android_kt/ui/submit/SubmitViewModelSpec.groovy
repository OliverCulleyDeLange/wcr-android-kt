package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import com.google.android.gms.maps.model.LatLng
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify

class SubmitViewModelSpec extends Specification {

    @Rule
    InstantTaskExecutorRule rule = new InstantTaskExecutorRule()

    SubmitViewModel submitViewModel

    def mockLocationRepository = mock(LocationRepository)
    def mockView = mock(View)

    def setup() {
        submitViewModel = new SubmitViewModel(mockLocationRepository)
    }

    def "submitButtonEnabled should default to false"() {
        expect:
        submitViewModel.submitButtonEnabled.value == false
    }

    @Unroll
    def "submit button enabled [#buttonEnabled] if crag name [#enteredName]"() {
        given: "No error exists already and submitButtonEnabled is being observed"
        submitViewModel.submitButtonEnabled.observeForever({})
        submitViewModel.cragNameError.value = null
        submitViewModel.cragName.value = null

        when: "user has not entered a crag enteredName"
        submitViewModel.cragName.value = enteredName

        then: "submit button is not enabled & crag enteredName error value is set"
        submitViewModel.submitButtonEnabled.value == buttonEnabled
        submitViewModel.cragNameError.value == error

        and: "crag enteredName should be #cragname"
        submitViewModel.crag.name == cragname

        where:
        enteredName | error              | buttonEnabled | cragname
        null        | "Can not be empty" | false         | ""
        ""          | "Can not be empty" | false         | ""
        " "         | "Can not be empty" | false         | ""
        "crag"      | null               | true          | "crag"
    }

    @Unroll
    def "crag location lat and lng set by submitButtonEnabled"() {
        given: "No latlng exists and submitButtonEnabled is being observed"
        submitViewModel.submitButtonEnabled.observeForever({})
        submitViewModel.crag.lat = 0.0D
        submitViewModel.crag.lng = 0.0D

        when: "crag latlng is updated"
        submitViewModel.cragLatLng.value = latlng

        then: "crag location latlng are updated"
        submitViewModel.crag.lat == lat
        submitViewModel.crag.lng == lng

        where:
        latlng                 | lat   | lng
        new LatLng(52.0, -2.0) | 52.0D | -2.0D
        null                   | 0.0D  | 0.0D
    }

    def "submit() should save crag to DB using Repository"() {
        given: "a crag to submit"
        def submission = submitViewModel.crag

        when: "user clicks submit"
        submitViewModel.submit(mockView)

        then: "crag enteredName error value is set"
        verify(mockLocationRepository).save(submission)
    }
}
