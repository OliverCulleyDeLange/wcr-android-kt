package uk.co.oliverdelange.wcr_android_kt.view.map

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.GoogleMap
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel

import static org.mockito.Mockito.mock

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
        mapViewModel.onSubmit(mockView)

        then:
        mapViewModel.mapMode.value == after

        where:
        before       | after
        DEFAULT_MODE | SUBMIT_CRAG_MODE
        CRAG_MODE    | SUBMIT_SECTOR_MODE
        SECTOR_MODE  | SUBMIT_TOPO_MODE
    }

    @Unroll
    def "toggleMap should change map type from #before to #after"() {
        given:
        mapViewModel.mapType.value = before

        when:
        mapViewModel.onToggleMap(mockView)

        then:
        mapViewModel.mapType.value == after

        where:
        before                       | after
        GoogleMap.MAP_TYPE_SATELLITE | GoogleMap.MAP_TYPE_NORMAL
        GoogleMap.MAP_TYPE_NORMAL    | GoogleMap.MAP_TYPE_SATELLITE
    }
}
