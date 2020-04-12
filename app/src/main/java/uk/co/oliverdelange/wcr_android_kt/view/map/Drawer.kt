package uk.co.oliverdelange.wcr_android_kt.view.map

import android.app.AlertDialog
import android.widget.ImageView
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.badgeable.secondaryItem
import co.zsmb.materialdrawerkt.imageloader.drawerImageLoader
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.AuthUI
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.squareup.picasso.Picasso
import uk.co.oliverdelange.wcr_android_kt.BuildConfig
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.service.downloadSync
import uk.co.oliverdelange.wcr_android_kt.service.uploadSync
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel

const val DEV_MENU_CLICKS_REQUIRED = 7
const val MENU_SIGN_IN_ID = 1L
const val MENU_SIGN_OUT_ID = 2L

class DrawerWrapper(private val mapsActivity: MapsActivity, private val viewModel: MapViewModel) {
    var drawer: Drawer
    private lateinit var signInDrawerItem: PrimaryDrawerItem
    private lateinit var signOutDrawerItem: PrimaryDrawerItem

    init {
        drawer = mapsActivity.drawer {
            selectedItem = -1
            accountHeader {
                backgroundScaleType = ImageView.ScaleType.FIT_CENTER
                background = R.drawable.logo
            }
            signInDrawerItem = primaryItem(R.string.menu_signin) {
                identifier = MENU_SIGN_IN_ID
                iicon = GoogleMaterial.Icon.gmd_account_circle
                selectable = false
                onClick { _ ->
                   viewModel.onClickSignInButton()
                    false
                }
            }
            signOutDrawerItem = primaryItem(R.string.menu_signout) {
                identifier = MENU_SIGN_OUT_ID
                iicon = GoogleMaterial.Icon.gmd_exit_to_app
                selectable = false
                onClick { _ ->
                    AlertDialog.Builder(mapsActivity)
                            .setMessage(R.string.signout_prompt)
                            .setNegativeButton("No") { _, _ -> }
                            .setPositiveButton("Yes") { _, _ ->
                                AuthUI.getInstance()
                                        .signOut(mapsActivity)
                                        .addOnCompleteListener {
                                            viewModel.userSignedIn.value = false
                                        }
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
                            .start(mapsActivity)
                    false
                }
            }
            footer {
                secondaryItem("${BuildConfig.BUILD_TYPE} ${BuildConfig.VERSION_NAME} ") {
                    onClick { _, _, _ ->
                        viewModel.buildVersionClicked()
                        true
                    }
                }
            }
        }

        drawerImageLoader {
            placeholder { ctx, _ ->
                DrawerUIUtils.getPlaceHolder(ctx)
            }
            set { imageView, uri, placeholder, _ ->
                val req = Picasso.get().load(uri)
                placeholder?.let {
                    req.placeholder(placeholder)
                }
                req.into(imageView)
            }
            cancel { imageView ->
                Picasso.get().cancelRequest(imageView)
            }
        }
    }

    fun toggleSignedIn(signedIn: Boolean) {
        drawer.removeItem(MENU_SIGN_IN_ID)
        drawer.removeItem(MENU_SIGN_OUT_ID)
        if (signedIn) {
            drawer.setItemAtPosition(signOutDrawerItem, 1)
        } else {
            drawer.setItemAtPosition(signInDrawerItem, 1)
        }
    }

    fun showDevMenu() {
        drawer.addItem(DividerDrawerItem())
        addMenuItems(mapOf(
                Pair("Nuke DB", { viewModel.nukeDb() }),
                Pair("Sync Up", { uploadSync() }),
                Pair("Sync Down", { downloadSync() }),
                Pair("Test Crash", { Crashlytics.getInstance().crash() })
        ))
    }

    private fun addMenuItems(items: Map<String, () -> Unit>) {
        drawer.addItems(*items.map {
            PrimaryDrawerItem()
                    .withName(it.key)
                    .withIcon(GoogleMaterial.Icon.gmd_warning)
                    .withSelectable(false)
                    .withOnDrawerItemClickListener { _, _, _ ->
                        it.value()
                        false
                    }
        }.toTypedArray())
    }
}
