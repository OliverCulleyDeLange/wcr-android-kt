package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*

class MapViewModelTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var mapViewModel: MapViewModel
    private val mockView = mock<View> {}

    @Before
    fun setUp() {
        mapViewModel = MapViewModel()
    }

    @Test
    fun submitShouldChangeMapMode() {
        for (inOut in arrayListOf(
                Pair(DEFAULT, SUBMIT_CRAG),
                Pair(CRAG, SUBMIT_SECTOR),
                Pair(SECTOR, SUBMIT_TOPO))) {

            mapViewModel.mapMode.value = inOut.first

            mapViewModel.submit(mockView)

            assertEquals(mapViewModel.mapMode.value, inOut.second)
        }
    }

    @Test
    fun toggleMap() {
        for (inOut in arrayListOf(
                Pair(GoogleMap.MAP_TYPE_SATELLITE, GoogleMap.MAP_TYPE_NORMAL),
                Pair(GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE))) {

            mapViewModel.mapType.value = inOut.first

            mapViewModel.toggleMap(mockView)

            assertEquals(mapViewModel.mapType.value, inOut.second)
        }
    }
}