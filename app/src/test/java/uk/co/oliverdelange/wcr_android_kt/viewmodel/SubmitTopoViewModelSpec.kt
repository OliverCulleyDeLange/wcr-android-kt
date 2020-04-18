package uk.co.oliverdelange.wcr_android_kt.viewmodel

import android.net.Uri
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
            vm.showTakePhotoIcon.observeForever { }
        }

        "initial state" {
            vm.isDrawing.value shouldBe true
            vm.showTakePhotoIcon.value shouldBe false
            vm.shouldShowAddRouteButton.value shouldBe true
            vm.localTopoImage.value shouldBe null
            vm.topoName.value shouldBe null
            vm.topoNameError.value shouldBe null
            vm.activeRoute.value shouldBe null
            vm.routes.value shouldBe listOf(Route())
            vm.submitting.value shouldBe false
        }

        "viewEvents" - {
            "NavigateToImageSelectionGallery" {
                vm.onSelectExistingPhoto()
                vm.viewEvents.value is NavigateToImageSelectionGallery
            }
        }

        "isDrawing can be toggled" {
            vm.isDrawing.value shouldBe true
            vm.onToggleDrawing()
            vm.isDrawing.value shouldBe false
            vm.onToggleDrawing()
            vm.isDrawing.value shouldBe true
        }

        "showTakePhotoIcon" - {
            "with no topo image chosen," - {
                println("No topo image chosen")
                vm.localTopoImage.value shouldBe null
                "is true if camera is available" {
                    vm.showTakePhotoIcon.value shouldBe false

                    vm.setHasCamera(true)

                    vm.showTakePhotoIcon.value shouldBe true
                }
                "is false if camera is not available" {
                    vm.setHasCamera(true)
                    vm.showTakePhotoIcon.value shouldBe true

                    vm.setHasCamera(false)

                    vm.showTakePhotoIcon.value shouldBe false
                }
            }
            "with topo image chosen, is false" {
                val mockUri = mockk<Uri>()
                vm.onSelectedExistingPhoto(mockUri)
                vm.localTopoImage.value shouldNotBe null

                vm.showTakePhotoIcon.value shouldBe false
            }
        }

        "shouldShowAddRouteButton," - {
            "when there are no routes, is true" {
                vm.onRemoveRoute(0)
                vm.routes.value shouldBe emptyList()
                vm.shouldShowAddRouteButton.value shouldBe true
            }
            //TODO Figure out the scenarios for these tests, they're a bit confusing
            "when drag scrolling (offset > 0)" - {
                "and scrolled far right, is true" {
                    vm.onRoutePagerScroll(2, 0, 1f)
                    vm.shouldShowAddRouteButton.value shouldBe true
                }
                "and not quite scrolled all the way right, is false"{
                    vm.onRoutePagerScroll(2, 0, 0.99f)
                    vm.shouldShowAddRouteButton.value shouldBe false
                }
                "and not on last page, is false"{
                    vm.onRoutePagerScroll(3, 0, 1f)
                    vm.shouldShowAddRouteButton.value shouldBe false
                }
            }
            "when stationary" - {
                "and not on last page, is false "{
                    vm.onRoutePagerScroll(3, 0, 0f)
                    vm.shouldShowAddRouteButton.value shouldBe false
                }
                "and on last page, is true"{
                    vm.onRoutePagerScroll(2, 1, 0f)
                    vm.shouldShowAddRouteButton.value shouldBe true
                }
            }
        }

        "topoNameError" {
            vm.topoNameError.observeForever { }
            vm.topoNameError.value shouldBe null
            vm.topoName.value = ""
            vm.topoNameError.value shouldBe "Can not be empty"
        }

        "activeRoute," - {
            "when route added, is id of new route" {
                vm.activeRoute.value shouldBe null
                vm.onAddRoute(1)
                vm.activeRoute.value shouldBe 1
            }
            "when route selected, is id of selected route" {
                vm.activeRoute.value shouldBe null
                vm.onSelectRoute(1)
                vm.activeRoute.value shouldBe 1
            }
            "!FIXME? when only route is removed, is null" {
                vm.onAddRoute(1)
                vm.activeRoute.value shouldBe 1
                vm.onRemoveRoute(1)
                vm.activeRoute.value shouldBe null
            }
            "when route is removed and only one route is left, is id of only route left" {
                vm.onAddRoute(1)
                vm.onAddRoute(2)
                vm.activeRoute.value shouldBe 2
                vm.onRemoveRoute(2)
                vm.activeRoute.value shouldBe 1
            }
            "when route removed, is id of right sibling" {
                vm.onAddRoute(1)
                vm.onAddRoute(2)
                vm.onAddRoute(3)
                vm.onSelectRoute(1)
                vm.activeRoute.value shouldBe 1
                vm.onRemoveRoute(1)
                vm.activeRoute.value shouldBe 2
            }
            "when route removed, is left sibling, if no right sibling exists" {
                vm.onAddRoute(1)
                vm.onAddRoute(2)
                vm.onAddRoute(3)
                vm.activeRoute.value shouldBe 3
                vm.onRemoveRoute(3)
                vm.activeRoute.value shouldBe 2
            }
        }

        "routes," - {
            "" {
                vm._routes.value
            }
        }

        "!TODO SubmissionSucceeded" {
            vm.viewEvents.value is SubmissionSucceeded
        }
        "!TODO SubmissionFailed" {
            vm.viewEvents.value is SubmissionFailed
        }
    }
}