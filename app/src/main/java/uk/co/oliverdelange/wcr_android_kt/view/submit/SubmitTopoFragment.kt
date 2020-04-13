package uk.co.oliverdelange.wcr_android_kt.view.submit

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.ExifInterface.ORIENTATION_UNDEFINED
import android.media.ExifInterface.TAG_ORIENTATION
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
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
import uk.co.oliverdelange.wcr_android_kt.BuildConfig
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitTopoBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.util.inTransaction
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MAX_TOPO_SIZE_PX
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmissionFailed
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmissionSucceeded
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmitTopoViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSubmitTopoBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SubmitTopoViewModel::class.java)
        binding.vm = viewModel
        context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)?.let {
            Timber.d("Camera available: $it")
            viewModel.setHasCamera(it)
        }

        val sectorIdSnap = sectorId
        if (sectorIdSnap != null) {
            Timber.d("Setting sectorId to '$sectorId' on ViewModel")
            viewModel.sectorId = sectorIdSnap
        } else { //Can this ever happen?
            Timber.e("SectorID not set in VM, submission will fail")
        }

//        viewModel.doUndoDrawing.observe(this, Observer {
//            Timber.d("doUndoDrawing changed, undoing last drawing action")
//            // This feels super hacky, shouldn't all the route data live in the VM?
//            // That way we just modify that, and re-draw the Paintable View.
//            binding.topoImage.undoAction()
//        })

        viewModel.submitting.observe(viewLifecycleOwner, Observer {
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

        viewModel.viewEvents.observe(viewLifecycleOwner, Observer {
            when (it) {
                is SubmissionSucceeded -> activityInteractor?.onTopoSubmitted(it.topoId)
                is SubmissionFailed -> Snackbar.make(binding.submit, it.error, Snackbar.LENGTH_SHORT).show()
            }
        })

        viewModel.topoNameError.observe(viewLifecycleOwner, Observer { _ ->
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
                viewModel.onRouteSelected(pagerAdapter.getItemId(position).toInt())
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val routeCount = binding.routePager.adapter?.count
                viewModel.onRoutePagerScroll(routeCount, position, positionOffset)
            }
        })

        addRoute(pagerAdapter)

        viewModel.activeRoute.observe(viewLifecycleOwner, Observer { activeRouteFragmentId ->
            Timber.d("activeRoute changed, controlling new drawing path")
            activeRouteFragmentId?.let { routeFragmentId ->
                viewModel.routes[routeFragmentId]?.let { route ->
                    Timber.d("Active route fragment changed: $activeRouteFragmentId - route name: ${route.name}")
//                    binding.topoImage.controlPath(routeFragmentId, route)
                }
            } //FragmentID
        })

        //FIXME just redraw the topoImage view when the routes change
//        // Update the route line colour on the topo
//        viewModel.routeColourUpdate.observe(viewLifecycleOwner, Observer {
//            Timber.d("routeColourUpdate changed, refreshig topo image")
//            binding.topoImage.refresh()
//        })

        // FIXME Logic in view - why did i do this!?
        // Update the grade if the route type changes
//        viewModel.routeTypeUpdate.observe(this, Observer { routeType ->
//            Timber.d("Route type changed, force selected the grade")
//            if (routeType == null) {
//                Timber.e("RouteType enum is null - wtf?")
//                return@Observer
//            }
//            // Force select the right grade
//            val submitRouteFragment = routeFragments[binding.routePager.currentItem]
//            submitRouteFragment.binding?.let { routeFragmentBinding ->
//                when (routeType) {
//                    RouteType.TRAD -> {
//                        // Only have to do one, as the grade gets set when either are selected
//                        Timber.d("Route type now TRAD, setting trad adj grade")
//                        val selectedTradAdjectivalGrade = TradAdjectivalGrade.values()[routeFragmentBinding.tradAdjectivalGradeSpinner.selectedItemPosition]
////                        routeFragmentBinding.tradAdjectivalGradeSpinner.setSelection(selectedTradAdjectivalGrade.ordinal, false)
//                        routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedTradAdjectivalGrade.ordinal, GradeDropDown.TRAD_ADJ)
//                    }
//                    RouteType.SPORT -> {
//                        Timber.d("Route type now SPORT, setting sport grade")
//                        val selectedSportGrade = SportGrade.values()[routeFragmentBinding.sportGradeSpinner.selectedItemPosition]
////                        routeFragmentBinding.sportGradeSpinner.setSelection(selectedSportGrade.ordinal, false)
//                        routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedSportGrade.ordinal, GradeDropDown.SPORT)
//                    }
//                    RouteType.BOULDERING -> {
//                        if (binding.vm?.useVGradeForBouldering == true) {
//                            Timber.d("Route type now BOULDERING, setting V grade")
//                            val selectedVGrade = VGrade.values()[routeFragmentBinding.vGradeSpinner.selectedItemPosition]
////                        routeFragmentBinding.vGradeSpinner.setSelection(selectedVGrade.ordinal, false)
//                            routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedVGrade.ordinal, GradeDropDown.V)
//                        } else {
//                            Timber.d("Route type now BOULDERING, setting FONT grade")
//                            val selectedFGrade = FontGrade.values()[routeFragmentBinding.fGradeSpinner.selectedItemPosition]
////                        routeFragmentBinding.vGradeSpinner.setSelection(selectedVGrade.ordinal, false)
//                            routeFragmentBinding.vm?.onGradeChanged(routeFragmentBinding.fragmentId!!, selectedFGrade.ordinal, GradeDropDown.FONT)
//                        }
//                    }
//                }
//                Unit // .let must return something
//            }
//        })

        binding.addRoute.setOnClickListener { addRoute(pagerAdapter) }

        binding.selectTopoImage.setOnClickListener {
            Timber.d("User wants to select topo image from gallery")
            if (viewModel.localTopoImage.value == null) selectImage()
        }

        binding.takePhotoImage.setOnClickListener {
            Timber.d("User wants to take topo image with camera")
            if (viewModel.localTopoImage.value == null) takePhoto()
        }

        binding.topoImage.setOnTouchListener { _, event ->
            val touchEvent = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> SubmitTopoViewModel.TouchEvent.TOUCH_DOWN
                MotionEvent.ACTION_MOVE -> SubmitTopoViewModel.TouchEvent.TOUCH_MOVE
                MotionEvent.ACTION_UP-> SubmitTopoViewModel.TouchEvent.TOUCH_UP
                else -> SubmitTopoViewModel.TouchEvent.IGNORE
            }
            viewModel.onDraw(event.x, event.y, touchEvent )
        }

        return binding.root
    }

    fun removeRouteFragment(routeFragment: SubmitRouteFragment) {
        binding.vm?.onRemoveRoute(routeFragment.fragmentId)
        // Remove path from topo
//        binding.topoImage.removePath(routeFragment.fragmentId)
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
        binding.vm?.onAddRoute(activeRouteFragId)
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    private fun loadImage(imageUri: Uri) {
        val contentResolver = context?.contentResolver
        MediaStore.Images.Media.getBitmap(contentResolver, imageUri)?.let { bitmap ->
            Timber.d("Image WAS ${bitmap.width}x${bitmap.height} kb:${bitmap.byteCount / 1000}")
            //Get orientation https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
            val imageInputStream = contentResolver?.openInputStream(imageUri)
            // TODO Test on various OS versions
            val exif = if (Build.VERSION.SDK_INT > 23) ExifInterface(imageInputStream) else ExifInterface(imageUri.path)
            val orientation = exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_UNDEFINED)

            val out = ByteArrayOutputStream()
            val matrix = Matrix()
            // Scale
            val widthScale = MAX_TOPO_SIZE_PX.toFloat() / bitmap.width
            matrix.setScale(widthScale, widthScale)
            // Rotate
            matrix.postRotate(when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            })
            val scaledAndRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            // TODO CHeck memory usage
            bitmap.recycle()
            // Compress
            scaledAndRotated.compress(Bitmap.CompressFormat.WEBP, 75, out)
            scaledAndRotated.recycle()
            val bytes = out.toByteArray()

            val scaledAndCompressed = BitmapFactory.decodeStream(ByteArrayInputStream(bytes))
            Timber.d("Image IS ${scaledAndCompressed.width}x${scaledAndCompressed.height} " +
                    "kb:${scaledAndCompressed.byteCount / 1000}, filesize: ${bytes.size / 1000}kb")

            binding.topoImage.setImageBitmap(scaledAndCompressed)
        }
    }

    //https://developer.android.com/training/camera/photobasics
    @SuppressLint("SimpleDateFormat")
    private fun takePhoto() {
        activity?.let { activity ->
            activity.packageManager?.let { packageManager ->
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {
                        // Create the File where the photo should go
                        val photoFile: File? = try {
                            // Create an image file name
                            val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Date())
                            val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
                                    "${BuildConfig.APPLICATION_ID}.android.fileprovider",
                                    file
                            )
                            binding.vm?.onSelectTakePhoto(photoURI)
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

                        binding.vm?.onSelectExistingPhoto(uri)
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