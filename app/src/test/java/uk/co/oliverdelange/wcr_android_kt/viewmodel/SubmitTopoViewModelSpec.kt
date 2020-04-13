package uk.co.oliverdelange.wcr_android_kt.viewmodel

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import uk.co.oliverdelange.wcr_android_kt.InstantExecutorListener
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.repository.RouteRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository

class SubmitTopoViewModelSpec : FreeSpec() {
    lateinit var vm: SubmitTopoViewModel

    lateinit var mockTopoRepo: TopoRepository
    lateinit var mockRouteRepo: RouteRepository

    init {
        listener(InstantExecutorListener())

        beforeTest {
            mockTopoRepo = mockk()
            mockRouteRepo = mockk()
            vm = SubmitTopoViewModel(mockTopoRepo, mockRouteRepo)
        }

        "initial state" {
            vm.isDrawing.value shouldBe true
            vm.showTakePhotoIcon.value shouldBe false
            vm.shouldShowAddRouteButton.value shouldBe true
            vm.localTopoImage.value shouldBe null
            vm.topoName.value shouldBe null
            vm.topoNameError.value shouldBe null
            vm.activeRoute.value shouldBe null
            vm.routes shouldBe emptyMap<Int, Route>()
            vm.submitting.value shouldBe false
        }

        "onToggleDrawing" {
            vm.isDrawing.value shouldBe true
            vm.onToggleDrawing()
            vm.isDrawing.value shouldBe false
            vm.onToggleDrawing()
            vm.isDrawing.value shouldBe true
        }
    }
}