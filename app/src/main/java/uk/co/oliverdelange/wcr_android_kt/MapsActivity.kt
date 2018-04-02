package uk.co.oliverdelange.wcr_android_kt

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.LinearLayout
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.imageloader.drawerImageLoader
import com.arlib.floatingsearchview.FloatingSearchView
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.MarkerManager
import com.google.maps.android.clustering.ClusterManager
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.squareup.picasso.Picasso
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.databinding.ActivityMapsBinding
import uk.co.oliverdelange.wcr_android_kt.map.CragClusterItem
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapViewModel
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitFragment
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitViewModel
import uk.co.oliverdelange.wcr_android_kt.util.removeFragment
import uk.co.oliverdelange.wcr_android_kt.util.replaceFragment
import java.lang.Math.round
import javax.inject.Inject

const val DEFAULT_ZOOM = 6f
const val CRAG_ZOOM = 14f
const val MAP_ANIMATION_DURATION = 400
const val MAP_PADDING_TOP = 150

class MapsActivity : AppCompatActivity(), HasSupportFragmentInjector, OnMapReadyCallback, SubmitFragment.OnCompleteListener {
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun supportFragmentInjector(): DispatchingAndroidInjector<Fragment> {
        return dispatchingAndroidInjector
    }

    private var fragmentToRemove: Fragment? = null
    private val submitFragment = SubmitFragment.newInstance()

    internal lateinit var map: GoogleMap
    internal val defaultLatLng = LatLng(52.0, -2.0)
    internal lateinit var bottomSheet: BottomSheetBehavior<LinearLayout>

    private lateinit var clusterManager: ClusterManager<CragClusterItem>

    private lateinit var slidingDrawer: Drawer
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)
        binding.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)
        binding.vm = viewModel

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initialiseDrawer()
        initialiseFloatingSearchBar()
        initialiseBottomSheet()
    }

    override fun onBackPressed() {
        when (binding.vm?.mapMode?.value) {
            SUBMIT_CRAG -> {
                binding.vm?.mapMode?.value = DEFAULT
                if (bottomSheet.state == STATE_EXPANDED) fragmentToRemove = submitFragment
                else removeFragment(submitFragment)
            }
            SUBMIT_SECTOR -> binding.vm?.mapMode?.value = CRAG
            SUBMIT_TOPO -> binding.vm?.mapMode?.value = SECTOR
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapBottomPadding(bottom_sheet_peek.height)

        // For creating marker icons with text on the fly
//        val iconHelper = IconHelper(applicationContext)
        // To handle separation between clustered markers (crags) and climb markers
        val markerManager = MarkerManager(map)
        // Handles clustering and data fetch on map idle
        clusterManager = ClusterManager(applicationContext, map, markerManager)
        // Allows custom icons for cluster items
//        val customRenderer = CustomRenderer(applicationContext, map, clusterManager)

        // Set some listeners
//        clusterManager.setRenderer(customRenderer)
//        clusterManager.setOnClusterItemClickListener(this)

        map.setOnMarkerClickListener(markerManager)
        map.setOnCameraIdleListener(clusterManager)

        observeViewModel()
        map.moveCamera(newLatLngZoom(defaultLatLng, DEFAULT_ZOOM))
    }

    private fun observeViewModel() {
        binding.vm?.mapType?.observe(this, Observer {
            if (GoogleMap.MAP_TYPE_NORMAL == it) {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
            } else {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
        })

        binding.vm?.mapMode?.observe(this, Observer {
            when (it) {
                DEFAULT -> {
                    fabStyle(R.drawable.add_crag_button, R.color.fab_new_crag)
                    binding.vm?.showFab?.set(true)
                    bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                CRAG -> {
                    fabStyle(R.drawable.add_sector_button, R.color.fab_new_sector)
                }
                SECTOR -> {
                    fabStyle(R.drawable.add_topo_button, R.color.fab_new_topo)
                }
                TOPO -> {
                    bottomSheet.state = STATE_EXPANDED
                }
                SUBMIT_CRAG -> {
                    replaceFragment(submitFragment, R.id.bottom_sheet_content_container)
                }
                SUBMIT_SECTOR -> {
                }
                SUBMIT_TOPO -> {
                }
            }
            when (it) {
                DEFAULT, CRAG, SECTOR, TOPO -> {
                    binding.vm?.showFab?.set(true)
                }
                SUBMIT_CRAG, SUBMIT_SECTOR, SUBMIT_TOPO -> {
                    binding.vm?.showFab?.set(false)
                }
            }
        })

        binding.vm?.crags?.observe(this, Observer { locations: List<Location>? ->
            Timber.d("New crag location to display. Locations: %s", locations)
            clusterManager.clearItems()
            clusterManager.addItems(locations?.map { CragClusterItem(it) })
            clusterManager.cluster()
        })
    }

    private fun initialiseDrawer() {
        slidingDrawer = drawer {
            accountHeader {
                background = R.drawable.nature
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
                            .start(applicationContext)
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
            override fun onFocusCleared() {}
            override fun onFocus() {
                bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })
    }

    private var doAfterBottomSheetExpanded: () -> Unit = {}

    private fun initialiseBottomSheet() {
        bottomSheet = BottomSheetBehavior.from(bottom_sheet)

        bottomSheet.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    STATE_EXPANDED -> {
                        setMapBottomPadding(bottom_sheet_content_container.measuredHeight + bottom_sheet_peek.height)
                        doAfterBottomSheetExpanded.invoke(); doAfterBottomSheetExpanded = {}
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        setMapBottomPadding(bottom_sheet_peek.height)
                        // Remove the fragment after the bottom sheet has settled
                        fragmentToRemove?.let { fragment ->
                            removeFragment(fragment)
                            fragmentToRemove = null
                        }
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setMapBottomPadding(round(bottom_sheet_content_container.height * slideOffset + bottom_sheet_peek.height))
            }
        })
    }

    private fun fabStyle(iconId: Int, colourId: Int) {
        fab.backgroundTintList = ColorStateList.valueOf(resources.getColor(colourId))
        fab.setImageResource(iconId)
    }

    private fun setMapBottomPadding(padding: Int) {
        if (::map.isInitialized) map.setPadding(/*Left*/ 0, MAP_PADDING_TOP, /*Right*/ 0, /*Bottom*/ padding)
    }

    override fun onSubmitFragmentReady(vm: SubmitViewModel?) {
        //TODO Feels hacky - Better way to execute code when BottomSheet state is expanded?
        doAfterBottomSheetExpanded = {
            val mapCenter = map.projection.visibleRegion.latLngBounds.center
            map.addMarker(MarkerOptions()
                    .position(mapCenter)
                    .draggable(true)
            )
            vm?.cragLatLng?.value = mapCenter

            map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    vm?.cragLatLng?.value = marker.position
                }
            })
        }

        bottomSheet.state = STATE_EXPANDED
    }


}
