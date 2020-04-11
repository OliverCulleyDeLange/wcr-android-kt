package uk.co.oliverdelange.wcr_android_kt.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.ExifInterface.ORIENTATION_UNDEFINED
import android.media.ExifInterface.TAG_ORIENTATION
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.PREF_USE_V_GRADE_FOR_BOULDERING
import uk.co.oliverdelange.wcr_android_kt.WcrApp
import uk.co.oliverdelange.wcr_android_kt.factory.from
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.repository.RouteRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.service.uploadSync
import uk.co.oliverdelange.wcr_android_kt.util.PathCapture
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject


const val MAX_TOPO_SIZE_PX = 1020
//TODO Test me

//@Singleton Not a singleton so a new one gets created so half finished submissions don't retain
class SubmitTopoViewModel @Inject constructor(application: Application,
                                              private val topoRepository: TopoRepository,
                                              private val routeRepository: RouteRepository) : AndroidViewModel(application) {

    val hasCamera: Boolean = application.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    val isDrawing = ObservableBoolean(true)

    val doUndoDrawing = MutableLiveData<Void>()

    val photoUri = MutableLiveData<Uri>()

    val localTopoImage = MutableLiveData<Uri?>()
    private val localTopoImageBytes = Transformations.map(localTopoImage) {
        it?.let { imageUri ->
            MediaStore.Images.Media.getBitmap(getApplication<WcrApp>().contentResolver, imageUri)?.let { bitmap ->
                Timber.d("Image WAS ${bitmap.width}x${bitmap.height} kb:${bitmap.byteCount / 1000}")
                //Get orientation https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
                val imageInputStream = application.contentResolver.openInputStream(imageUri)
                // TODO Test on various OS versions
                val exif = if (Build.VERSION.SDK_INT > 23) ExifInterface(imageInputStream) else ExifInterface(imageUri.path)
                val orientation = exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_UNDEFINED)

                val out = ByteArrayOutputStream()
                val matrix = Matrix()
                // Scale
                val widthScale = MAX_TOPO_SIZE_PX.toFloat() / bitmap.width
                matrix.setScale(widthScale, widthScale)
                // Rotate
                matrix.postRotate(when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                })
                val scaledAndRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
                // TODO CHeck memory usage
                bitmap.recycle()
                // Compress
                scaledAndRotated.compress(Bitmap.CompressFormat.WEBP, 75, out)
                scaledAndRotated.recycle()
                out.toByteArray()
            }
        }
    }

    val localTopoImageBitmap = Transformations.map(localTopoImageBytes) {
        it?.let {
            val scaledAndCompressed = BitmapFactory.decodeStream(ByteArrayInputStream(it))
            Timber.d("Image IS ${scaledAndCompressed.width}x${scaledAndCompressed.height} kb:${scaledAndCompressed.byteCount / 1000}, filesize: ${it.size / 1000}kb")
            scaledAndCompressed
        }
    }

    val topoName = MutableLiveData<String?>()
    val topoNameError = Transformations.map(topoName) {
        if (it?.isEmpty() == true) "Can not be empty"
        else null
    }

    val shouldShowAddRouteButton = MutableLiveData<Boolean>().also { it.value = true }

    val activeRoute = MutableLiveData<Int>()

    val routes = HashMap<Int, Route>()

    val routeTypeUpdate = MutableLiveData<RouteType>()

    val halfFinishedTradGrades = mutableMapOf<Long, Pair<TradAdjectivalGrade?, TradTechnicalGrade?>>()

    val useVGradeForBouldering = getApplication<WcrApp>().prefs.getBoolean(PREF_USE_V_GRADE_FOR_BOULDERING, true)

    val routeColourUpdate = MutableLiveData<Int>()

    val visibilityTracker = mutableMapOf<Pair<Int, GradeType>, ObservableInt>()

    var sectorId: String = ""
    val submissionResult = MutableLiveData<SubmissionResult>()
    // We display a loader and disable the submit button when a submission is in progress
    val submitting = MutableLiveData<Boolean>().also {
        it.value = false
    }

    fun shouldShowGradePicker(fragmentId: Int, gradeType: GradeType): ObservableInt {
        return visibilityTracker[Pair(fragmentId, gradeType)]
                ?: ObservableInt(View.GONE).apply {
                    visibilityTracker[Pair(fragmentId, gradeType)] = this
                }
    }

    /*
        User actions
     */

    fun onUndoDrawing(view: View) {
        doUndoDrawing.value = null
    }

    fun onToggleDrawing(view: View) {
        Timber.d("Toggling drawing mode")
        if (isDrawing.get()) {
            isDrawing.set(false)
        } else {
            isDrawing.set(true)
        }
    }

    fun onPhotoTaken() {
        localTopoImage.value = photoUri.value
    }

    fun onAddRoute(activeRouteFragId: Int, path: PathCapture?) {
        if (!routes.containsKey(activeRouteFragId)) {
            routes[activeRouteFragId] = Route(name = "")
            Timber.d("Added empty route to view model with fragment id $activeRouteFragId")
        }
        activeRoute.value = activeRouteFragId
        // Link the route path capture to the Route in the view model
        routes[activeRouteFragId]?.path = path?.actionStack
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
                if (firstRouteToRight != null) activeRoute.value = firstRouteToRight
                else activeRoute.value = firstRouteToLeft
            } else if (routes.size == 1) {
                activeRoute.value = routes.keys.first()
            }
        }
    }

    fun onRouteRemoved(routeCount: Int?) {
        if (routeCount == 0) shouldShowAddRouteButton.value = true
    }

    fun onRoutePagerScroll(routeCount: Int?, position: Int, positionOffset: Float) {
        onRouteRemoved(routeCount)
        if (positionOffset > 0) {
            val onLastRoute = routeCount == position + 2
            shouldShowAddRouteButton.value = onLastRoute && positionOffset > 0.99
        } else {
            shouldShowAddRouteButton.value = routeCount == position + 1
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
            routeTypeUpdate.value = routeType
        }
    }

    fun onGradeChanged(fragmentId: Int, position: Int, gradeDropDown: GradeDropDown) {
        routeColourUpdate.value = fragmentId
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

    fun onClickSubmit(view: View) {
        Timber.d("Submit clicked for $sectorId")

        val hasName = !topoName.value.isNullOrEmpty()
        val hasImage = localTopoImage.value != null
        val hasAtLeast1Route = routes.size > 0
        val routesHaveNameDescriptionAndPath = routes.none { route ->
            val emptyName = route.value.name?.isEmpty() ?: true
            val emptyDescription = route.value.description.isNullOrEmpty()
            val noRoutePath = (route.value.path?.flatten()?.size ?: 0) < 2
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
                        submissionResult.postValue(SubmissionResult.success(submittedTopoId))
                    }, { e ->
                        Timber.e(e, "Submission Failed")
                        submissionResult.postValue(SubmissionResult.failure("Failed to onSubmit topo!"))
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
            submissionResult.postValue(SubmissionResult.failure(error))
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
            localTopoImageBytes.value?.let {
                val uploadTask = imageRef.putBytes(it)
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
    }

    private fun submit(sectorId: String): Single<String> {
        val topoName = topoName.value
        val topoImage = localTopoImage.value
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
            visibilityTracker[Pair(fragmentId, gradeType)]?.set(View.GONE)
        }
        when (routeType) {
            RouteType.TRAD -> visibilityTracker[Pair(fragmentId, GradeType.TRAD)]?.set(View.VISIBLE)
            RouteType.SPORT -> visibilityTracker[Pair(fragmentId, GradeType.SPORT)]?.set(View.VISIBLE)
            RouteType.BOULDERING -> {
                val prefs = getApplication<WcrApp>().prefs
                //TODO Settings toggle for PREF_USE_V_GRADE_FOR_BOULDERING
                if (prefs.getBoolean(PREF_USE_V_GRADE_FOR_BOULDERING, true)) {
                    visibilityTracker[Pair(fragmentId, GradeType.V)]?.set(View.VISIBLE)
                } else {
                    visibilityTracker[Pair(fragmentId, GradeType.FONT)]?.set(View.VISIBLE)
                }
            }
        }
    }
}

class SubmissionResult private constructor(val success: Boolean) {
    companion object {
        fun success(submittedTopoId: String): SubmissionResult {
            return SubmissionResult(true).withTopoId(submittedTopoId)
        }

        fun failure(errorMessage: String): SubmissionResult {
            return SubmissionResult(false).withErrorMessage(errorMessage)
        }
    }

    var submittedTopoId: String? = null
    var errorMessage: String? = null

    private fun withErrorMessage(errorMessage: String): SubmissionResult {
        this.errorMessage = errorMessage
        return this
    }

    private fun withTopoId(topoId: String): SubmissionResult {
        this.submittedTopoId = topoId
        return this
    }
}