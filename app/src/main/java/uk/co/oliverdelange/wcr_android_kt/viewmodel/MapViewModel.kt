package uk.co.oliverdelange.wcr_android_kt.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.PREF_BOTTOM_SHEET_OPENED
import uk.co.oliverdelange.wcr_android_kt.WcrApp
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb
import uk.co.oliverdelange.wcr_android_kt.db.dao.remote.TopoReporter
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.LocationRouteInfo
import uk.co.oliverdelange.wcr_android_kt.map.CragClusterItem
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.repository.RouteRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.service.AuthService
import uk.co.oliverdelange.wcr_android_kt.util.AbsentLiveData
import uk.co.oliverdelange.wcr_android_kt.view.map.DEV_MENU_CLICKS_REQUIRED
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapViewModel @Inject constructor(application: Application,
                                       private val locationRepository: LocationRepository,
                                       private val topoRepository: TopoRepository,
                                       private val routeRepository: RouteRepository,
                                       private val authService: AuthService,
                                       private val db: WcrDb,
                                       private val topoReporter: TopoReporter,
                                       private val analytics: FirebaseAnalytics) : AndroidViewModel(application) {

    /* State, exposed via LiveData. All MutableLiveData should be _private and exposed via a LiveData field*/

    private val disposables: CompositeDisposable = CompositeDisposable()

    private val _viewEvents = SingleLiveEvent<Event>()
    val viewEvents: LiveData<Event> get() = _viewEvents

    private val _userSignedIn = MutableLiveData<Boolean>().also {
        val signedIn = authService.currentUser() != null
        Timber.d("Initialising userSignedIn: $signedIn")
        it.value = signedIn
    }
    val userSignedIn: LiveData<Boolean> get() = _userSignedIn

    private val _mapType: MutableLiveData<Int> = MutableLiveData<Int>().also {
        it.value = GoogleMap.MAP_TYPE_NORMAL
    }
    val mapType: LiveData<Int> get() = _mapType

    private val _mapLabel: LiveData<String> = Transformations.map(_mapType) {
        if (it == GoogleMap.MAP_TYPE_NORMAL) "SAT" else "MAP"
    }
    val mapLabel: LiveData<String> get() = _mapLabel

    private val _mapMode: MutableLiveData<MapMode> = MutableLiveData<MapMode>().also {
        it.value = MapMode.DEFAULT_MODE
    }
    val mapMode: LiveData<MapMode> get() = _mapMode

    private val mapModesThatDisplayFab = listOf(MapMode.DEFAULT_MODE, MapMode.CRAG_MODE, MapMode.TOPO_MODE, MapMode.SECTOR_MODE)
    private val _showFab = MediatorLiveData<Boolean>().also {
        it.value = false
        fun getShowFab(): Boolean {
            val show = _userSignedIn.value == true && mapModesThatDisplayFab.contains(_mapMode.value)
            Timber.d("'showFab': $show")
            return show
        }
        it.addSource(_userSignedIn) { _ -> it.value = getShowFab() }
        it.addSource(_mapMode) { _ -> it.value = getShowFab() }
    }
    val showFab: LiveData<Boolean> get() = _showFab

    private val _selectedLocationId: MutableLiveData<String?> = MutableLiveData<String?>().also {
        it.value = null
    }
    val selectedLocationRouteInfo: LiveData<LocationRouteInfo?> = Transformations.switchMap(_selectedLocationId) {
        if (it != null) {
            Timber.d("SelectedLocationId changed to $it: Updating 'selectedLocationRouteInfo'")
            locationRepository.loadRouteInfoFor(it)
        } else {
            Timber.d("SelectedLocationId changed to null, 'selectedLocationRouteInfo' is now absent")
            AbsentLiveData.create()
        }
    }

    // Distinct until changed stop the data reloading when the underlying Location object has not changed
    // For example, when its uploaded to firestore and the DB record gets its 'uploaded at' field updated
    // However the Location domain object doesn't have this field, so the compared Location objects are equal
    val selectedLocation: LiveData<Location?> = Transformations.distinctUntilChanged(
            Transformations.switchMap(_selectedLocationId) {
                if (it != null) {
                    Timber.d("SelectedLocationId changed to $it: Updating 'selectedLocation'")
                    locationRepository.load(it)
                } else {
                    Timber.d("SelectedLocationId changed to null, 'selectedLocation' is now absent")
                    AbsentLiveData.create()
                }
            }
    )

    val submitButtonLabel = Transformations.map(selectedLocation) {
        if (it?.type == LocationType.CRAG) "Submit sector" else "Submit topo"
    }

    val crags: LiveData<List<Location>> = Transformations.distinctUntilChanged(locationRepository.loadCrags())

    val cragClusterItems = Transformations.map(crags) {
        it.map { location -> CragClusterItem(location) }
    }

    val sectors: LiveData<List<Location>> = Transformations.distinctUntilChanged(
            Transformations.switchMap(selectedLocation) { selectedLocation ->
                if (selectedLocation?.id != null) {
                    Timber.d("SelectedLocation changed to ${selectedLocation.id}: Updating 'sectors'")
                    when (selectedLocation.type) {
                        LocationType.CRAG -> locationRepository.loadSectorsFor(selectedLocation.id)
                        LocationType.SECTOR -> selectedLocation.parentLocationId?.let { parentID ->
                            locationRepository.loadSectorsFor(parentID)
                        }
                    }
                } else {
                    Timber.d("SelectedLocation changed to null, 'sectors' is now absent")
                    AbsentLiveData.create()
                }
            }
    )

    val topos: LiveData<List<TopoAndRoutes>> = Transformations.switchMap(selectedLocation) { selectedLocation ->
        selectedLocation?.id?.let {
            Timber.d("SelectedLocation changed to $it: Updating 'topos'")
            analytics.logEvent("wcr_load_topos", Bundle().apply {
                putString("locationId", selectedLocation.id)
                putString("locationType", selectedLocation.type.toString())
            })
            topoRepository.loadToposForLocation(selectedLocation.id)
        }
    }

    private val _mapLatLngBounds = MediatorLiveData<List<LatLng>>().also {
        it.value = emptyList()
        it.addSource(selectedLocation) { location ->
            if (location?.type == LocationType.SECTOR) {
                Timber.d("selectedLocation changed to a SECTOR, setting mapLatLngBounds")
                it.value = listOf(location.latlng)
            } else if (location?.type == LocationType.CRAG) {
                Timber.d("selectedLocation changed to a CRAG, setting mapLatLngBounds")
                val locations = listOf(location).plus(sectors.value ?: emptyList())
                it.value = locations.map { l -> l.latlng }
            }
        }
        it.addSource(crags) { crags ->
            Timber.d("crags changed, setting mapLatLngBounds")
            it.value = crags.map { crag -> crag.latlng }
        }
        it.addSource(sectors) { sectors ->
            if (selectedLocation.value?.type == LocationType.CRAG) {
                Timber.d("sectors changed, and selectedLocation is a CRAG, setting mapLatLngBounds")
                it.value = sectors?.plus(selectedLocation.value!!)?.map { l -> l.latlng }
            }
        }
        it.addSource(_mapMode) { mapMode ->
            val numberOfCrags: Int = crags.value?.size ?: 0
            if (mapMode == MapMode.DEFAULT_MODE && numberOfCrags > 0) {
                Timber.d("mapMode changed to DEFAULT_MODE, and there are crags, setting mapLatLngBounds")
                it.value = crags.value!!.map { c -> c.latlng }
            }
        }
    }
    val mapLatLngBounds: LiveData<List<LatLng>> get() = _mapLatLngBounds

    private val _bottomSheetState: MutableLiveData<Int> = MutableLiveData()
    val bottomSheetState: MutableLiveData<Int> get() = _bottomSheetState
    private val _bottomSheetRequestedState: MutableLiveData<Int> = MutableLiveData()
    val bottomSheetRequestedState: MutableLiveData<Int> get() = _bottomSheetRequestedState
    val bottomSheetTitle: LiveData<String> = Transformations.map(selectedLocation) {
        it?.name
    }

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: MutableLiveData<String> get() = _searchQuery
    val searchResults: LiveData<List<SearchSuggestionItem>> = Transformations.switchMap(_searchQuery) { query ->
        Timber.i("Search query changed to: $query")
        analytics.logEvent("wcr_search", Bundle().apply { putString("query", query) })
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            val mediator = MediatorLiveData<List<SearchSuggestionItem>>()
            mediator.addSource(locationRepository.search(query)) { locations: List<Location>? ->
                addToSearchItems(mediator, locations?.map {
                    val type = if (it.type == LocationType.CRAG) SearchResultType.CRAG else SearchResultType.SECTOR
                    SearchSuggestionItem(it.name, type, it.id)
                })
            }
            mediator.addSource(topoRepository.search(query)) { topos: List<Topo>? ->
                addToSearchItems(mediator, topos?.map { SearchSuggestionItem(it.name, SearchResultType.TOPO, it.id) })
            }
            mediator.addSource(routeRepository.searchOnName(query)) { routes ->
                addToSearchItems(mediator, routes?.map {
                    val type = when (it.type) {
                        RouteType.TRAD -> SearchResultType.ROUTE_TRAD
                        RouteType.SPORT -> SearchResultType.ROUTE_SPORT
                        RouteType.BOULDERING -> SearchResultType.ROUTE_BOULDER
                        else -> SearchResultType.ROUTE
                    }
                    SearchSuggestionItem(it.name, type, it.id)
                })
            }
            mediator
        } else {
            AbsentLiveData.create<List<SearchSuggestionItem>>()
        }
    }


    /*  User Actions  */

    fun onTutorialStart() {
        collapseBottomSheet()
    }

    @SuppressLint("CheckResult")
    fun onCragTutorialFinish() {
        Observable.fromCallable { locationRepository.randomCragId() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { id ->
                    Timber.d("Random location id = $id")
                    _selectedLocationId.value = id
                    _mapMode.value = MapMode.CRAG_MODE
                }
    }

    fun onLocationInfoTutorialComplete() {
        expandBottomSheet()
    }

    fun onTopoTutorialComplete() {
        collapseBottomSheet()
    }

    fun onClickSignInButton() {
        _viewEvents.value = NavigateToSignIn
    }

    fun onUserSignInSuccess() {
        _userSignedIn.value = true
        val user = FirebaseAuth.getInstance().currentUser
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null)
        Timber.d("User successfully signed in: ${user?.email}")
    }

    fun onUserSignInFail() {
        val user = FirebaseAuth.getInstance().currentUser
        _userSignedIn.value = user != null
        Timber.d("User sign in failed")
    }

    fun onUserSignOut() {
        _userSignedIn.value = false
        Timber.d("User signed out")
    }

    fun onToggleBottomSheetState(view: View) {
        Timber.d("Toggling bottom sheet state")
        if (bottomSheetIsCollapsed()) {
            expandBottomSheet()
        } else {
            collapseBottomSheet()
        }
    }

    fun onBottomSheetStateChanged(newState: Int) {
        _bottomSheetState.value = newState
    }

    private var bottomSheetExpandedPrefSet = false
    fun onBottomSheetExpand() {
        if (!bottomSheetExpandedPrefSet) {
            Timber.d("Bottom sheet expanded for first time since app open")
            with(getApplication<WcrApp>().prefs.edit()) {
                putBoolean(PREF_BOTTOM_SHEET_OPENED, true)
                apply()
            }
            bottomSheetExpandedPrefSet = true
        }
    }

    fun onSubmit(view: View) {
        when (_mapMode.value) {
            MapMode.DEFAULT_MODE -> _mapMode.value = MapMode.SUBMIT_CRAG_MODE
            MapMode.CRAG_MODE -> _mapMode.value = MapMode.SUBMIT_SECTOR_MODE
            MapMode.SECTOR_MODE, MapMode.TOPO_MODE -> _mapMode.value = MapMode.SUBMIT_TOPO_MODE
            else -> Timber.e("Submit FAB clicked when it shouldn't be visible!")
        }
    }

    fun onLocationSubmitted(submittedLocationId: String, locationType: LocationType) {
        if (locationType == LocationType.CRAG) {
            Timber.d("Crag submitted, changing map mode")
            _mapMode.value = MapMode.CRAG_MODE
        } else {
            Timber.d("Sector submitted, changing map mode")
            _mapMode.value = MapMode.SECTOR_MODE
        }
        _selectedLocationId.value = submittedLocationId
    }

    fun onSubmitTopoActivityComplete() {
        _mapMode.value = MapMode.SECTOR_MODE
    }

    fun onToggleMap(view: View) {
        Timber.d("Toggling map type")
        if (GoogleMap.MAP_TYPE_NORMAL == _mapType.value) {
            _mapType.value = GoogleMap.MAP_TYPE_SATELLITE
        } else {
            _mapType.value = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    fun onClusterItemClick(id: String?) {
        Timber.d("Selecting crag with id %s", id)
        _selectedLocationId.value = id
        tmpMapMode = MapMode.CRAG_MODE
    }

    fun onMapMarkerClick(id: String?) {
        Timber.d("Selecting sector with id %s", id)
        _selectedLocationId.value = id
        tmpMapMode = MapMode.SECTOR_MODE
    }

    /**
     *  Slightly hacky way of deferring the bottom sheet layout until after the map animation has finished
     */
    var tmpMapMode: MapMode? = null
    fun onMapAnimationFinished() {
        Timber.d("Map has finished animating, we can do layout now")
        tmpMapMode?.let {
            _mapMode.value = it
            tmpMapMode = null
        }
    }

    // User taps the ! button on the topo
    fun onTapReportTopo(topo: Topo) {
        Timber.w("User might want to report a topo! $topo")
        _viewEvents.postValue(ReportTopo(topo))
    }

    // User submits the topo report
    fun onReportTopo(report: String, topo: Topo) {
        Timber.w("User has reported a topo! Report: $report. Reported topo: $topo")
        topoReporter.reportTopo(topo, report)
    }

    fun onSearchBarUnfocus() {
//        collapseBottomSheet()
    }

    fun onSearchBarFocus() {
        hideBottomSheet()
    }

    fun onSearchSuggestionClicked(searchSuggestion: SearchSuggestionItem) {
        when (searchSuggestion.type) {
            SearchResultType.CRAG -> {
                collapseBottomSheet()
                onClusterItemClick(searchSuggestion.id)
            }
            SearchResultType.SECTOR -> {
                collapseBottomSheet()
                onMapMarkerClick(searchSuggestion.id)
            }
            SearchResultType.TOPO -> selectTopo(searchSuggestion.id)
            SearchResultType.ROUTE, SearchResultType.ROUTE_BOULDER, SearchResultType.ROUTE_TRAD, SearchResultType.ROUTE_SPORT -> {
                selectRoute(searchSuggestion.id)
            }
        }
    }

    fun onNavigateBack() {
        if (bottomSheetIsExpanded() ||
                bottomSheetIsHidden()) {
            collapseBottomSheet()
        } else {
            when (_mapMode.value) {
                MapMode.DEFAULT_MODE -> collapseBottomSheet()
                MapMode.CRAG_MODE -> {
                    _mapMode.value = MapMode.DEFAULT_MODE
                    _selectedLocationId.value = null
                }
                MapMode.SUBMIT_CRAG_MODE -> _mapMode.value = MapMode.DEFAULT_MODE
                MapMode.SECTOR_MODE, MapMode.TOPO_MODE -> {
                    _mapMode.value = MapMode.CRAG_MODE
                    selectedLocation.value?.parentLocationId?.let { _selectedLocationId.value = it }
                }
                MapMode.SUBMIT_SECTOR_MODE -> _mapMode.value = MapMode.CRAG_MODE
                MapMode.SUBMIT_TOPO_MODE -> _mapMode.value = MapMode.SECTOR_MODE
            }
        }
    }

    private var devMenuClicksLeft = DEV_MENU_CLICKS_REQUIRED
    fun buildVersionClicked() {
        devMenuClicksLeft--
        if (devMenuClicksLeft <= 3) {
            _viewEvents.postValue(ShowXClicksToDevMenuToast(devMenuClicksLeft))
        }
        if (devMenuClicksLeft == 0) {
            _viewEvents.postValue(ShowDevMenu)
        }
    }

    fun nukeDb() {
        disposables.add(Completable.fromAction {
            Timber.d("Nuking DB")
            db.clearAllTables()
        }
                .subscribeOn(Schedulers.io())
                .subscribe {
                    Timber.d("DB Nuked")
                })
    }

    /*
        Private
     */

    private fun selectTopo(id: String?) {
        Timber.d("Selecting topo with id %s", id)
        id?.let {
            expandBottomSheet()
            _mapMode.value = MapMode.TOPO_MODE
            Observable.fromCallable { topoRepository.topoDao.get(it) } //FIXME Why is the dao exposed? And why am i using it?
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { topo ->
                        _selectedLocationId.value = topo.locationId
                        _viewEvents.postValue(ScrollToTopo(topo.id))
                    }
        }
    }

    private fun selectRoute(id: String?) {
        Timber.d("Selecting route with id %s", id)
        id?.let { routeId ->
            Observable.fromCallable { routeRepository.get(routeId) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { route ->
                        selectTopo(route?.topoId)
                        //TODO Select route in route pager
                    }
        }
    }

    private fun expandBottomSheet() {
        _bottomSheetRequestedState.value = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun collapseBottomSheet() {
        _bottomSheetRequestedState.value = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun hideBottomSheet() {
        _bottomSheetRequestedState.value = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun bottomSheetIsHidden() = _bottomSheetState.value == BottomSheetBehavior.STATE_HIDDEN

    private fun bottomSheetIsExpanded() = _bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED
    private fun bottomSheetIsCollapsed() = _bottomSheetState.value == BottomSheetBehavior.STATE_COLLAPSED

    private fun addToSearchItems(mediator: MediatorLiveData<List<SearchSuggestionItem>>, new: List<SearchSuggestionItem>?) {
        val existing = mediator.value
        if (existing != null && new != null) mediator.value = new + existing
        else mediator.value = new
    }
}

enum class MapMode {
    DEFAULT_MODE, CRAG_MODE, SECTOR_MODE, TOPO_MODE, SUBMIT_CRAG_MODE, SUBMIT_SECTOR_MODE, SUBMIT_TOPO_MODE
}