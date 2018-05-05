package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import android.databinding.ObservableInt
import android.net.Uri
import android.view.View
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.service.WorkerService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitTopoViewModel @Inject constructor(private val topoRepository: TopoRepository,
                                              private val workerService: WorkerService) : ViewModel() {

    var topoImage = MutableLiveData<Uri>()
    val topoName = MutableLiveData<String>()
    val topoNameError = Transformations.map(topoName) {
        setEnableSubmit()
        if (it.isEmpty()) "Can not be empty"
        else null
    }

    val routes = HashMap<Int, Route>()

    fun routeNameChanged(fragmentId: Int, text: CharSequence) {
        routes[fragmentId]?.let {
            it.name = text.toString()
        }
        setEnableSubmit()
    }

    fun routeDescriptionChanged(fragmentId: Int, text: CharSequence) {
        routes[fragmentId]?.let {
            it.description = text.toString()
        }
        setEnableSubmit()
    }

    fun routeTypeChanged(fragmentId: Int, position: Int) {
        routes[fragmentId]?.let {
            val routeType = RouteType.values()[position]
            it.type = routeType
            setGradeVisibility(fragmentId, routeType)
        }
    }

    private fun setGradeVisibility(fragmentId: Int, routeType: RouteType): Unit? {
        for (gradeType in GradeType.values()) {
            visibilityTracker[Pair(fragmentId, gradeType)]?.set(View.GONE)
        }
        return when (routeType) {
            RouteType.TRAD -> visibilityTracker[Pair(fragmentId, GradeType.TRAD)]?.set(View.VISIBLE)
            RouteType.SPORT -> visibilityTracker[Pair(fragmentId, GradeType.SPORT)]?.set(View.VISIBLE)
            RouteType.BOULDERING -> {
                visibilityTracker[Pair(fragmentId, GradeType.FONT)]?.set(View.VISIBLE)
                visibilityTracker[Pair(fragmentId, GradeType.V)]?.set(View.VISIBLE)
            }
        }
    }

    val halfFinishedTradGrades = mutableMapOf<Long, Pair<TradAdjectivalGrade?, TradTechnicalGrade?>>()
    val boulderingGradeType = MutableLiveData<GradeType>()
    var autoGradeChange = false
    fun gradeChanged(fragmentId: Int, position: Int, gradeDropDown: GradeDropDown) {
        routes[fragmentId]?.let { route ->
            when (gradeDropDown) {
                GradeDropDown.V -> {
                    if (!autoGradeChange) {
                        route.grade = Grade.from(VGrade.values()[position])
                        boulderingGradeType.value = GradeType.V
                    } else {
                        autoGradeChange = false
                    }
                }
                GradeDropDown.FONT -> {
                    if (!autoGradeChange) {
                        route.grade = Grade.from(FontGrade.values()[position])
                        boulderingGradeType.value = GradeType.FONT
                    } else {
                        autoGradeChange = false
                    }
                }
                GradeDropDown.SPORT -> route.grade = Grade.from(SportGrade.values()[position])
                GradeDropDown.TRAD_ADJ -> {
                    val routeId = fragmentId.toLong()
                    val chosenTradAdjGrade = TradAdjectivalGrade.values()[position]
                    val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                    if (halfFinishedTradGrade?.second != null) {
                        route.grade = Grade.from(chosenTradAdjGrade, halfFinishedTradGrade.second!!)
                    }
                    halfFinishedTradGrades[routeId] = Pair(chosenTradAdjGrade, halfFinishedTradGrades[routeId]?.second)
                }
                GradeDropDown.TRAD_TECH -> {
                    val routeId = fragmentId.toLong()
                    val chosenTradTechGrade = TradTechnicalGrade.values()[position]
                    val halfFinishedTradGrade = halfFinishedTradGrades[routeId]
                    if (halfFinishedTradGrade?.first != null) {
                        route.grade = Grade.from(halfFinishedTradGrade.first!!, chosenTradTechGrade)
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

    fun setEnableSubmit() {
        submitButtonEnabled.set(!topoName.value.isNullOrEmpty() &&
                topoImage.value != null &&
                routes.none {
                    it.value.name.isNullOrEmpty() ||
                            it.value.description.isNullOrEmpty()
                })
    }

    fun submit(sectorId: Long): MutableLiveData<Pair<Long, Array<Long>>> {
        val topoName = topoName.value
        if (topoName != null) {
            val topo = Topo(name = topoName, locationId = sectorId)
            val savedIds = topoRepository.save(topo, routes.values)
            workerService.updateRouteInfo(sectorId)
            return savedIds
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            return MutableLiveData()
        }
    }
}
