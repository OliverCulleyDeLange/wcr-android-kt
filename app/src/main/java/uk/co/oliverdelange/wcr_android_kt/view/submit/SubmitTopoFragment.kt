package uk.co.oliverdelange.wcr_android_kt.view.submit

import android.animation.ObjectAnimator
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_submit_topo.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitTopoBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.util.inTransaction
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmitTopoViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt


const val SELECT_PICTURE = 999
const val TAKE_PHOTO = 998
const val ROUTE_PADDING = 0.15

/*
    This is the main fragment when submitting a new topo.
    It allows the user to select an image, draw routes on it, add route info and submit
 */
class SubmitTopoFragment : Fragment(), Injectable {
    companion object {
        fun newTopoSubmissionFor(sectorId: String): SubmitTopoFragment {
            return SubmitTopoFragment().withSectorId(sectorId)
        }
    }

    interface ActivityInteractor {
        fun onTopoSubmitted(submittedTopoId: String?)
    }

    var sectorId: String? = null
    private fun withSectorId(id: String): SubmitTopoFragment = apply { this.sectorId = id }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var activityInteractor: ActivityInteractor? = null
    private var routeFragments: MutableList<SubmitRouteFragment> = mutableListOf()
    private lateinit var binding: FragmentSubmitTopoBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActivityInteractor) activityInteractor = context
    }

    override fun onDestroy() {
        super.onDestroy()
        SubmitRouteFragment.routeFragmentIdCounter = 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSubmitTopoBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SubmitTopoViewModel::class.java)
        binding.vm = viewModel
        val sectorIdSnap = sectorId
        if (sectorIdSnap != null) {
            Timber.d("Setting sectorId to '$sectorId' on ViewModel")
            viewModel.sectorId = sectorIdSnap
        } else { //Can this ever happen?
            Timber.e("SectorID not set in VM, submission will fail")
        }

        viewModel.doUndoDrawing.observe(this, Observer {
            Timber.d("doUndoDrawing changed, undoing last drawing action")
            // This feels super hacky, shouldn't all the route data live in the VM?
            // That way we just modify that, and re-draw the Paintable View.
            binding.topoImage.undoAction()
        })

        viewModel.submitting.observe(this, Observer {
            Timber.d("submitting changed, ${if (it) "starting" else "stopping"} animation")
            if (it == true) {
                val animation = ObjectAnimator.ofInt(topo_submit_progress, "progress", 0, 500)
                animation.duration = 5000 // in milliseconds
                animation.interpolator = DecelerateInterpolator()
                animation.repeatCount = Animation.INFINITE
                animation.start()
            } else {
                topo_submit_progress.clearAnimation()
            }
        })

        viewModel.submissionResult.observe(this, Observer {
            if (it.success) {
                activityInteractor?.onTopoSubmitted(it.submittedTopoId)
            } else {
                Snackbar.make(binding.submit, it.errorMessage
                        ?: "Oops, something went wrong", Snackbar.LENGTH_SHORT).show()
            }
        })

        viewModel.topoNameError.observe(this, Observer { _ ->
            Timber.d("topoNameError changed, updating error message")
            binding.topoNameInputLayout.error = binding.vm?.topoNameError?.value
        })

        val pagerAdapter = SubmitRoutePagerAdapter(childFragmentManager, routeFragments)
        binding.routePager.adapter = pagerAdapter
        binding.routePager.clipToPadding = false
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val paddinglr = (displayMetrics.widthPixels * ROUTE_PADDING).roundToInt()
        binding.routePager.setPadding(paddinglr, 0, paddinglr, 10)
        binding.routePager.pageMargin = 25
        binding.routePager.offscreenPageLimit = 99 // TODO More elegant way of fixing this
        binding.routePager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageSelected(position: Int) {
                viewModel.activeRoute.value = pagerAdapter.getItemId(position).toInt()
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val routeCount = binding.routePager.adapter?.count
                viewModel.onRoutePagerScroll(routeCount, position, positionOffset)
            }
        })

        addRoute(pagerAdapter)

        viewModel.activeRoute.observe(this, Observer { activeRouteFragmentId ->
            Timber.d("activeRoute changed, controlling new drawing path")
            activeRouteFragmentId?.let { routeFragmentId ->
                viewModel.routes.get(routeFragmentId)?.let { route ->
                    Timber.d("Active route fragment changed: $activeRouteFragmentId - route name: ${route.name}")
                    binding.topoImage.controlPath(routeFragmentId, route)
                }
            } //FragmentID
        })

        // Update the route line colour on the topo
        viewModel.routeColourUpdate.observe(this, Observer {
            Timber.d("routeColourUpdate changed, refreshig topo image")
            binding.topoImage.refresh()
        })

        // Update the grade if the route type changes
        viewModel.routeTypeUpdate.observe(this, Observer { routeType ->
            Timber.d("Route type changed, force selected the grade")
            if (routeType == null) {
                Timber.e("RouteType enum is null - wtf?")
                return@Observer
            }
            // Force select the right grade
            val submitRouteFragment = routeFragments[binding.routePager.currentItem]
            submitRouteFragment.binding?.let { routeFragmentBinding ->
                when (routeType) {
                    RouteType.TRAD -> {
                        // Only have to do one, as the grade gets set when either are selected
                        Timber.d("Route type now TRAD, setting trad adj grade")
                        val selectedTradAdjectivalGrade = TradAdjectivalGrade.values()[routeFragmentBinding.tradAdjectivalGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.tradAdjectivalGradeSpinner.setSelection(selectedTradAdjectivalGrade.ordinal, false)
                        routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedTradAdjectivalGrade.ordinal, GradeDropDown.TRAD_ADJ)
                    }
                    RouteType.SPORT -> {
                        Timber.d("Route type now SPORT, setting sport grade")
                        val selectedSportGrade = SportGrade.values()[routeFragmentBinding.sportGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.sportGradeSpinner.setSelection(selectedSportGrade.ordinal, false)
                        routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedSportGrade.ordinal, GradeDropDown.SPORT)
                    }
                    RouteType.BOULDERING -> {
                        if (binding.vm?.useVGradeForBouldering == true) {
                            Timber.d("Route type now BOULDERING, setting V grade")
                            val selectedVGrade = VGrade.values()[routeFragmentBinding.vGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.vGradeSpinner.setSelection(selectedVGrade.ordinal, false)
                            routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedVGrade.ordinal, GradeDropDown.V)
                        } else {
                            Timber.d("Route type now BOULDERING, setting FONT grade")
                            val selectedFGrade = FontGrade.values()[routeFragmentBinding.fGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.vGradeSpinner.setSelection(selectedVGrade.ordinal, false)
                            routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedFGrade.ordinal, GradeDropDown.FONT)
                        }
                    }
                }
                Unit // .let must return something
            }
        })

        binding.addRoute.setOnClickListener { addRoute(pagerAdapter) }

        binding.selectTopoImage.setOnClickListener {
            Timber.d("User wants to select topo image from gallery")
            if (viewModel.localTopoImage.value == null) selectImage()
        }

        binding.takePhotoImage.setOnClickListener {
            Timber.d("User wants to take topo image with camera")
            if (viewModel.localTopoImage.value == null) takePhoto()
        }

        return binding.root
    }

    fun removeRouteFragment(routeFragment: SubmitRouteFragment) {
        binding.vm?.onRemoveRoute(routeFragment.fragmentId)
        // Remove path from topo
        binding.topoImage.removePath(routeFragment.fragmentId)
        // Remove the fragment
        routeFragments.remove(routeFragment)
        childFragmentManager.inTransaction {
            remove(routeFragment)
        }
        binding.routePager.adapter?.notifyDataSetChanged()
        // Check if we should now show the add route button
        binding.vm?.onRouteRemoved(binding.routePager.adapter?.count)
    }

    private fun addRoute(pagerAdapter: SubmitRoutePagerAdapter) {
        //Create the new route fragment and add it to the view pager
        val routeFragment = SubmitRouteFragment.newRouteFragment(this)
        routeFragments.add(routeFragment)
        pagerAdapter.notifyDataSetChanged()
        // Automatically scroll to the new route fragment
        binding.routePager.setCurrentItem(pagerAdapter.count, true)
        // Get the fragment ID and set it as active so we control the right topo route path
        val activeRouteFragId = pagerAdapter.getItemId(binding.routePager.currentItem).toInt()
        binding.vm?.onAddRoute(activeRouteFragId, binding.topoImage.getPath(activeRouteFragId))
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    //https://developer.android.com/training/camera/photobasics
    private fun takePhoto() {
        activity?.let { activity ->
            activity.packageManager?.let { packageManager ->
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {
                        // Create the File where the photo should go
                        val photoFile: File? = try {
                            // Create an image file name
                            val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Date())
                            val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            File.createTempFile(
                                    "TOPO_${timeStamp}_",
                                    ".jpg",
                                    storageDir
                            ).apply {
                                // Save a file: path for use with ACTION_VIEW intents
                                Timber.d("Photo path: $absolutePath")
                            }
                        } catch (ex: IOException) {
                            Timber.e("Couldn't create file for topo photo to be saved to")
                            null
                        }
                        photoFile?.also { file ->
                            val photoURI: Uri = FileProvider.getUriForFile(
                                    activity,
                                    "com.example.android.fileprovider",
                                    file
                            )
                            binding.vm?.photoUri?.value = photoURI
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            startActivityForResult(takePictureIntent, TAKE_PHOTO)
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                data?.let { intent ->
                    val uri = intent.data
                    Timber.d("User selected picture: %s", uri)
                    if (uri != null) {
                        val takeFlags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        activity?.contentResolver?.takePersistableUriPermission(uri, takeFlags)

                        binding.vm?.localTopoImage?.value = uri
                    }
                }
            } else if (requestCode == TAKE_PHOTO) {
                Timber.d("Photo taken")
                binding.vm?.onPhotoTaken()
            }
        }
    }
}

class SubmitRoutePagerAdapter(fragmentManager: FragmentManager, val routeFragments: List<SubmitRouteFragment>) : FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        return routeFragments[position]
    }

    override fun getCount(): Int {
        return routeFragments.size
    }

    override fun getItemId(position: Int): Long {
        return routeFragments[position].fragmentId?.toLong() ?: -1
    }

    override fun getItemPosition(fragment: Any): Int {
        val fragmentPosition = routeFragments.indexOf(fragment as Fragment)
        return if (fragmentPosition == -1) POSITION_NONE else fragmentPosition
    }
}