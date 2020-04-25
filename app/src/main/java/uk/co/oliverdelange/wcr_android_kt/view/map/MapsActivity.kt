package uk.co.oliverdelange.wcr_android_kt.view.map

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.arlib.floatingsearchview.FloatingSearchView
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.maps.android.MarkerManager
import com.google.maps.android.clustering.ClusterManager
import com.squareup.picasso.Picasso
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_maps.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.PREFS_KEY
import uk.co.oliverdelange.wcr_android_kt.PREF_BOTTOM_SHEET_OPENED
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.ActivityMapsBinding
import uk.co.oliverdelange.wcr_android_kt.map.*
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.model.SearchResultType.*
import uk.co.oliverdelange.wcr_android_kt.model.SearchSuggestionItem
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.service.downloadSync
import uk.co.oliverdelange.wcr_android_kt.util.hideKeyboard
import uk.co.oliverdelange.wcr_android_kt.util.replaceFragment
import uk.co.oliverdelange.wcr_android_kt.util.stateFromInt
import uk.co.oliverdelange.wcr_android_kt.view.TutorialManager
import uk.co.oliverdelange.wcr_android_kt.view.submit.SubmitActivity
import uk.co.oliverdelange.wcr_android_kt.view.submit.SubmitLocationFragment
import uk.co.oliverdelange.wcr_android_kt.viewmodel.*
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapMode.*
import java.lang.Math.round
import javax.inject.Inject

const val MAP_ANIMATION_DURATION = 400
const val MAP_PADDING_INSET = 150
const val MAP_PADDING_TOP = 150

const val EXTRA_SECTOR_ID = "EXTRA_SECTOR_ID"
const val ACTIVITY_RESULT_SUBMIT = 999
const val ACTIVITY_RESULT_SIGNIN = 998

/**
The Main Activity of the app. Shows a map with markers for crags, which when clicked reveal its sectors.
Floating search bar allows searching crags, sectors and routes. It also give access to drawer menu

Bottom sheet gives information on the selected crag/sector location map marker, including topos.
- See [BottomSheetFragment]
 */
class MapsActivity : AppCompatActivity(),
        HasSupportFragmentInjector,
        OnMapReadyCallback,
        ClusterManager.OnClusterItemClickListener<CragClusterItem>,
        GoogleMap.OnMarkerClickListener,
        SubmitLocationFragment.ActivityInteractor {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun supportFragmentInjector(): DispatchingAndroidInjector<Fragment> {
        return dispatchingAndroidInjector
    }

    private val submitCragFragment = SubmitLocationFragment.newCragSubmission()
    private val submitSectorFragment = SubmitLocationFragment.newSectorSubmission()
    private val bottomSheetFragment = BottomSheetFragment.newBottomSheet()
    private val welcomeFragment = WelcomeFragment.newWelcomeFragment()

    internal lateinit var map: GoogleMap
    private var bottomSheet: BottomSheetBehavior<CardView>? = null
    private val tutorialManager = TutorialManager()

    private lateinit var clusterManager: ClusterManager<CragClusterItem>
    private lateinit var sectorMarkers: MarkerManager.Collection
    private lateinit var drawerWrapper: DrawerWrapper
    private lateinit var toast: Toast

    internal lateinit var binding: ActivityMapsBinding

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.v("MapsActivity : onCreate")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)
        binding.lifecycleOwner = this
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)
        binding.vm = viewModel
        binding.floatingSearchView.setQueryTextSize(14)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        drawerWrapper = DrawerWrapper(this, viewModel)
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        initialiseFloatingSearchBar()
        initialiseBottomSheet()
        downloadSync()
    }

    override fun onBackPressed() {
        binding.vm?.onNavigateBack()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ACTIVITY_RESULT_SUBMIT -> {
                binding.vm?.onSubmitTopoActivityComplete()
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("User submitted topo")
                }
            }
            ACTIVITY_RESULT_SIGNIN -> {
//                val response = IdpResponse.fromResultIntent(data)
                if (resultCode == RESULT_OK) {
                    binding.vm?.onUserSignInSuccess()
                } else {
                    binding.vm?.onUserSignInFail()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Timber.d("Google map ready")
        map = googleMap
        setMapBottomPadding(bottomSheet?.peekHeight ?: 0)

        val markerManager = MarkerManager(map)
        sectorMarkers = markerManager.newCollection()
        sectorMarkers.setOnMarkerClickListener(this)
        clusterManager = ClusterManager(applicationContext, map, markerManager)
        clusterManager.renderer = CustomRenderer(binding.vm, applicationContext, map, clusterManager)
        clusterManager.setOnClusterItemClickListener(this)
        clusterManager.setOnClusterClickListener {
            map.animate(it)
            true
        }
        map.setOnMarkerClickListener(markerManager)
        map.setOnCameraIdleListener(clusterManager)

        observeViewModel()
    }

    /**
     * All ClusterItems are CRAGS (As they cluster when there are too many to display sensibly
     * */
    override fun onClusterItemClick(clusterItem: CragClusterItem): Boolean {
        binding.vm?.onClusterItemClick(clusterItem.location.id)
        return true
    }

    /**
     * All Markers are SECTORS
     * */
    override fun onMarkerClick(marker: Marker): Boolean {
        binding.vm?.onMapMarkerClick((marker.tag as Location).id)
        return true
    }

    override fun onLocationSubmitted(locationType: LocationType, submittedLocationId: String) {
        binding.vm?.onLocationSubmitted(submittedLocationId, locationType)
        hideKeyboard(this)
    }

    private fun observeViewModel() {
        binding.vm?.viewEvents?.observe(this, Observer {
            when (it) {
                ShowDevMenu -> drawerWrapper.showDevMenu()
                is ReportTopo -> showReportTopoDialog(it.topo)
                is ShowXClicksToDevMenuToast -> showToast(getString(R.string.clicks_to_dev_menu, it.clicks))
                NavigateToSignIn -> startSignInActivity()
            }
        })

        binding.vm?.userSignedIn?.observe(this, Observer {
            Timber.d("userSignedIn changed, swapping drawer button")
            drawerWrapper.toggleSignedIn(it)
        })

        binding.vm?.mapType?.observe(this, Observer {
            Timber.d("Map type changed, updating map")
            if (GoogleMap.MAP_TYPE_NORMAL == it) {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
            } else {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
        })

        binding.vm?.bottomSheetRequestedState?.observe(this, Observer {
            Timber.d("bottomSheetRequestedState changed to ${stateFromInt(it)}, changing state")
            bottomSheet?.state = it
        })

        binding.vm?.bottomSheetState?.observe(this, Observer {
            Timber.d("bottomSheetState changed to ${stateFromInt(it)}")
            it?.let { newState ->
                when (newState) {
                    STATE_EXPANDED -> {
                        setMapBottomPadding(bottom_sheet.measuredHeight)
                        binding.vm?.onBottomSheetExpand()
                    }
                    STATE_COLLAPSED -> setMapBottomPadding(bottomSheet?.peekHeight ?: 0)
                }
                Unit
            }
        })

        binding.vm?.crags?.observe(this, Observer { crags: List<Location>? ->
            Timber.d("crags changed, new crags: %s", crags?.map { it.name })
        })

        binding.vm?.cragClusterItems?.observe(this, Observer { clusterItems ->
            Timber.d("cragClusterItems changed")
            //TODO Do diff insteaf of re-initisliaing
            clusterManager.clearItems()
            clusterManager.addItems(clusterItems)
            clusterManager.cluster()
        })

        binding.vm?.sectors?.observe(this, Observer { sectors: List<Location>? ->
            Timber.d("sectors changed, new sectors: %s", sectors?.map { it.name })
            refreshSectorsForCrag(sectors)
        })

        binding.vm?.mapLatLngBounds?.observe(this, Observer {
            Timber.d("mapLatLngBounds changed, animating map pan")
            if (it.isNotEmpty()) {
                map.animate(getBoundsForLatLngs(it)) {
                    Timber.d("Map animation finished (callback), we can do expensive things now")
                    binding.vm?.onMapAnimationFinished()
                }
            }
        })

        binding.vm?.mapMode?.observe(this, Observer {
            when (it) {
                DEFAULT_MODE -> {
                    Timber.d("MapMode changed to DEFAULT_MODE")
                    fabStyle(R.drawable.ic_add_crag, R.color.fab_new_crag)
                    replaceFragment(welcomeFragment, R.id.bottom_sheet)
                }
                CRAG_MODE -> {
                    Timber.d("MapMode changed to CRAG_MODE")
                    fabStyle(R.drawable.ic_add_sector, R.color.fab_new_sector)
                    refreshSectorsForCrag(binding.vm?.sectors?.value)
                    replaceFragment(bottomSheetFragment, R.id.bottom_sheet)
                }
                SECTOR_MODE, TOPO_MODE -> {
                    Timber.d("MapMode changed to SECTOR_MODE || TOPO MODE")
                    fabStyle(R.drawable.ic_add_topo, R.color.fab_new_topo)
                    replaceFragment(bottomSheetFragment, R.id.bottom_sheet)
                }
                SUBMIT_CRAG_MODE -> {
                    Timber.d("MapMode changed to SUBMIT_CRAG_MODE")
                    replaceFragment(submitCragFragment, R.id.bottom_sheet)
                }
                SUBMIT_SECTOR_MODE -> {
                    Timber.d("MapMode changed to SUBMIT_SECTOR_MODE")
                    submitSectorFragment.parentId = binding.vm?.selectedLocation?.value?.id
                    replaceFragment(submitSectorFragment, R.id.bottom_sheet)
                    refreshSectorsForCrag(binding.vm?.sectors?.value)
                }
                SUBMIT_TOPO_MODE -> {
                    Timber.d("MapMode changed to SUBMIT_TOPO_MODE")
                    binding.vm?.selectedLocation?.value?.id?.let { sectorId ->
                        val intent = Intent(this, SubmitActivity::class.java)
                        intent.putExtra(EXTRA_SECTOR_ID, sectorId)
                        startActivityForResult(intent, ACTIVITY_RESULT_SUBMIT)
                    }
                }
            }
            when (it) {
                DEFAULT_MODE, CRAG_MODE, SECTOR_MODE, TOPO_MODE -> {
                    val bottomSheetOpened = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                            .getBoolean(PREF_BOTTOM_SHEET_OPENED, false)
                    if (!bottomSheetOpened) bounceBottomSheet()
                }
                SUBMIT_CRAG_MODE, SUBMIT_SECTOR_MODE, SUBMIT_TOPO_MODE -> {

                }
            }
        })
    }

    private fun refreshSectorsForCrag(sectors: List<Location>?) {
        // Doing a diff here wouldn't make much DIFFerence
        // as we normally have a totally new set of sectors when this is called
        sectorMarkers.clear()
        val iconHelper = IconHelper(this)
        sectors?.forEach {
            val iconStyle = if (binding.vm?.mapMode?.value == SUBMIT_SECTOR_MODE) Icon.SECTOR_DIMMED else Icon.SECTOR
            val icon = iconHelper.getIcon(it.name, iconStyle) //30ms
            val marker = sectorMarkers.addMarker(MarkerOptions() //15ms
                    .icon(BitmapDescriptorFactory.fromBitmap(icon))
                    .position(it.latlng)
                    .draggable(false))
            marker.tag = it
        }
    }

    private fun showToast(text: String) {
        toast.setText(text)
        toast.show()
    }

    private fun showReportTopoDialog(topo: Topo) {
        val editText = EditText(this)
        AlertDialog.Builder(this)
                .setTitle(R.string.report_topo_title)
                .setMessage(R.string.report_topo_text)
                .setView(editText)
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("Submit") { _, _ ->
                    binding.vm?.onReportTopo(editText.text.toString(), topo)
                }
                .show()
    }

    private fun initialiseFloatingSearchBar() {
        floating_search_view.attachNavigationDrawerToMenuButton(drawerWrapper.drawer.drawerLayout)
        floating_search_view.setOnFocusChangeListener(object : FloatingSearchView.OnFocusChangeListener {
            override fun onFocusCleared() {
                binding.vm?.onSearchBarUnfocus()
            }

            override fun onFocus() {
                binding.vm?.onSearchBarFocus()
            }
        })

        floating_search_view.setOnSearchListener(object : FloatingSearchView.OnSearchListener {
            override fun onSuggestionClicked(searchSuggestion: SearchSuggestion) {
                Timber.i("Search suggestion clicked: ${searchSuggestion.body}")
                if (searchSuggestion is SearchSuggestionItem) {
                    floating_search_view.clearQuery()
                    floating_search_view.clearSuggestions()
                    floating_search_view.clearSearchFocus()

                    binding.vm?.onSearchSuggestionClicked(searchSuggestion)
                }
            }

            override fun onSearchAction(currentQuery: String) {
                // no op
            }
        })

        // React to search term changing
        floating_search_view.setOnQueryChangeListener { _, newQuery ->
            binding.vm?.searchQuery?.value = newQuery
        }

        // React to new search results
        binding.vm?.searchResults?.observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                floating_search_view.swapSuggestions(it) // TODO Don't swap, do diff
            } else {
                floating_search_view.clearSuggestions()
            }
        })

        // Set appropriate icons for search item
        floating_search_view.setOnBindSuggestionCallback { _, leftIcon, _, item, _ ->
            if (item is SearchSuggestionItem) {
                when (item.type) {
                    CRAG -> Picasso.get().load(R.drawable.location_marker_crag_no_text).into(leftIcon)
                    SECTOR -> Picasso.get().load(R.drawable.location_marker_sector_no_text).into(leftIcon)
                    TOPO -> leftIcon.setImageResource(R.drawable.ic_topo)
                    ROUTE_BOULDER -> Picasso.get().load(R.drawable.ic_boulder).into(leftIcon)
                    ROUTE_TRAD -> Picasso.get().load(R.drawable.ic_cam).into(leftIcon)
                    ROUTE_SPORT -> Picasso.get().load(R.drawable.ic_quick_draw).into(leftIcon)
                    else -> Timber.e("Route doesn't have type")
                }
            }
        }
    }

    private fun initialiseBottomSheet() {
        bottomSheet = BottomSheetBehavior.from(bottom_sheet)
        binding.vm?.bottomSheetState?.value = bottomSheet?.state
        bottomSheet?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheetView: View, newState: Int) {
                binding.vm?.onBottomSheetStateChanged(newState)
            }

            override fun onSlide(bottomSheetView: View, slideOffset: Float) {
                val peek = bottomSheet?.peekHeight ?: 0
                if (slideOffset >= 0) {
                    setMapBottomPadding(round((bottom_sheet.height - peek) * slideOffset + peek))
                    fab.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start()
                } else {
                    setMapBottomPadding(round(peek - (peek * -slideOffset)))
                    fab.animate().scaleX(1 + slideOffset).scaleY(1 + slideOffset).setDuration(0).start()
                }
            }
        })
    }

    private fun bounceBottomSheet() {
        val animation = TranslateAnimation(0f, 0f, -100f, 0f)
        animation.duration = 1000
        animation.interpolator = BounceInterpolator()
        bottom_sheet.startAnimation(animation)
    }

    private fun setMapBottomPadding(padding: Int) {
        if (::map.isInitialized) map.setPadding(/*Left*/ 0, MAP_PADDING_TOP, /*Right*/ 0, /*Bottom*/ padding)
    }

    private fun fabStyle(iconId: Int, colourId: Int) {
        fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, colourId))
        fab.setImageResource(iconId)
    }

    private fun startSignInActivity() {
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(listOf(
                        AuthUI.IdpConfig.EmailBuilder().build()
                ))
                .build(),
                ACTIVITY_RESULT_SIGNIN
        )
    }

    fun doTutorial(view: View) {
        tutorialManager.launch(this, binding.vm)
    }

    fun continueTutorial(view: View) {
        tutorialManager.next()
    }

    fun exitTutorial(view: View) {
        tutorialManager.exit()
    }
}