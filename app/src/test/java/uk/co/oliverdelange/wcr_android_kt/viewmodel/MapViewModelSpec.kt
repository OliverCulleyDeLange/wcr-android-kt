package uk.co.oliverdelange.wcr_android_kt.viewmodel

import android.app.Application
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import uk.co.oliverdelange.wcr_android_kt.InstantExecutorListener
import uk.co.oliverdelange.wcr_android_kt.auth.AuthService
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.repository.RouteRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.util.AbsentLiveData

class MapViewModelSpec : BehaviorSpec() {
    lateinit var vm: MapViewModel

    var mockApp = mockk<Application>()
    var mockLocationRepo = mockk<LocationRepository>()
    var mockTopoRepo = mockk<TopoRepository>()
    var mockRouteRepo = mockk<RouteRepository>()
    var mockAuthService = mockk<AuthService>()
    var mockDb = mockk<WcrDb>()

    init {
        listener(InstantExecutorListener())

        Given("User is not logged in") {
            println("Mocking not logged in response")
            every { mockAuthService.currentUser() } returns null
            And("No available locations") {
                println("Mocking empty crags response")
                every { mockLocationRepo.loadCrags() } returns AbsentLiveData.create()

                When("I create a MapViewModel") {
                    println("Initialising ViewModel")
                    vm = MapViewModel(mockApp, mockLocationRepo, mockTopoRepo, mockRouteRepo, mockAuthService, mockDb)
                    vm.mapLabel.observeForever { /*So the label is updated*/ }

                    Then("Test initial state") {
                        vm.viewEvents.value shouldBe null
                        vm.userSignedIn.value shouldBe false
                        vm.mapType.value shouldBe MAP_TYPE_NORMAL
                        vm.mapLabel.value shouldBe "SAT" // Label should display opposite of map type
                        vm.mapMode.value shouldBe MapMode.DEFAULT_MODE
                        vm.showFab.value shouldBe false // as not signed in
                        vm.selectedLocationRouteInfo.value shouldBe null
                        vm.selectedLocation.value shouldBe null
                        vm.submitButtonLabel.value shouldBe null
                        vm.crags.value shouldBe null
                        vm.cragClusterItems.value shouldBe null
                        vm.sectors.value shouldBe null
                        vm.topos.value shouldBe null
                        vm.mapLatLngBounds.value shouldBe emptyList()
                        vm.bottomSheetState.value shouldBe null
                        vm.bottomSheetRequestedState.value shouldBe null
                        vm.bottomSheetTitle.value shouldBe null
                        vm.searchQuery.value shouldBe null
                        vm.searchResults.value shouldBe null
                    }
                }
            }
        }
    }
}