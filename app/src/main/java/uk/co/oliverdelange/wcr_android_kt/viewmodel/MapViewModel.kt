package uk.co.oliverdelange.wcr_android_kt.viewmodel

import android.app.Application
import android.content.Context
import android.view.View
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.LocationRouteInfo
import uk.co.oliverdelange.wcr_android_kt.db.preload
import uk.co.oliverdelange.wcr_android_kt.map.CragClusterItem
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.repository.LocationRepository
import uk.co.oliverdelange.wcr_android_kt.repository.RouteRepository
import uk.co.oliverdelange.wcr_android_kt.repository.TopoRepository
import uk.co.oliverdelange.wcr_android_kt.util.AbsentLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapViewModel @Inject constructor(application: Application,
                                       val locationRepository: LocationRepository,
                                       val topoRepository: TopoRepository,
                                       val routeRepository: RouteRepository,
                                       val db: WcrDb) : AndroidViewModel(application) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    val userSignedIn = MutableLiveData<Boolean>().also {
        val signedIn = FirebaseAuth.getInstance().currentUser != null
        Timber.v("Initialising userSignedIn: $signedIn")
        it.value = signedIn
    }

    fun onUserSignInSuccess() {
        userSignedIn.value = true
        val user = FirebaseAuth.getInstance().currentUser
        Timber.d("User successfully signed in: ${user?.email}")
    }

    fun onUserSignInFail() {
        val user = FirebaseAuth.getInstance().currentUser
        userSignedIn.value = user != null
        Timber.d("User sign in failed")
    }

    val mapType: MutableLiveData<Int> = MutableLiveData<Int>().also {
        it.value = GoogleMap.MAP_TYPE_NORMAL
    }
    val mapLabel: LiveData<String> = Transformations.map(mapType) {
        if (it == 1) "SAT" else "MAP"
    }
    val mapMode: MutableLiveData<MapMode> = MutableLiveData<MapMode>().also {
        it.value = MapMode.DEFAULT_MODE
    }

    private val mapModesThatDisplayFab = listOf(MapMode.DEFAULT_MODE, MapMode.CRAG_MODE, MapMode.TOPO_MODE, MapMode.SECTOR_MODE)
    val showFab = MediatorLiveData<Boolean>().also {
        it.value = false
        fun getShowFab(): Boolean {
            val show = userSignedIn.value == true && mapModesThatDisplayFab.contains(mapMode.value)
            Timber.v("'showFab': $show")
            return show
        }
        it.addSource(userSignedIn) { _ -> it.value = getShowFab() }
        it.addSource(mapMode) { _ -> it.value = getShowFab() }
    }
    val selectedLocationId: MutableLiveData<String?> = MutableLiveData<String?>().also {
        it.value = null
    }
    val selectedLocationRouteInfo: LiveData<LocationRouteInfo?> = Transformations.switchMap(selectedLocationId) {
        if (it != null) {
            Timber.v("SelectedLocationId changed to $it: Updating 'selectedLocationRouteInfo'")
            locationRepository.loadRouteInfoFor(it)
        } else {
            Timber.v("SelectedLocationId changed to null, 'selectedLocationRouteInfo' is now absent")
            AbsentLiveData.create()
        }
    }

    // Distinct until changed stop the data reloading when the underlying Location object has not changed
    // For example, when its uploaded to firestore and the DB record gets its 'uploaded at' field updated
    // However the Location domain object doesn't have this field, so the compared Location objects are equal
    val selectedLocation: LiveData<Location?> = Transformations.distinctUntilChanged(
            Transformations.switchMap(selectedLocationId) {
                if (it != null) {
                    Timber.v("SelectedLocationId changed to $it: Updating 'selectedLocation'")
                    locationRepository.load(it)
                } else {
                    Timber.v("SelectedLocationId changed to null, 'selectedLocation' is now absent")
                    AbsentLiveData.create()
                }
            }
    )

    val crags: LiveData<List<Location>> = Transformations.distinctUntilChanged(locationRepository.loadCrags())

    val cragClusterItems = Transformations.map(crags) {
        it.map { location -> CragClusterItem(location) }
    }

    val sectors: LiveData<List<Location>?> = Transformations.distinctUntilChanged(
            Transformations.switchMap(selectedLocation) { selectedLocation ->
                if (selectedLocation?.id != null) {
                    Timber.v("SelectedLocation changed to ${selectedLocation.id}: Updating 'sectors'")
                    when (selectedLocation.type) {
                        LocationType.CRAG -> locationRepository.loadSectorsFor(selectedLocation.id)
                        LocationType.SECTOR -> selectedLocation.parentLocationId?.let { parentID ->
                            locationRepository.loadSectorsFor(parentID)
                        }
                    }
                } else {
                    Timber.v("SelectedLocation changed to null, 'sectors' is now absent")
                    AbsentLiveData.create()
                }
            }
    )

    val selectedTopoId = MutableLiveData<String>()
    val topos: LiveData<List<TopoAndRoutes>> = Transformations.switchMap(selectedLocation) { selectedLocation ->
        selectedLocation?.id?.let {
            Timber.d("SelectedLocation changed to $it: Updating 'topos'")
            when (selectedLocation.type) {
                LocationType.SECTOR -> topoRepository.loadToposForLocation(selectedLocation.id)
                LocationType.CRAG -> getToposForCrag(selectedLocation.id)
            }
        }
    }

    val mapLatLngBounds = MediatorLiveData<List<LatLng>>().also {
        it.value = emptyList()
        it.addSource(crags) { crags ->
            it.value = crags.map { crag -> crag.latlng }
        }
        it.addSource(sectors) { sectors ->
            if (selectedLocation.value?.type == LocationType.CRAG) {
                it.value = sectors?.plus(selectedLocation.value!!)?.map { l -> l.latlng }
            }
        }
        it.addSource(mapMode) { mapMode ->
            val numberOfCrags: Int = crags.value?.size ?: 0
            if (mapMode == MapMode.DEFAULT_MODE && numberOfCrags > 0) {
                it.value = crags.value!!.map { c -> c.latlng }
            }
        }
    }

    private fun getToposForCrag(cragId: String): LiveData<List<TopoAndRoutes>> {
        Timber.d("Getting topos for crag with id: %s", cragId)
        val topos: MediatorLiveData<List<TopoAndRoutes>> = MediatorLiveData()
        val loadSectorsForCrag = locationRepository.loadSectorsFor(cragId)
        topos.addSource(loadSectorsForCrag) { sectorsForCrag ->
            topos.removeSource(loadSectorsForCrag)
            if (sectorsForCrag?.isNotEmpty() == true) {
                sectorsForCrag.forEach { sector ->
                    sector.id?.let { sectorId ->
                        topos.addSource(topoRepository.loadToposForLocation(sectorId)) {
                            it?.let { newToposAndRoutes ->
                                topos.value = newToposAndRoutes.plus(topos.value ?: emptyList())
                            }
                        }
                    }
                }
            } else {
                topos.value = null //TODO NPE possible?
            }
        }
        return topos
    }

    val bottomSheetState: MutableLiveData<Int> = MutableLiveData()
    val bottomSheetTitle: LiveData<String> = Transformations.map(selectedLocation) {
        it?.name
    }

    var bottomSheetExpanded = false
    fun onBottomSheetExpand() {
        if (!bottomSheetExpanded) {
            Timber.v("Bottom sheet expanded for first time since app open")
            with(getApplication<WcrApp>().prefs.edit()) {
                putBoolean(PREF_BOTTOM_SHEET_OPENED, true)
                apply()
            }
            bottomSheetExpanded = true
        }
    }

    fun submit(view: View) {
        when (mapMode.value) {
            MapMode.DEFAULT_MODE -> mapMode.value = MapMode.SUBMIT_CRAG_MODE
            MapMode.CRAG_MODE -> mapMode.value = MapMode.SUBMIT_SECTOR_MODE
            MapMode.SECTOR_MODE, MapMode.TOPO_MODE -> mapMode.value = MapMode.SUBMIT_TOPO_MODE
            else -> Timber.e("Submit FAB clicked when it shouldn't be visible!")
        }
    }

    fun toggleMap(view: View) {
        Timber.d("Toggling map type")
        if (GoogleMap.MAP_TYPE_NORMAL == mapType.value) {
            mapType.value = GoogleMap.MAP_TYPE_SATELLITE
        } else {
            mapType.value = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    fun selectCrag(id: String?) {
        Timber.d("Selecting crag with id %s", id)
        selectedLocationId.postValue(id)
        mapMode.value = MapMode.CRAG_MODE
    }

    fun selectSector(id: String?) {
        Timber.d("Selecting sector with id %s", id)
        selectedLocationId.postValue(id)
        mapMode.value = MapMode.SECTOR_MODE
    }

    fun selectTopo(id: String?) {
        Timber.d("Selecting topo with id %s", id)
        id?.let {
            bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED
            mapMode.value = MapMode.TOPO_MODE
            Observable.fromCallable { topoRepository.topoDao.get(it) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { topo ->
                        selectedLocationId.value = topo.locationId
                        selectedTopoId.value = topo.id
                    }
        }
    }

    fun selectRoute(id: String?) {
        Timber.d("Selecting route with id %s", id)
        id?.let { routeId ->
            Observable.fromCallable { routeRepository.get(routeId) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { route ->
                        selectTopo(route.topoId)
                        //TODO Select route in route pager
                    }
        }
    }

    val searchQuery = MutableLiveData<String>()
    val searchResults: LiveData<List<SearchSuggestionItem>> = Transformations.switchMap(searchQuery) { query ->
        Timber.i("Search query changed to: $query")
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

    fun onSearchBarUnfocus() {
        bottomSheetState.value = BottomSheetBehavior.STATE_COLLAPSED

    }

    fun onSearchBarFocus() {
        bottomSheetState.value = BottomSheetBehavior.STATE_HIDDEN

    }

    fun onSearchSuggestionClicked(searchSuggestion: SearchSuggestionItem) {
        when (searchSuggestion.type) {
            SearchResultType.CRAG -> selectCrag(searchSuggestion.id)
            SearchResultType.SECTOR -> selectSector(searchSuggestion.id)
            SearchResultType.TOPO -> selectTopo(searchSuggestion.id)
            SearchResultType.ROUTE, SearchResultType.ROUTE_BOULDER, SearchResultType.ROUTE_TRAD, SearchResultType.ROUTE_SPORT -> {
                selectRoute(searchSuggestion.id)
            }
        }
    }

    private fun addToSearchItems(mediator: MediatorLiveData<List<SearchSuggestionItem>>, new: List<SearchSuggestionItem>?) {
        val existing = mediator.value
        if (existing != null && new != null) mediator.value = new + existing
        else mediator.value = new
    }

    fun back() {
        if (bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED ||
                bottomSheetState.value == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetState.value = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            when (mapMode.value) {
                MapMode.DEFAULT_MODE -> bottomSheetState.value = BottomSheetBehavior.STATE_COLLAPSED
                MapMode.CRAG_MODE -> {
                    mapMode.value = MapMode.DEFAULT_MODE
                    selectedLocationId.value = null
                }
                MapMode.SUBMIT_CRAG_MODE -> mapMode.value = MapMode.DEFAULT_MODE
                MapMode.SECTOR_MODE, MapMode.TOPO_MODE -> {
                    mapMode.value = MapMode.CRAG_MODE
                    selectedLocation.value?.parentLocationId?.let { selectedLocationId.value = it }
                }
                MapMode.SUBMIT_SECTOR_MODE -> mapMode.value = MapMode.CRAG_MODE
                MapMode.SUBMIT_TOPO_MODE -> mapMode.value = MapMode.SECTOR_MODE
            }
        }
    }

    fun nukeDb(applicationContext: Context) {
        disposables.add(Completable.fromAction {
            Timber.d("Nuking DB")
            db.clearAllTables()
        }.andThen(preload(WcrDb.getInstance(applicationContext)))
                .subscribeOn(Schedulers.io())
                .subscribe {
                    Timber.d("DB Nuked")
                })
    }
}

enum class MapMode {
    DEFAULT_MODE, CRAG_MODE, SECTOR_MODE, TOPO_MODE, SUBMIT_CRAG_MODE, SUBMIT_SECTOR_MODE, SUBMIT_TOPO_MODE
}