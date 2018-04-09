package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import com.google.android.gms.maps.GoogleMap
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository

import static org.mockito.Mockito.mock
import static uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*

class MapViewModelSpec extends Specification {

    @Rule
    InstantTaskExecutorRule rule = new InstantTaskExecutorRule()

    MapViewModel mapViewModel

    def mockLocationRepository = mock(LocationRepository)
    def mockView = mock(View)

    void setup() {
        mapViewModel = new MapViewModel(mockLocationRepository)
    }

    @Unroll
    def "submit() should change map mode from #before to #after"() {
        given:
        mapViewModel.mapMode.value = before

        when:
        mapViewModel.submit(mockView)

        then:
        mapViewModel.mapMode.value == after

        where:
        before  | after
        DEFAULT | SUBMIT_CRAG
        CRAG    | SUBMIT_SECTOR
        SECTOR  | SUBMIT_TOPO
    }

    @Unroll
    def "toggleMap should change map type from #before to #after"() {
        given:
        mapViewModel.mapType.value = before

        when:
        mapViewModel.toggleMap(mockView)

        then:
        mapViewModel.mapType.value == after

        where:
        before                       | after
        GoogleMap.MAP_TYPE_SATELLITE | GoogleMap.MAP_TYPE_NORMAL
        GoogleMap.MAP_TYPE_NORMAL    | GoogleMap.MAP_TYPE_SATELLITE
    }
}