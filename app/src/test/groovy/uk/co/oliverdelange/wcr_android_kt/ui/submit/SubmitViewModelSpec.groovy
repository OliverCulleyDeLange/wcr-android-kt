package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import com.google.android.gms.maps.model.LatLng
import org.junit.Rule
import org.mockito.ArgumentCaptor
import spock.lang.Specification
import spock.lang.Unroll
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*

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

        where:
        enteredName | error              | buttonEnabled
        null        | "Can not be empty" | false
        ""          | "Can not be empty" | false
        " "         | "Can not be empty" | false
        "crag"      | null               | true
    }

    def "submit() should build and save crag to DB using Repository"() {
        given: "all crag details available"
        submitViewModel.cragName.value = name
        submitViewModel.cragLatLng.value = new LatLng(lat, lng)

        when: "user clicks submit"
        submitViewModel.submit(mockView)

        then: "save location to DB"
        ArgumentCaptor<Location> location = ArgumentCaptor.forClass(Location.class)
        verify(mockLocationRepository).save(location.capture())
        location.getValue().name == name
        location.getValue().lat == lat
        location.getValue().lng == lng

        where:
        name       | lat | lng
        "cragname" | 52  | -2
    }

    def "submit() should log error if not all location information available"() {
        when: "user clicks submit where information isn't available"
        submitViewModel.submit(mockView)

        then: "log error"
        verify(mockLocationRepository, never()).save(any())
    }
}
