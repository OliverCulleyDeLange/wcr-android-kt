package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.app.Application
import android.net.Uri
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.preprocess.BitmapEncoder
import com.cloudinary.android.preprocess.ImagePreprocessChain
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.USE_V_GRADE_FOR_BOULDERING
import uk.co.oliverdelange.wcr_android_kt.WcrApp
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.service.WorkerService
import uk.co.oliverdelange.wcr_android_kt.ui.view.PaintableTopoImageView
import javax.inject.Inject

const val MAX_TOPO_SIZE_PX = 640
//@Singleton
class SubmitTopoViewModel @Inject constructor(application: Application,
                                              private val topoRepository: TopoRepository,
                                              private val workerService: WorkerService) : AndroidViewModel(application) {

    val localTopoImage = MutableLiveData<Uri?>()
    val topoName = MutableLiveData<String?>()
    val topoNameError = Transformations.map(topoName) {
        tryEnableSubmit()
        if (it?.isEmpty() == true) "Can not be empty"
        else null
    }

    val shouldShowAddRouteButton = MutableLiveData<Boolean>().also { it.value = true }
    fun setShouldShowAddRouteButton(routeCount: Int?) {
        if (routeCount == 0) shouldShowAddRouteButton.value = true
    }

    fun setShouldShowAddRouteButton(routeCount: Int?, position: Int, positionOffset: Float) {
        setShouldShowAddRouteButton(routeCount)
        if (positionOffset > 0) {
            val onLastRoute = routeCount == position + 2
            shouldShowAddRouteButton.value = onLastRoute && positionOffset > 0.99
        } else {
            shouldShowAddRouteButton.value = routeCount == position + 1
        }
    }

    val activeRoute = MutableLiveData<Int>()
    val routes = HashMap<Int, Route>()
    fun addRoute(activeRouteFragId: Int, paths: MutableMap<Int, PaintableTopoImageView.PathCapture>) {
        if (!routes.containsKey(activeRouteFragId)) {
            routes[activeRouteFragId] = Route(name = "")
            Timber.d("Added empty route to view model with fragment id $activeRouteFragId")
        }
        activeRoute.value = activeRouteFragId
        // Link the route path capture to the Route in the view model
        val pathCapture = paths[activeRouteFragId]?.capture
        routes[activeRouteFragId]?.path = pathCapture
        Timber.d("Set route $activeRouteFragId path to $pathCapture")
        // We probably need to disable the submit button if a new route has been added without filled in info.
        tryEnableSubmit()
    }

    fun removeRoute(fragmentId: Int?) {
        routes.remove(fragmentId)

        // Now set active route to right or left sibling
        val activeRouteVal = activeRoute.value
        activeRouteVal?.let { activeRouteFragmentId ->
            if (routes.size > 1) {
                val firstRouteToRight = routes.keys.firstOrNull { it > activeRouteFragmentId }
                val firstRouteToLeft = routes.keys.firstOrNull { it < activeRouteFragmentId }
                if (firstRouteToRight != null) activeRoute.value = firstRouteToRight
                else activeRoute.value = firstRouteToLeft
            } else if (routes.size == 1) {
                activeRoute.value = routes.keys.first()
            }
        }
    }

    fun routeNameChanged(fragmentId: Int, text: CharSequence) {
        routes[fragmentId]?.let {
            Timber.d("Route fragment $fragmentId (${it.name}) name changed to $text")
            it.name = "$text"
            //Fow now, ID is name
//            it.id = "$text"
        }
        tryEnableSubmit()
    }

    fun routeDescriptionChanged(fragmentId: Int, text: CharSequence) {
        routes[fragmentId]?.let {
            it.description = text.toString()
            Timber.d("Route fragment $fragmentId (${it.name}) description changed to ${it.description}")
        }
        tryEnableSubmit()
    }

    val routeTypeUpdate = MutableLiveData<RouteType>()
    fun routeTypeChanged(fragmentId: Int, position: Int) {
        routes[fragmentId]?.let {
            val routeType = RouteType.values()[position]
            it.type = routeType
            Timber.d("Route fragment $fragmentId (${it.name}) type changed to ${it.type}")
            setGradeVisibility(fragmentId, routeType)
            routeTypeUpdate.value = routeType
        }
    }

    private fun setGradeVisibility(fragmentId: Int, routeType: RouteType) {
        for (gradeType in GradeType.values()) {
            visibilityTracker[Pair(fragmentId, gradeType)]?.set(View.GONE)
        }
        when (routeType) {
            RouteType.TRAD -> visibilityTracker[Pair(fragmentId, GradeType.TRAD)]?.set(View.VISIBLE)
            RouteType.SPORT -> visibilityTracker[Pair(fragmentId, GradeType.SPORT)]?.set(View.VISIBLE)
            RouteType.BOULDERING -> {
                val prefs = getApplication<WcrApp>().prefs
                //TODO Settings toggle for USE_V_GRADE_FOR_BOULDERING
                if (prefs.getBoolean(USE_V_GRADE_FOR_BOULDERING, true)) {
                    visibilityTracker[Pair(fragmentId, GradeType.V)]?.set(View.VISIBLE)
                } else {
                    visibilityTracker[Pair(fragmentId, GradeType.FONT)]?.set(View.VISIBLE)
                }
            }
        }
    }

    val halfFinishedTradGrades = mutableMapOf<Long, Pair<TradAdjectivalGrade?, TradTechnicalGrade?>>()
    val useVGradeForBouldering = getApplication<WcrApp>().prefs.getBoolean(USE_V_GRADE_FOR_BOULDERING, true)
    val routeColourUpdate = MutableLiveData<Int>()
    fun gradeChanged(fragmentId: Int, position: Int, gradeDropDown: GradeDropDown) {
        routeColourUpdate.value = fragmentId
        routes[fragmentId]?.let { route ->
            when (gradeDropDown) {
                GradeDropDown.V -> {
                    route.grade = Grade.from(VGrade.values()[position])
                    Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                }
                GradeDropDown.FONT -> {
                    route.grade = Grade.from(FontGrade.values()[position])
                    Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                }
                GradeDropDown.SPORT -> {
                    route.grade = Grade.from(SportGrade.values()[position])
                    Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                }
                GradeDropDown.TRAD_ADJ -> {
                    val routeId = fragmentId.toLong()
                    val chosenTradAdjGrade = TradAdjectivalGrade.values()[position]
                    val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                    if (halfFinishedTradGrade?.second != null) {
                        route.grade = Grade.from(chosenTradAdjGrade, halfFinishedTradGrade.second!!)
                        Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                    }
                    halfFinishedTradGrades[routeId] = Pair(chosenTradAdjGrade, halfFinishedTradGrades[routeId]?.second)
                }
                GradeDropDown.TRAD_TECH -> {
                    val routeId = fragmentId.toLong()
                    val chosenTradTechGrade = TradTechnicalGrade.values()[position]
                    val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                    if (halfFinishedTradGrade?.first != null) {
                        route.grade = Grade.from(halfFinishedTradGrade.first!!, chosenTradTechGrade)
                        Timber.d("Route fragment $fragmentId (${route.name}) grade changed to ${route.grade}")
                    }
                    halfFinishedTradGrades[routeId] = Pair(halfFinishedTradGrades[routeId]?.first, chosenTradTechGrade)
                }
            }
        }
    }

    val visibilityTracker = mutableMapOf<Pair<Int, GradeType>, ObservableInt>()
    fun shouldShow(fragmentId: Int, gradeType: GradeType): ObservableInt {
        return visibilityTracker[Pair(fragmentId, gradeType)]
                ?: ObservableInt(View.GONE).apply {
                    visibilityTracker[Pair(fragmentId, gradeType)] = this
                }
    }

    val submitButtonEnabled = ObservableBoolean(false)

    fun tryEnableSubmit() {
        submitButtonEnabled.set(!topoName.value.isNullOrEmpty() &&
                localTopoImage.value != null &&
                routes.size > 0 &&
                routes.none {
                    it.value.name.isEmpty() ||
                            it.value.description.isNullOrEmpty() ||
                            it.value.path?.size?.let { it < 2 } ?: false
                })
    }

    val submitting = MutableLiveData<Boolean>().also {
        it.value = false
    }

    fun submit(sectorId: String): MutableLiveData<Pair<Long, List<Long>>> {
        val topoName = topoName.value
        val topoImage = localTopoImage.value
        return if (topoName != null && topoImage != null) {
            Timber.i("Submission started")
            submitButtonEnabled.set(false)
            submitting.value = true
            val mediator = MediatorLiveData<Pair<Long, List<Long>>>()
            MediaManager.get().upload(topoImage)
                    .unsigned("wcr_topo_upload")
                    .option("folder", "topo/$sectorId")
                    .option("public_id", topoName)
                    .preprocess(ImagePreprocessChain.limitDimensionsChain(MAX_TOPO_SIZE_PX, MAX_TOPO_SIZE_PX)
                            .saveWith(BitmapEncoder(BitmapEncoder.Format.WEBP, 80)))
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = bytes.toDouble() / totalBytes
                            Timber.d("Image upload progress: %s", progress.toString())
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            Timber.d("Image upload success: %s", resultData)
                            val imageUrl = resultData["secure_url"] as String
                            // For now the id is the sectorID and name concatenated with a hyphen
                            val topo = Topo(name = topoName, locationId = sectorId, image = imageUrl, id = "$sectorId-$topoName")
                            val saved = topoRepository.save(topo, routes.values)
                            workerService.updateRouteInfo(sectorId)
                            mediator.addSource(saved) {
                                mediator.value = it
                            }
                            submitting.value = false
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            submitButtonEnabled.set(true)
                            submitting.value = false
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            // your code here
                        }
                    })
                    .dispatch(getApplication())
            mediator
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            MutableLiveData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("SubmitTopoViewModel is being destroyed...")
    }
}