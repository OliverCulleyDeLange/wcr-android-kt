package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableInt
import android.view.View
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitTopoViewModel @Inject constructor(private val topoRepository: TopoRepository) : ViewModel() {

    val topoName = MutableLiveData<String>()
    val topoNameError = MutableLiveData<String>()

    val routes = MutableLiveData<MutableMap<Int, Route>>().also { it.value = mutableMapOf() }

    fun routeNameChanged(fragmentId: Int, text: CharSequence) {
        routes.value?.get(fragmentId)?.let {
            it.name = text.toString()
        }
    }

    fun routeDescriptionChanged(fragmentId: Int, text: CharSequence) {
        routes.value?.get(fragmentId)?.let {
            it.description = text.toString()
        }
    }

    fun routeTypeChanged(fragmentId: Int, position: Int) {
        routes.value?.get(fragmentId)?.let {
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
    fun gradeChanged(fragmentId: Int, position: Int, gradeDropDown: GradeDropDown) {
        routes.value?.get(fragmentId)?.let { route ->
            when (gradeDropDown) {
                GradeDropDown.V -> route.grade = Grade.from(VGrade.values()[position])
                GradeDropDown.FONT -> route.grade = Grade.from(FontGrade.values()[position])
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

    val submitButtonEnabled = MediatorLiveData<Boolean>().also {
        it.value = false
        it.addSource(topoName) { locationName: String? ->
            if (locationName == null || locationName.isBlank()) {
                it.value = false; topoNameError.value = "Can not be empty"
            } else {
                it.value = true; topoNameError.value = null
            }
        }
    }

    fun submit(sectorId: Long): MutableLiveData<Pair<Long, Array<Long>>> {
        val locationName = topoName.value
        if (locationName != null) {
            val topo = Topo(name = locationName, locationId = sectorId)
            return topoRepository.save(topo, routes.value?.values ?: emptyList())
        } else {
            Timber.e("Submit attempted but not all information available. (Submit button shouldn't have been active!)")
            return MutableLiveData()
        }
    }
}
