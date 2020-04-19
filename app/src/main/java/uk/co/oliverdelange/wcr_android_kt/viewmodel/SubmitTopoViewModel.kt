package uk.co.oliverdelange.wcr_android_kt.viewmodel

import android.net.Uri
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.factory.from
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.repository.RouteRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.service.uploadSync
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random


const val MAX_TOPO_SIZE_PX = 1020

class RouteViewModel(val route: Route = Route(),
                     var isActive: Boolean = true,
                     var pagerPosition: Int = 0,
                     var pagerId: Int = Random.nextInt(),
        // This keeps track of any grades the user might have selected for
        // route types that aren't currently selected
                     val selectedGrades: MutableMap<RouteType, Grade> = mutableMapOf())

//@Singleton Not a singleton so a new one gets created so half finished submissions don't retain
class SubmitTopoViewModel @Inject constructor(private val topoRepository: TopoRepository,
                                              private val routeRepository: RouteRepository) : ViewModel() {
    /* This needs to be set by the View otherwise submission will fail*/
    var sectorId: String? = null

    private val _viewEvents = SingleLiveEvent<Event>()
    val viewEvents: LiveData<Event> get() = _viewEvents

    private val _isDrawing = MutableLiveData(true) //Tested
    val isDrawing: LiveData<Boolean> get() = _isDrawing

    private val _localTopoImage = MutableLiveData<Uri?>()
    val localTopoImage: LiveData<Uri?> get() = _localTopoImage

    private val _hasCamera = MutableLiveData(false)

    private val _showTakePhotoIcon = MediatorLiveData<Boolean>().also {
        it.value = false
        fun shouldShow(): Boolean {
            val show = _hasCamera.value == true &&
                    _localTopoImage.value == null
            Timber.d("'showTakePhotoIcon': $show")
            return show
        }
        it.addSource(_hasCamera) { _ -> it.value = shouldShow() }
        it.addSource(_localTopoImage) { _ -> it.value = shouldShow() }
    } //Tested
    val showTakePhotoIcon: LiveData<Boolean> get() = _showTakePhotoIcon

    private val _photoUri = MutableLiveData<Uri>()

    private val _shouldShowAddRouteButton = MutableLiveData<Boolean>().also { it.value = true }
    val shouldShowAddRouteButton: LiveData<Boolean> get() = _shouldShowAddRouteButton

    // Expose MutableLiveData so databinding can change the value (Two way data binding)
    val topoName = MutableLiveData<String?>()
    val topoNameError = Transformations.distinctUntilChanged(Transformations.map(topoName) {
        if (it?.isEmpty() == true) "Can not be empty"
        else null
    }) //Tested

    private val _routes = MutableLiveData<List<RouteViewModel>>().apply { value = listOf(RouteViewModel()) }
    val routes: LiveData<List<RouteViewModel>>
        get() = _routes

//    val useVGradeForBouldering = getApplication<WcrApp>()
//            .prefs.getBoolean(PREF_USE_V_GRADE_FOR_BOULDERING, true)

//    val routeColourUpdate = MutableLiveData<Int>()

    // We display a loader and disable the submit button when a submission is in progress
    val submitting = MutableLiveData<Boolean>().also {
        it.value = false
    }

    private val visibilityTracker = mutableMapOf<Pair<Int, GradeType>, ObservableBoolean>()
    fun shouldShowGradePicker(fragmentId: Int, gradeType: GradeType): ObservableBoolean {
        return visibilityTracker[Pair(fragmentId, gradeType)]
                ?: ObservableBoolean(false).apply {
                    visibilityTracker[Pair(fragmentId, gradeType)] = this
                }
    }

    /*
        User actions
     */

    fun onToggleDrawing() {
        _isDrawing.value = _isDrawing.value != true
        Timber.d("Toggling drawing mode: Drawing = ${_isDrawing.value}")
    } //Tested

    fun setHasCamera(has: Boolean) {
        Timber.d("Camera available: $has")
        _hasCamera.value = has
    }//Tested

    // Is this needed just to avoid android..view classes in view model?
    enum class TouchEvent {
        TOUCH_DOWN, TOUCH_MOVE, TOUCH_UP, IGNORE
    }

    fun onDraw(x: Float, y: Float, event: TouchEvent) {
        val pair = Pair(x, y)
        Timber.v("User is drawing: $event x:$x y:$y ")
        updateActiveRoute { vm ->
            vm.route.path = (vm.route.path?.toMutableList() ?: mutableListOf()).also { path ->
                Timber.v("Current path: $path")
                when (event) {
                    TouchEvent.TOUCH_DOWN -> {
                        path.add(PathSegment(listOf(pair)))
                        if (path.size == 1){
                        // Hack to get white dot to show for first tap
                            path[0].addPoint(Pair(x+0.0001f, y))
                        }
                    }
                    TouchEvent.TOUCH_MOVE -> {
                        //TODO only add point if suitable distance away from the last one
                        path.last().addPoint(pair)
                    }
                }
            }
        }
    }

    @kotlin.ExperimentalStdlibApi
    fun onUndoDrawing() {
        Timber.d("User wants to undo drawing")
        updateActiveRoute {
            it.route.path = it.route.path?.toMutableList()?.apply {
                removeLastOrNull()
            }
        }
    }

    fun onSelectTakePhoto() {
        Timber.d("User wants to take topo image with camera")
        if (localTopoImage.value == null) _viewEvents.postValue(NavigateToCamera)
    }

    fun onPhotoTaken(uri: Uri) {
        Timber.d("User has taken a photo: $uri")
        _localTopoImage.value = uri
    }

    fun onSelectExistingPhoto() {
        Timber.d("User wants to select topo image from gallery")
        if (localTopoImage.value == null) _viewEvents.postValue(NavigateToImageSelectionGallery)
    }

    fun onSelectedExistingPhoto(uri: Uri) {
        Timber.d("User selected topo image from gallery: $uri")
        _localTopoImage.value = uri
    }


    fun onSelectRoute(position: Int) {
        Timber.d("User selected route at position $position")
        updateRoutes {
            it.makeActiveNotActive()
            it.makeActive(position)
        }
    }

    fun onAddRoute() {
        Timber.d("User wants to add a new route")
        updateRoutes {
            it.makeActiveNotActive()
            it.addNewRoute()
        }
    }

    fun onRemoveRoute(id: Int) {
        Timber.d("User wants to remove route $id")
        updateRoutes {
            it.removeById(id)
            it.setActiveNearest(id)
        }
    } // Tested

    fun onRoutePagerScroll(position: Int, positionOffset: Float) {
        val routeCount = _routes.value?.size
        val show = if (positionOffset > 0) {
            // Dragging between settled states
            val onLastRoute = routeCount == position + 2
            onLastRoute && positionOffset > 0.99f
        } else {
            // In a settled state
            routeCount == position + 1
        }
        Timber.v("showAddRouteButton: $show. position=$position, offset=$positionOffset")
        _shouldShowAddRouteButton.value = show
    }


    fun onRouteNameChanged(id: Int, text: CharSequence) {
        Timber.d("Route $id name changed to: $text")
        _routes.value?.firstOrNull { it.pagerId == id }?.let {
            // TODO Check ok? Just modifying the value, not posting a new value
            // We don't really want to trigger anything, just capture the change
            it.route.name = "$text"
        }
    }

    fun onRouteDescriptionChanged(id: Int, text: CharSequence) {
        Timber.d("Route $id description changed to $text")
        _routes.value?.firstOrNull { it.pagerId == id }?.let {
            it.route.description = text.toString()
        }
    }

    fun onRouteTypeChanged(id: Int, enumIndex: Int) {
        val routeType = RouteType.values()[enumIndex]
        Timber.d("Route $id type changed to $routeType")
        updateRoutes { routes ->
            routes.firstOrNull { it.pagerId == id }?.let { vm ->
                vm.route.type = routeType
                setGradeVisibility(id, routeType)
                vm.selectedGrades[routeType]?.let {
                    vm.route.grade = it
                    Timber.d("Set route grade to $it as it was previously chosen")
                }
            }
        }
    }

    private fun setGradeVisibility(id: Int, routeType: RouteType) {
        for (gradeType in GradeType.values()) {
            visibilityTracker[Pair(id, gradeType)]?.set(false)
        }
        when (routeType) {
            RouteType.TRAD -> visibilityTracker[Pair(id, GradeType.TRAD)]?.set(true)
            RouteType.SPORT -> visibilityTracker[Pair(id, GradeType.SPORT)]?.set(true)
            RouteType.BOULDERING -> {
                // FIXME
//                val prefs = getApplication<WcrApp>().prefs
                //TODO Settings toggle for PREF_USE_V_GRADE_FOR_BOULDERING
//                if (prefs.getBoolean(PREF_USE_V_GRADE_FOR_BOULDERING, true)) {
                visibilityTracker[Pair(id, GradeType.V)]?.set(true)
//                } else {
//                    visibilityTracker[Pair(fragmentId, GradeType.FONT)]?.set(true)
//                }
            }
        }
    }

    private val halfFinishedTradGrades = mutableMapOf<Long, Pair<TradAdjectivalGrade?, TradTechnicalGrade?>>()
    fun onGradeChanged(id: Int, enumIndex: Int, gradeDropDown: GradeDropDown) {
        Timber.d("Route $id $gradeDropDown grade changed to value at index $enumIndex ↴")
        updateRoutes { routes ->
            routes.firstOrNull { it.pagerId == id }?.let { vm ->
                when (gradeDropDown) {
                    GradeDropDown.V -> {
                        val grade = from(VGrade.values()[enumIndex])
                        vm.route.grade = grade
                        vm.selectedGrades[RouteType.BOULDERING] = grade
                    }
                    GradeDropDown.FONT -> {
                        val grade = from(FontGrade.values()[enumIndex])
                        vm.route.grade = grade
                        vm.selectedGrades[RouteType.BOULDERING] = grade
                    }
                    GradeDropDown.SPORT -> {
                        val grade = from(SportGrade.values()[enumIndex])
                        vm.route.grade = grade
                        vm.selectedGrades[RouteType.SPORT] = grade
                    }
                    GradeDropDown.TRAD_ADJ -> {
                        val routeId = id.toLong()
                        val chosenTradAdjGrade = TradAdjectivalGrade.values()[enumIndex]
                        val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                        if (halfFinishedTradGrade?.second != null) {
                            val grade = from(chosenTradAdjGrade, halfFinishedTradGrade.second!!)
                            vm.route.grade = grade
                            vm.selectedGrades[RouteType.TRAD] = grade
                        }
                        halfFinishedTradGrades[routeId] = Pair(chosenTradAdjGrade, halfFinishedTradGrades[routeId]?.second)
                    }
                    GradeDropDown.TRAD_TECH -> {
                        val routeId = id.toLong()
                        val chosenTradTechGrade = TradTechnicalGrade.values()[enumIndex]
                        val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                        if (halfFinishedTradGrade?.first != null) {
                            val grade = from(halfFinishedTradGrade.first!!, chosenTradTechGrade)
                            vm.route.grade = grade
                            vm.selectedGrades[RouteType.TRAD] = grade
                        }
                        halfFinishedTradGrades[routeId] = Pair(halfFinishedTradGrades[routeId]?.first, chosenTradTechGrade)
                    }
                }
                Timber.d("Route $id $gradeDropDown grade changed to ${vm.route.grade}")
            }
        }
    }

    fun onClickSubmit() {
        Timber.d("Submit clicked for $sectorId")

        val hasName = !topoName.value.isNullOrEmpty()
        val hasImage = _localTopoImage.value != null
        val hasAtLeast1Route = _routes.value?.isNotEmpty() ?: false
        val routesHaveNameDescriptionAndPath = _routes.value?.all { vm ->
            val emptyName = vm.route.name?.isEmpty() ?: true
            val emptyDescription = vm.route.description.isNullOrEmpty()
            !emptyName && !emptyDescription && vm.route.hasPath()
        } ?: false

        val allowSubmission = sectorId != null && hasName && hasImage && hasAtLeast1Route && routesHaveNameDescriptionAndPath
        Timber.d("Submission allowed: $allowSubmission (sectorId:$sectorId, hasName:$hasName, hasImage:$hasImage, hasAtLeast1Route:$hasAtLeast1Route, routesHaveNameDescriptionAndPath:$routesHaveNameDescriptionAndPath)")
        if (allowSubmission) {
            //FIXME Add submission to disposables and dispose on destroy
            submit(sectorId!!)
                    .subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({ submittedTopoId ->
                        Timber.i("Submission Succeeded")
                        _viewEvents.postValue(SubmissionSucceeded(submittedTopoId))
                    }, { e ->
                        Timber.e(e, "Submission Failed")
                        _viewEvents.postValue(SubmissionFailed("Failed to submit topo!"))
                    })
        } else {
            val error = if (!hasName) {
                "Please enter a name"
            } else if (!hasImage) {
                "Please select an image"
            } else if (!hasAtLeast1Route) {
                "A topo should have at least one route"
            } else if (!routesHaveNameDescriptionAndPath) {
                "Every route should have a name, description and topo path"
            } else {
                "Unknown error, please contact us for details"
            }
            _viewEvents.postValue(SubmissionFailed(error))
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("SubmitTopoViewModel is being destroyed...")
    }

/*
    Private
*/
    private fun submit(sectorId: String): Single<String> {
        val topoName = topoName.value
        val topoImage = _localTopoImage.value
        return if (topoName != null && topoImage != null) {
            Timber.i("Submission started")
            submitting.value = true

            uploadImage(topoName, sectorId, topoImage)
                    .flatMap { imageUrl ->
                        val topo = Topo(name = topoName, locationId = sectorId, image = imageUrl.toString())
                        topoRepository.save(topo)
                    }.flatMap { topoId ->
                        Timber.d("Topo saved")
                        val routesWithTopoId = _routes.value?.map {
                            it.route.topoId = topoId
                            it
                        } ?: emptyList()
                        val saveRoutes = routesWithTopoId.map { routeRepository.save(it.route) }
                        Completable.mergeDelayError(saveRoutes).toSingleDefault(topoId)
                    }.doOnSuccess { topoId ->
                        Timber.d("All routes saved for topo $topoId")
                        uploadSync()
                        submitting.postValue(false)
                    }.doOnError {
                        submitting.postValue(false)
                    }
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            Single.just("")
        }
    }

    private fun uploadImage(topoName: String, sectorId: String, topoImage: Uri): Single<Uri> {
        return Single.create { emitter ->
            val rootRef = FirebaseStorage.getInstance().reference
            val imageRef = rootRef.child("topos/$sectorId/$topoName")

            val uploadTask = imageRef.putFile(topoImage)
                    .addOnProgressListener { snapshot ->
                        val percent = snapshot.bytesTransferred / snapshot.totalByteCount
                        Timber.d("Image uploading... ${percent * 100}%")
                    }
            try {
                Tasks.await(uploadTask)
                val url = Tasks.await(imageRef.downloadUrl)
                emitter.onSuccess(url)
            } catch (e: IOException) {
                Timber.e(e, "Error uploading topo image to cloud")
                submitting.value = false
                emitter.onError(e)
            }
        }
    }

    // Convenience methods to update route state without forgetting to post the updated value
    private fun updateRoutes(update: (vm: MutableList<RouteViewModel>) -> Unit) {
        _routes.value?.toMutableList()?.also { routes ->
            update(routes)
            _routes.postValue(routes)
        }
    }

    private fun updateActiveRoute(update: (vm: RouteViewModel) -> Unit) {
        updateRoutes { routes ->
            routes.firstOrNull { it.isActive }?.also {
                update(it)
            }
        }
    }
}

//TODO Check ok. Playing around with extension functions, seems nice but not sure if its "ok"
private fun MutableList<RouteViewModel>.setActiveNearest(id: Int) {
    singleOrNull { it.pagerId == id }?.also { route ->
        if (size > 1) {
            firstOrNull { it.pagerPosition > route.pagerPosition }?.apply { isActive = true }
                    ?: firstOrNull { it.pagerPosition <= route.pagerPosition }?.apply { isActive = true }

        } else if (size == 1) {
            first().isActive = true
        }
    }
}

private fun MutableList<RouteViewModel>.removeById(id: Int) {
    singleOrNull { it.pagerId == id }?.also { route ->
        remove(route)
        this.filter { it.pagerPosition > route.pagerPosition }.forEach { it.pagerPosition-- }
    }
}

private fun MutableList<RouteViewModel>.addNewRoute() {
    add(RouteViewModel(pagerPosition = size))
}

private fun MutableList<RouteViewModel>.makeActive(position: Int) {
    singleOrNull { vm -> vm.pagerPosition == position }?.isActive = true
}

private fun MutableList<RouteViewModel>.makeActiveNotActive() {
    filter { vm -> vm.isActive }.forEach { it.isActive = false }
}
