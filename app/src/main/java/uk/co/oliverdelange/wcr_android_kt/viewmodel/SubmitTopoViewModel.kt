package uk.co.oliverdelange.wcr_android_kt.viewmodel

import android.net.Uri
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.*
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
import javax.inject.Inject


const val MAX_TOPO_SIZE_PX = 1020
//TODO Test me

//@Singleton Not a singleton so a new one gets created so half finished submissions don't retain
class SubmitTopoViewModel @Inject constructor(private val topoRepository: TopoRepository,
                                              private val routeRepository: RouteRepository) : ViewModel() {
    private val _viewEvents = SingleLiveEvent<Event>()
    val viewEvents: LiveData<Event> get() = _viewEvents

    private val _isDrawing = MutableLiveData(true)
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
    }
    val showTakePhotoIcon: LiveData<Boolean> get() = _showTakePhotoIcon

    private val _photoUri = MutableLiveData<Uri>()

    private val _shouldShowAddRouteButton = MutableLiveData<Boolean>().also { it.value = true }
    val shouldShowAddRouteButton: LiveData<Boolean> get() = _shouldShowAddRouteButton


    // Expose MutableLiveData so databinding can change the value
    val topoName = MutableLiveData<String?>()
    val topoNameError = Transformations.map(topoName) {
        if (it?.isEmpty() == true) "Can not be empty"
        else null
    }

    private val _activeRoute = MutableLiveData<Int>()
    val activeRoute: LiveData<Int>
        get() = _activeRoute

    //FIXME Shouldn't this be MLD?
    val routes = HashMap<Int, Route>()

//    val useVGradeForBouldering = getApplication<WcrApp>()
//            .prefs.getBoolean(PREF_USE_V_GRADE_FOR_BOULDERING, true)

//    val routeColourUpdate = MutableLiveData<Int>()


    var sectorId: String = ""

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
        Timber.d("Toggling drawing mode")
        _isDrawing.value = _isDrawing.value != true
    }

    fun setHasCamera(has: Boolean) {
        _hasCamera.value = has
    }
    // Is this needed just to avoid android..view classes in view model?
    enum class TouchEvent {
        TOUCH_DOWN, TOUCH_MOVE, TOUCH_UP, IGNORE

    }

    fun onDraw(x: Float, y: Float, event: TouchEvent): Boolean {

        return true //Event consumed
    }

    fun onUndoDrawing() {
//        doUndoDrawing.value = null
        //FIXME remove last action and redraw
    }

    // Taking a photo is a two step process.
    // 1. Create the file, and save the URI
    fun onSelectTakePhoto(uri: Uri) {
        _photoUri.value = uri
    }

    // 2. Take the photo
    fun onPhotoTaken() {
        _localTopoImage.value = _photoUri.value
    }

    fun onSelectExistingPhoto(uri: Uri) {
        _localTopoImage.value = uri
    }


    fun onRouteSelected(routeId: Int) {
        _activeRoute.value = routeId
    }

    fun onAddRoute(activeRouteFragId: Int) {
        if (!routes.containsKey(activeRouteFragId)) {
            routes[activeRouteFragId] = Route(name = "")
            Timber.d("Added empty route to view model with fragment id $activeRouteFragId")
        }
        _activeRoute.value = activeRouteFragId
        //FIXME don't link, just update on change
//        routes[activeRouteFragId]?.path = path?.actionStack
        Timber.d("Set route $activeRouteFragId path")
    }

    fun onRemoveRoute(fragmentId: Int?) {
        routes.remove(fragmentId)

        // Now set active route to right or left sibling
        val activeRouteVal = activeRoute.value
        activeRouteVal?.let { activeRouteFragmentId ->
            if (routes.size > 1) {
                val firstRouteToRight = routes.keys.firstOrNull { it > activeRouteFragmentId }
                val firstRouteToLeft = routes.keys.firstOrNull { it < activeRouteFragmentId }
                if (firstRouteToRight != null) _activeRoute.value = firstRouteToRight
                else _activeRoute.value = firstRouteToLeft
            } else if (routes.size == 1) {
                _activeRoute.value = routes.keys.first()
            }
        }
    }

    fun onRouteRemoved(routeCount: Int?) {
        if (routeCount == 0) _shouldShowAddRouteButton.value = true
    }

    fun onRoutePagerScroll(routeCount: Int?, position: Int, positionOffset: Float) {
        _shouldShowAddRouteButton.value = if (positionOffset > 0) {
            // Dragging between settled states
            val onLastRoute = routeCount == position + 2
            onLastRoute && positionOffset > 0.99f
        } else {
            // In a settled state
            routeCount == position + 1
        }
    }


    fun onRouteNameChanged(fragmentId: Int, text: CharSequence) {
        routes[fragmentId]?.let {
            Timber.d("Route fragment $fragmentId (${it.name}) name changed to $text")
            it.name = "$text"
        }
    }

    fun onRouteDescriptionChanged(fragmentId: Int, text: CharSequence) {
        routes[fragmentId]?.let {
            it.description = text.toString()
            Timber.d("Route fragment $fragmentId (${it.name}) description changed to ${it.description}")
        }
    }

    fun onRouteTypeChanged(fragmentId: Int, position: Int) {
        routes[fragmentId]?.let {
            val routeType = RouteType.values()[position]
            it.type = routeType
            Timber.d("Route fragment $fragmentId (${it.name}) type changed to ${it.type}")
            setGradeVisibility(fragmentId, routeType)
//            routeTypeUpdate.value = routeType
        }
    }

    private val halfFinishedTradGrades = mutableMapOf<Long, Pair<TradAdjectivalGrade?, TradTechnicalGrade?>>()
    fun onGradeChanged(fragmentId: Int, position: Int, gradeDropDown: GradeDropDown) {
//        routeColourUpdate.value = fragmentId
        routes[fragmentId]?.let { route ->
            when (gradeDropDown) {
                GradeDropDown.V -> {
                    route.grade = from(VGrade.values()[position])
                    Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                }
                GradeDropDown.FONT -> {
                    route.grade = from(FontGrade.values()[position])
                    Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                }
                GradeDropDown.SPORT -> {
                    route.grade = from(SportGrade.values()[position])
                    Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                }
                GradeDropDown.TRAD_ADJ -> {
                    val routeId = fragmentId.toLong()
                    val chosenTradAdjGrade = TradAdjectivalGrade.values()[position]
                    val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                    if (halfFinishedTradGrade?.second != null) {
                        route.grade = from(chosenTradAdjGrade, halfFinishedTradGrade.second!!)
                        Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                    }
                    halfFinishedTradGrades[routeId] = Pair(chosenTradAdjGrade, halfFinishedTradGrades[routeId]?.second)
                }
                GradeDropDown.TRAD_TECH -> {
                    val routeId = fragmentId.toLong()
                    val chosenTradTechGrade = TradTechnicalGrade.values()[position]
                    val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                    if (halfFinishedTradGrade?.first != null) {
                        route.grade = from(halfFinishedTradGrade.first!!, chosenTradTechGrade)
                        Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                    }
                    halfFinishedTradGrades[routeId] = Pair(halfFinishedTradGrades[routeId]?.first, chosenTradTechGrade)
                }
            }
        }
    }

    fun onClickSubmit() {
        Timber.d("Submit clicked for $sectorId")

        val hasName = !topoName.value.isNullOrEmpty()
        val hasImage = _localTopoImage.value != null
        val hasAtLeast1Route = routes.size > 0
        val routesHaveNameDescriptionAndPath = routes.none { route ->
            val emptyName = route.value.name?.isEmpty() ?: true
            val emptyDescription = route.value.description.isNullOrEmpty()
            val noRoutePath = (route.value.path?.size ?: 0) < 2 //TODO test me
            emptyName || emptyDescription || noRoutePath
        }
        val allowSubmit = hasName && hasImage && hasAtLeast1Route && routesHaveNameDescriptionAndPath
        Timber.d("Submission allowed: $allowSubmit (hasName:$hasName, hasImage:$hasImage, hasAtLeast1Route:$hasAtLeast1Route, routesHaveNameDescriptionAndPath:$routesHaveNameDescriptionAndPath)")

        if (allowSubmit) {
            submit(sectorId)
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

    private fun uploadImage(topoName: String, sectorId: String): Single<Uri> {
        return Single.create { emitter ->
            val rootRef = FirebaseStorage.getInstance().reference
            val imageRef = rootRef.child("topos/$sectorId/$topoName")

            //FIXME use file instead
//            val uploadTask = imageRef.putBytes(it)
//                    .addOnProgressListener { snapshot ->
//                        val percent = snapshot.bytesTransferred / snapshot.totalByteCount
//                        Timber.d("Image uploading... ${percent * 100}%")
//                    }
//
//            try {
//                Tasks.await(uploadTask)
//                val url = Tasks.await(imageRef.downloadUrl)
//                emitter.onSuccess(url)
//            } catch (e: IOException) {
//                Timber.e(e, "Error uploading topo image to cloud")
//                submitting.value = false
//                emitter.onError(e)
//            }
        }
    }

    private fun submit(sectorId: String): Single<String> {
        val topoName = topoName.value
        val topoImage = _localTopoImage.value
        return if (topoName != null && topoImage != null) {
            Timber.i("Submission started")
            submitting.value = true

            uploadImage(topoName, sectorId)
                    .flatMap { imageUrl ->
                        val topo = Topo(name = topoName, locationId = sectorId, image = imageUrl.toString())
                        topoRepository.save(topo)
                    }.flatMap { topoId ->
                        Timber.d("Topo saved")
                        val routesWithTopoId = routes.values.map { r -> r.also { it.topoId = topoId } }
                        val saveRoutes = routesWithTopoId.map { routeRepository.save(it) }
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

    private fun setGradeVisibility(fragmentId: Int, routeType: RouteType) {
        for (gradeType in GradeType.values()) {
            visibilityTracker[Pair(fragmentId, gradeType)]?.set(false)
        }
        when (routeType) {
            RouteType.TRAD -> visibilityTracker[Pair(fragmentId, GradeType.TRAD)]?.set(true)
            RouteType.SPORT -> visibilityTracker[Pair(fragmentId, GradeType.SPORT)]?.set(true)
            RouteType.BOULDERING -> {
                // FIXME
//                val prefs = getApplication<WcrApp>().prefs
                //TODO Settings toggle for PREF_USE_V_GRADE_FOR_BOULDERING
//                if (prefs.getBoolean(PREF_USE_V_GRADE_FOR_BOULDERING, true)) {
//                    visibilityTracker[Pair(fragmentId, GradeType.V)]?.set(true)
//                } else {
//                    visibilityTracker[Pair(fragmentId, GradeType.FONT)]?.set(true)
//                }
            }
        }
    }
}
