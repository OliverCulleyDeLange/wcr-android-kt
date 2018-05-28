package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
import android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.imageloader.drawerImageLoader
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler
import com.amazonaws.mobile.auth.core.IdentityManager
import com.amazonaws.mobile.auth.core.IdentityProvider
import com.amazonaws.mobile.auth.core.SignInStateChangeListener
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration
import com.amazonaws.mobile.auth.ui.SignInActivity
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.config.AWSConfiguration
import com.arlib.floatingsearchview.FloatingSearchView
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.MarkerManager
import com.google.maps.android.clustering.ClusterManager
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.squareup.picasso.Picasso
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_maps.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.ActivityMapsBinding
import uk.co.oliverdelange.wcr_android_kt.map.*
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.model.SearchResultType.*
import uk.co.oliverdelange.wcr_android_kt.model.SearchSuggestionItem
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitActivity
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitLocationFragment
import uk.co.oliverdelange.wcr_android_kt.util.replaceFragment
import java.lang.Math.round
import javax.inject.Inject

const val DEFAULT_ZOOM = 6f
const val CRAG_ZOOM = 14f
const val MAP_ANIMATION_DURATION = 400
const val MAP_PADDING_TOP = 150

const val EXTRA_SECTOR_ID = "EXTRA_SECTOR_ID"
const val REQUEST_SUBMIT = 999

const val MENU_SIGN_IN_ID = 1L
const val MENU_SIGN_OUT_ID = 2L

const val BOTTOM_SHEET_OPENED = "BOTTOM_SHEET_OPENED"

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
    private var bottomSheet: BottomSheetBehavior<LinearLayout>? = null

    private lateinit var clusterManager: ClusterManager<CragClusterItem>
    private lateinit var sectorMarkers: MarkerManager.Collection
    private lateinit var slidingDrawer: Drawer
    private lateinit var signInDrawerItem: PrimaryDrawerItem
    private lateinit var signOutDrawerItem: PrimaryDrawerItem
    internal lateinit var binding: ActivityMapsBinding

    private var credentialsProvider: AWSCredentialsProvider? = null
    private var awsConfiguration: AWSConfiguration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)
        binding.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)
        binding.vm = viewModel
        binding.floatingSearchView.setQueryTextSize(14)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initialiseDrawer()
        initialiseFloatingSearchBar()
        initialiseBottomSheet()

        AWSMobileClient.getInstance().initialize(this) {
            Timber.d("AWSMobileClient is initialized")
            credentialsProvider = AWSMobileClient.getInstance().credentialsProvider
            awsConfiguration = AWSMobileClient.getInstance().configuration

//            IdentityManager.getDefaultIdentityManager().getUserID(object : IdentityHandler {
//                override fun handleError(exception: Exception?) {
//                    Timber.e(exception, "Retrieving identity: ${exception?.message}")
//                }
//
//                override fun onIdentityId(identityId: String?) {
//                    Timber.d("Identity = $identityId")
//                    val cachedIdentityId = IdentityManager.getDefaultIdentityManager().cachedUserID
//                    // Do something with the identity here
//                }
//            })

//            val identityManager = IdentityManager.getDefaultIdentityManager()
//            if (identityManager.isUserSignedIn) {


            IdentityManager.getDefaultIdentityManager().addSignInStateChangeListener(
                    object : SignInStateChangeListener {
                        override fun onUserSignedIn() {
                            binding.vm?.userSignedIn?.postValue(true)
                        }

                        override fun onUserSignedOut() {
                            binding.vm?.userSignedIn?.postValue(false)
                        }
                    }
            )
        }.execute()
    }

    override fun onBackPressed() {
        binding.vm?.back()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SUBMIT -> {
                binding.vm?.mapMode?.value = SECTOR_MODE
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("User submitted topo")
                    // TODO expand bottom sheet?
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapBottomPadding(bottomSheet?.peekHeight ?: 0)

        val markerManager = MarkerManager(map)
        sectorMarkers = markerManager.newCollection()
        sectorMarkers.setOnMarkerClickListener(this)
        clusterManager = ClusterManager(applicationContext, map, markerManager)
        clusterManager.renderer = CustomRenderer(binding.vm, applicationContext, map, clusterManager)
        clusterManager.setOnClusterItemClickListener(this)
        clusterManager.setOnClusterClickListener {
            val bounds: LatLngBounds = LatLngUtil.getBoundsForLatLngs(it.items.map { it.position })
            map.animate(bounds)
            true
        }
        map.setOnMarkerClickListener(markerManager)
        map.setOnCameraIdleListener(clusterManager)

        observeViewModel()
    }

    override fun onClusterItemClick(clusterItem: CragClusterItem): Boolean {
        binding.vm?.selectCrag(clusterItem.location.id)
        return true
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        binding.vm?.selectSector((marker.tag as Location).id)
        return true
    }

    override fun onLocationSubmitted(locationType: LocationType, submittedLocationId: Long) {
        if (locationType == LocationType.CRAG) {
            binding.vm?.mapMode?.value = CRAG_MODE
        } else {
            binding.vm?.mapMode?.value = SECTOR_MODE
        }
        binding.vm?.selectedLocationId?.value = submittedLocationId
    }

    private fun observeViewModel() {
        binding.vm?.userSignedIn?.observe(this, Observer {
            slidingDrawer.removeItem(MENU_SIGN_IN_ID)
            slidingDrawer.removeItem(MENU_SIGN_OUT_ID)
            if (it == true) {
                slidingDrawer.setItemAtPosition(signOutDrawerItem, 1)
            } else {
                slidingDrawer.setItemAtPosition(signInDrawerItem, 1)
            }
        })

        binding.vm?.mapType?.observe(this, Observer {
            if (GoogleMap.MAP_TYPE_NORMAL == it) {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
            } else {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
        })

        binding.vm?.bottomSheetState?.observe(this, Observer {
            it?.let {
                bottomSheet?.state = it
            }
        })

        binding.vm?.crags?.observe(this, Observer { crags: List<Location>? ->
            Timber.d("New crag location to display. Locations: %s", crags)
            refreshCragClusterItems()
            crags?.map { location -> location.latlng }?.let { if (it.isNotEmpty()) map.animate(LatLngUtil.getBoundsForLatLngs(it)) }
        })

        binding.vm?.sectors?.observe(this, Observer { sectors: List<Location>? ->
            Timber.d("New sector location to display. Locations: %s", sectors)
            refreshSectorsForCrag(sectors)
            if (binding.vm?.selectedLocation?.value?.type == LocationType.CRAG) {
                val locations = sectors?.plus(binding.vm!!.selectedLocation.value!!)
                locations?.map { location -> location.latlng }?.let { map.animate(LatLngUtil.getBoundsForLatLngs(it)) }
            }
        })

        binding.vm?.mapMode?.observe(this, Observer {
            when (it) {
                DEFAULT_MODE -> {
                    fabStyle(R.drawable.ic_add_crag, R.color.fab_new_crag)
                    refreshCragClusterItems()
                    val latlngs = binding.vm?.crags?.value?.map { it.latlng }
                    latlngs?.let { map.animate(LatLngUtil.getBoundsForLatLngs(it)) }
                    replaceFragment(welcomeFragment, R.id.bottom_sheet)
                }
                CRAG_MODE -> {
                    fabStyle(R.drawable.ic_add_sector, R.color.fab_new_sector)
                    refreshCragClusterItems()
                    refreshSectorsForCrag(binding.vm?.sectors?.value)
                    replaceFragment(bottomSheetFragment, R.id.bottom_sheet)
                }
                SECTOR_MODE, TOPO_MODE -> {
                    fabStyle(R.drawable.ic_add_topo, R.color.fab_new_topo)
                    refreshCragClusterItems()
                    replaceFragment(bottomSheetFragment, R.id.bottom_sheet)
                }
                SUBMIT_CRAG_MODE -> {
                    replaceFragment(submitCragFragment, R.id.bottom_sheet)
                    refreshCragClusterItems()
                }
                SUBMIT_SECTOR_MODE -> {
                    submitSectorFragment.parentId = binding.vm?.selectedLocationId?.value
                    replaceFragment(submitSectorFragment, R.id.bottom_sheet)
                    refreshCragClusterItems()
                    refreshSectorsForCrag(binding.vm?.sectors?.value)
                }
                SUBMIT_TOPO_MODE -> {
                    binding.vm?.selectedLocation?.value?.id?.let {
                        val intent = Intent(this, SubmitActivity::class.java)
                        intent.putExtra(EXTRA_SECTOR_ID, it)
                        startActivityForResult(intent, REQUEST_SUBMIT)
                    }
                }
            }
            when (it) {
                DEFAULT_MODE, CRAG_MODE, SECTOR_MODE, TOPO_MODE -> {
                    binding.vm?.showFab?.set(true)
                    val bottomSheetOpened = getPreferences(Context.MODE_PRIVATE)
                            .getBoolean(BOTTOM_SHEET_OPENED, false)
                    if (!bottomSheetOpened) bounceBottomSheet()
                }
                SUBMIT_CRAG_MODE, SUBMIT_SECTOR_MODE, SUBMIT_TOPO_MODE -> {
                    binding.vm?.showFab?.set(false)
                }
            }
        })
    }

    private fun refreshSectorsForCrag(sectors: List<Location>?) {
        sectorMarkers.clear()
        sectors?.forEach {
            val iconStyle = if (binding.vm?.mapMode?.value == SUBMIT_SECTOR_MODE) Icon.SECTOR_DIMMED else Icon.SECTOR
            val icon = IconHelper(this).getIcon(it.name, iconStyle)
            val marker = sectorMarkers.addMarker(MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(icon))
                    .position(it.latlng)
                    .draggable(false))
            marker.tag = it
        }
    }

    private fun refreshCragClusterItems() {
        val cragClusterItems = binding.vm?.crags?.value?.map { CragClusterItem(it) }
        cragClusterItems?.let {
            clusterManager.clearItems()
            clusterManager.addItems(cragClusterItems)
            clusterManager.cluster()
        }
    }

    private fun initialiseDrawer() {
        slidingDrawer = drawer {
            selectedItem = -1
            accountHeader {
                background = R.drawable.nature
            }
            signInDrawerItem = primaryItem(R.string.menu_signin) {
                identifier = MENU_SIGN_IN_ID
                iicon = GoogleMaterial.Icon.gmd_account_circle
                selectable = false
                onClick { _ ->
                    val identityManager = IdentityManager.getDefaultIdentityManager()
                    identityManager.login(this@MapsActivity, object : DefaultSignInResultHandler() {
                        override fun onSuccess(activity: Activity, identityProvider: IdentityProvider?) {
                            if (identityProvider != null) {
                                Timber.d("Sign-in succeeded. The identity provider name is available here using: ${identityProvider.displayName}")
                            }
                        }

                        override fun onCancel(activity: Activity): Boolean {
                            // Return false to prevent the user from dismissing the sign in screen by pressing back button.
                            // Return true to allow this.
                            return true
                        }
                    })

                    SignInActivity.startSignInActivity(this@MapsActivity, AuthUIConfiguration.Builder()
                            .userPools(true)
                            .canCancel(true)
                            .logoResId(R.mipmap.ic_launcher)
                            .build())


                    false
                }
            }
            signOutDrawerItem = primaryItem(R.string.menu_signout) {
                identifier = MENU_SIGN_OUT_ID
                iicon = GoogleMaterial.Icon.gmd_exit_to_app
                selectable = false
                onClick { _ ->
                    AlertDialog.Builder(this@MapsActivity)
                            .setMessage(R.string.signout_prompt)
                            .setNegativeButton("No") { _, _ -> }
                            .setPositiveButton("Yes") { _, _ ->
                                IdentityManager.getDefaultIdentityManager().signOut()
                            }
                            .show()
                    false
                }
            }
            primaryItem(R.string.menu_about) {
                iicon = GoogleMaterial.Icon.gmd_help
                selectable = false
                onClick { _ ->
                    LibsBuilder()
                            .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                            .withAboutAppName("We Climb Rocks")
                            .withLicenseShown(true)
                            .withActivityTitle("About")
                            .withAboutIconShown(true)
                            .withAboutVersionShown(true)
                            .withAboutDescription("We Climb Rocks is a platform for sharing climbing topos, and easily locating routes." +
                                    "<br /><b>For support, email <a href='mailto:weclimbrocks@oliverdelange.co.uk'>weclimbrocks@oliverdelange.co.uk</a></b>" +
                                    "<br /><br />" +
                                    "Below is a list of Open Source libraries used in this app.")
                            .start(this@MapsActivity)
                    false
                }
            }
        }

        drawerImageLoader {
            placeholder { ctx, _ ->
                DrawerUIUtils.getPlaceHolder(ctx)
            }
            set { imageView, uri, placeholder, _ ->
                Picasso.with(imageView.context).load(uri).placeholder(placeholder).into(imageView)
            }
            cancel { imageView ->
                Picasso.with(imageView.context).cancelRequest(imageView)
            }
        }
    }

    private fun initialiseFloatingSearchBar() {
        floating_search_view.attachNavigationDrawerToMenuButton(slidingDrawer.drawerLayout)
        floating_search_view.setOnFocusChangeListener(object : FloatingSearchView.OnFocusChangeListener {
            override fun onFocusCleared() {
                bottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            override fun onFocus() {
                bottomSheet?.state = BottomSheetBehavior.STATE_HIDDEN
            }
        })

        floating_search_view.setOnSearchListener(object : FloatingSearchView.OnSearchListener {
            override fun onSuggestionClicked(searchSuggestion: SearchSuggestion) {
                Timber.i("Search suggestion clicked: $searchSuggestion")
                if (searchSuggestion is SearchSuggestionItem) {
                    floating_search_view.clearQuery()
                    floating_search_view.clearSuggestions()
                    floating_search_view.clearSearchFocus()

                    when (searchSuggestion.type) {
                        CRAG -> binding.vm?.selectCrag(searchSuggestion.id)
                        SECTOR -> binding.vm?.selectSector(searchSuggestion.id)
                        TOPO -> binding.vm?.selectTopo(searchSuggestion.id)
                        ROUTE, ROUTE_BOULDER, ROUTE_TRAD, ROUTE_SPORT -> {
                            binding.vm?.selectRoute(searchSuggestion.id)
                        }
                    }
                }
            }

            override fun onSearchAction(currentQuery: String) {
                // no op
            }
        })

        // React to search term changing
        floating_search_view.setOnQueryChangeListener({ _, newQuery ->
            binding.vm?.searchQuery?.value = newQuery
        })

        // React to new search results
        binding.vm?.searchResults?.observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                floating_search_view.swapSuggestions(it) // TODO Don't swap, do diff
            } else {
                floating_search_view.clearSuggestions()
            }
        })

        // Set appropriate icons for search item
        floating_search_view.setOnBindSuggestionCallback(SearchSuggestionsAdapter.OnBindSuggestionCallback { _, leftIcon, _, item, _ ->
            if (item is SearchSuggestionItem) {
                when (item.type) {
                    CRAG -> Picasso.with(this).load(R.drawable.location_marker_crag_no_text).into(leftIcon)
                    SECTOR -> Picasso.with(this).load(R.drawable.location_marker_sector_no_text).into(leftIcon)
                    TOPO -> leftIcon.setImageResource(R.drawable.ic_topo)
                    ROUTE_BOULDER -> Picasso.with(this).load(R.drawable.ic_boulder).into(leftIcon)
                    ROUTE_TRAD -> Picasso.with(this).load(R.drawable.ic_cam).into(leftIcon)
                    ROUTE_SPORT -> Picasso.with(this).load(R.drawable.ic_quick_draw).into(leftIcon)
                }
            }
        })
    }

    private fun initialiseBottomSheet() {
        bottomSheet = BottomSheetBehavior.from(bottom_sheet)
        binding.vm?.bottomSheetState?.value = bottomSheet?.state
        bottomSheet?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheetView: View, newState: Int) {
                when (newState) {
                    STATE_EXPANDED -> {
                        setMapBottomPadding(bottom_sheet.measuredHeight)
                        with(getPreferences(Context.MODE_PRIVATE).edit()) {
                            putBoolean(BOTTOM_SHEET_OPENED, true)
                            apply()
                        }
                    }
                    STATE_COLLAPSED -> {
                        setMapBottomPadding(bottomSheet?.peekHeight ?: 0)
                    }
                }
                binding.vm?.bottomSheetState?.value = newState
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
        fab.backgroundTintList = ColorStateList.valueOf(resources.getColor(colourId))
        fab.setImageResource(iconId)
    }
}