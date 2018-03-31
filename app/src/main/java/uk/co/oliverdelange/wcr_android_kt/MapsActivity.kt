package uk.co.oliverdelange.wcr_android_kt

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.LinearLayout
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.imageloader.drawerImageLoader
import com.arlib.floatingsearchview.FloatingSearchView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import uk.co.oliverdelange.wcr_android_kt.databinding.ActivityMapsBinding
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapMode.*
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapViewModel
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitFragment
import uk.co.oliverdelange.wcr_android_kt.util.replaceFragment
import java.lang.Math.round


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var slidingDrawer: Drawer
    private lateinit var binding: ActivityMapsBinding
    private lateinit var bottomSheet: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)
        binding.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)
        binding.vm = viewModel

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initialiseDrawer()
        initialiseFloatingSearchBar()
        initialiseBottomSheet()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        binding.vm?.mapType?.observe(this, Observer {
            if (GoogleMap.MAP_TYPE_NORMAL == it) {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            } else {
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
        })

        binding.vm?.mapMode?.observe(this, Observer {
            when (it) {
                DEFAULT -> {
                    fabStyle(R.drawable.add_crag_button, R.color.fab_new_crag)
                }
                CRAG -> {
                    fabStyle(R.drawable.add_sector_button, R.color.fab_new_sector)
                }
                SECTOR -> {
                    fabStyle(R.drawable.add_topo_button, R.color.fab_new_topo)
                }
                TOPO -> {
                    bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                }
                SUBMIT_CRAG -> {
                    fab.animate().scaleX(0f).scaleY(0f).setDuration(2000).start()
                    bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                    replaceFragment(SubmitFragment.newInstance(), R.id.bottom_sheet_content_container)
                }
                SUBMIT_SECTOR -> {
                    bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                }
                SUBMIT_TOPO -> {
                    bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                }

            }
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

    private fun initialiseBottomSheet() {
        bottomSheet = BottomSheetBehavior.from(bottom_sheet)

        bottomSheet.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                mMap.setPadding(/*Left*/ 0, /*Top*/ 150, /*Right*/ 0, /*Bottom*/ if (slideOffset > 0) {
                    round(bottom_sheet_content_container.height * slideOffset + bottom_sheet_peek.height)
                } else {
                    round(bottom_sheet_peek.height - (bottom_sheet_peek.height * -slideOffset))
                })
            }
        })

    }

    private fun fabStyle(iconId: Int, colourId: Int) {
        fab.backgroundTintList = ColorStateList.valueOf(resources.getColor(colourId))
        fab.setImageResource(iconId)
    }
}
