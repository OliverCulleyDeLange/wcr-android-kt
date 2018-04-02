package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import com.google.android.gms.maps.GoogleMap
import de.jodamob.kotlin.testrunner.OpenedPackages
import de.jodamob.kotlin.testrunner.SpotlinTestRunner
import org.junit.Rule
import org.junit.runner.RunWith
import spock.lang.Specification
import spock.lang.Unroll

import static uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*

@RunWith(SpotlinTestRunner)
@OpenedPackages("uk.co.oliverdelange")
class MapViewModelSpec extends Specification {

    @Rule
    InstantTaskExecutorRule rule = new InstantTaskExecutorRule()

    MapViewModel mapViewModel

    View dummyView = new View(null)

    void setup() {
        mapViewModel = new MapViewModel()
    }

    @Unroll
    def "submit() should change map mode from #before to #after"() {
        given:
        mapViewModel.mapMode.value = before

        when:
        mapViewModel.submit(dummyView)

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
        mapViewModel.toggleMap(dummyView)

        then:
        mapViewModel.mapType.value == after

        where:
        before                       | after
        GoogleMap.MAP_TYPE_SATELLITE | GoogleMap.MAP_TYPE_NORMAL
        GoogleMap.MAP_TYPE_NORMAL    | GoogleMap.MAP_TYPE_SATELLITE
    }
}
