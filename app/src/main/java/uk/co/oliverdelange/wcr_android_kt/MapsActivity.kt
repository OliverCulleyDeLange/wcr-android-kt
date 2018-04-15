package uk.co.oliverdelange.wcr_android_kt

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
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
import com.google.android.gms.maps.model.*
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
import uk.co.oliverdelange.wcr_android_kt.map.*
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapViewModel
import uk.co.oliverdelange.wcr_android_kt.ui.map.ToposFragment
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitLocationFragment
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitTopoFragment
import uk.co.oliverdelange.wcr_android_kt.util.replaceFragment
import java.lang.Math.round
import javax.inject.Inject

const val DEFAULT_ZOOM = 6f
const val CRAG_ZOOM = 14f
const val MAP_ANIMATION_DURATION = 400
const val MAP_PADDING_TOP = 150

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
    private val submitTopoFragment = SubmitTopoFragment.newTopoSubmission()
    private val viewToposFragment = ToposFragment.newToposFragment()

    internal lateinit var map: GoogleMap
    private val defaultLatLng = LatLng(54.056, -3.155)
    internal lateinit var bottomSheet: BottomSheetBehavior<LinearLayout>

    private lateinit var clusterManager: ClusterManager<CragClusterItem>
    private lateinit var sectorMarkers: MarkerManager.Collection
    private lateinit var slidingDrawer: Drawer
    internal lateinit var binding: ActivityMapsBinding

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
        binding.vm?.back()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapBottomPadding(bottom_sheet_peek.height)

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
        map.moveCamera(newLatLngZoom(defaultLatLng, DEFAULT_ZOOM))
    }

    override fun onClusterItemClick(clusterItem: CragClusterItem): Boolean {
        binding.vm?.onCragClick(clusterItem.location)
        return true
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        binding.vm?.onSectorClick(marker.tag as Location)
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
        binding.vm?.mapType?.observe(this, Observer {
            if (GoogleMap.MAP_TYPE_NORMAL == it) {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
            } else {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
        })

        binding.vm?.bottomSheetState?.observe(this, Observer {
            it?.let {
                bottomSheet.state = it
            }
        })

        binding.vm?.crags?.observe(this, Observer { crags: List<Location>? ->
            Timber.d("New crag location to display. Locations: %s", crags)
            refreshCragClusterItems()
        })

        binding.vm?.sectors?.observe(this, Observer { sectors: List<Location>? ->
            Timber.d("New sector location to display. Locations: %s", sectors)
            refreshSectorsForCrag(sectors)
            val locations = sectors?.plus(binding.vm!!.selectedLocation.value!!)
            locations?.map { location -> location.latlng }?.let { map.animate(LatLngUtil.getBoundsForLatLngs(it)) }
        })

        binding.vm?.mapMode?.observe(this, Observer {
            when (it) {
                DEFAULT_MODE -> {
                    fabStyle(R.drawable.add_crag_button, R.color.fab_new_crag)
                    refreshCragClusterItems()
                    map.animateCamera(newLatLngZoom(defaultLatLng, DEFAULT_ZOOM))
                    replaceFragment(viewToposFragment, R.id.bottom_sheet_content_container)
                }
                CRAG_MODE -> {
                    fabStyle(R.drawable.add_sector_button, R.color.fab_new_sector)
                    refreshCragClusterItems()
                    replaceFragment(viewToposFragment, R.id.bottom_sheet_content_container)
                }
                SECTOR_MODE -> {
                    fabStyle(R.drawable.add_topo_button, R.color.fab_new_topo)
                    replaceFragment(viewToposFragment, R.id.bottom_sheet_content_container)
                }
                TOPO_MODE -> {
                }
                SUBMIT_CRAG_MODE -> {
                    replaceFragment(submitCragFragment, R.id.bottom_sheet_content_container)
                    refreshCragClusterItems()
                }
                SUBMIT_SECTOR_MODE -> {
                    submitSectorFragment.parentId = binding.vm?.selectedLocationId?.value
                    replaceFragment(submitSectorFragment, R.id.bottom_sheet_content_container)
                    refreshCragClusterItems()
                }
                SUBMIT_TOPO_MODE -> {
                    binding.vm?.selectedLocation?.value?.id?.let {
                        submitTopoFragment.sectorId = it
                        replaceFragment(submitTopoFragment, R.id.bottom_sheet_content_container)
                    }
                }
            }
            when (it) {
                DEFAULT_MODE, CRAG_MODE, SECTOR_MODE, TOPO_MODE -> {
                    binding.vm?.showFab?.set(true)
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
            val icon = IconHelper(this).getIcon(it.name, Icon.SECTOR)
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

    private fun initialiseBottomSheet() {
        bottomSheet = BottomSheetBehavior.from(bottom_sheet)

        bottomSheet.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    STATE_EXPANDED -> {
                        setMapBottomPadding(bottom_sheet_content_container.measuredHeight + bottom_sheet_peek.height)
                    }
                    STATE_COLLAPSED -> {
                        setMapBottomPadding(bottom_sheet_peek.height)
                    }
                }
                binding.vm?.bottomSheetState?.value = newState
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setMapBottomPadding(round(bottom_sheet_content_container.height * slideOffset + bottom_sheet_peek.height))
            }
        })
    }

    private fun setMapBottomPadding(padding: Int) {
        if (::map.isInitialized) map.setPadding(/*Left*/ 0, MAP_PADDING_TOP, /*Right*/ 0, /*Bottom*/ padding)
    }

    private fun fabStyle(iconId: Int, colourId: Int) {
        fab.backgroundTintList = ColorStateList.valueOf(resources.getColor(colourId))
        fab.setImageResource(iconId)
    }
}