package uk.co.oliverdelange.wcr_android_kt.view.submit

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_submit_topo.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.BuildConfig
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitTopoBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.viewmodel.*
import java.io.ByteArrayInputStream
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
class SubmitTopoFragment(private val sectorId: String) : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentSubmitTopoBinding

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

        Timber.d("Setting sectorId to '$sectorId' on ViewModel")
        viewModel.sectorId = sectorId

        val pagerAdapter = SubmitRoutePagerAdapter(this, childFragmentManager, emptyList())
        binding.routePager.adapter = pagerAdapter
        binding.routePager.clipToPadding = false
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val paddinglr = (displayMetrics.widthPixels * ROUTE_PADDING).roundToInt()
        binding.routePager.setPadding(paddinglr, 0, paddinglr, 10)
        binding.routePager.pageMargin = 25
        binding.routePager.offscreenPageLimit = 99 // TODO More elegant way of fixing this
        binding.routePager.addOnPageChangeListener(RoutePageListener(viewModel))

        observeViewModel(viewModel)
        setupEvents(viewModel)

        return binding.root
    }

    private fun observeViewModel(viewModel: SubmitTopoViewModel) {

        viewModel.topoImage.observe(viewLifecycleOwner, Observer {
            it?.let { bos ->
                val bytes = bos.toByteArray()
                val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(bytes))
                Timber.d("Image IS ${bitmap.width}x${bitmap.height} kb:${bitmap.byteCount / 1000}, filesize: ${bytes.size / 1000}kb")
                binding.topoImage.setImageBitmap(bitmap)
            }
        })

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
                is SubmissionFailed -> Snackbar.make(binding.submit, it.error, Snackbar.LENGTH_SHORT).show()
                is NavigateToImageSelectionGallery -> startSelectImageActivity()
                is NavigateToCamera -> takePhoto()
                is SubmissionSucceeded -> {
                    Timber.d("Finishing SubmitActivity. Topo ${it.topoId} uploaded ok.")
                    activity?.also { activity -> if (activity is SubmitActivity) activity.end() }
                }
            }
        })

        viewModel.topoNameError.observe(viewLifecycleOwner, Observer { _ ->
            Timber.d("topoNameError changed, updating error message")
            binding.topoNameInputLayout.error = binding.vm?.topoNameError?.value
        })

        viewModel.routes.observe(viewLifecycleOwner, Observer { routes ->
            Timber.d("Routes changed: $routes")
            val submitRoutePagerAdapter = binding.routePager.adapter as SubmitRoutePagerAdapter
            submitRoutePagerAdapter.routes = routes
            submitRoutePagerAdapter.notifyDataSetChanged()

            routes.firstOrNull { it.isActive }?.let {
                val pagerPosition = it.pagerPosition
                if (pagerPosition != binding.routePager.currentItem) {
                    Timber.d("Scrolling to new active route: $it")
                    binding.routePager.setCurrentItem(pagerPosition, true)
                }
            }
            // Update paths
            binding.topoImage.update(routes)
        })
    }

    private fun setupEvents(viewModel: SubmitTopoViewModel) {
        binding.addRoute.setOnClickListener {
            viewModel.onAddRoute()
        }

        binding.selectTopoImage.setOnClickListener {
            viewModel.onSelectExistingPhoto()
        }

        binding.takePhotoImage.setOnClickListener {
            viewModel.onSelectTakePhoto()
        }

        binding.topoImage.setOnDrawListener { x, y, event ->
            val touchEvent = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> SubmitTopoViewModel.TouchEvent.TOUCH_DOWN
                MotionEvent.ACTION_MOVE -> SubmitTopoViewModel.TouchEvent.TOUCH_MOVE
                MotionEvent.ACTION_UP -> SubmitTopoViewModel.TouchEvent.TOUCH_UP
                else -> SubmitTopoViewModel.TouchEvent.IGNORE
            }
            viewModel.onDraw(x, y, touchEvent)
            true // Event consumed
        }
    }

    private fun startSelectImageActivity() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    //TODO Not sure if this is the best place to store the URI...
    private var photoURI: Uri? = null

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
                            photoURI = FileProvider.getUriForFile(
                                    activity,
                                    "${BuildConfig.APPLICATION_ID}.android.fileprovider",
                                    file
                            )
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
                Timber.d("SELECT_PICTURE activity completed")
                data?.let { intent ->
                    val uri = intent.data
                    if (uri == null) Timber.e("User selected image from device, but no URI available")
                    if (uri != null) {
                        val takeFlags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        activity?.contentResolver?.takePersistableUriPermission(uri, takeFlags)
                        binding.vm?.onSelectedExistingPhoto(uri)
                    }
                }
            } else if (requestCode == TAKE_PHOTO) {
                Timber.d("TAKE_PHOTO activity completed")
                photoURI?.let {
                    Timber.d("Photo uri available")
                    binding.vm?.onPhotoTaken(it)
                }
            }
        }
    }
}

class RoutePageListener(private val viewModel: SubmitTopoViewModel) : ViewPager.OnPageChangeListener {
    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageSelected(position: Int) {
        viewModel.onSelectRoute(position)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        viewModel.onRoutePagerScroll(position, positionOffset)
    }
}

class SubmitRoutePagerAdapter(
        private val viewModelStoreOwner: ViewModelStoreOwner,
        fragmentManager: FragmentManager,
        var routes: List<RouteViewModel>
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {

        Timber.d("CREATING SubmitRouteFragment at position $position")
        return SubmitRouteFragment(viewModelStoreOwner, routes[position].pagerId)
    }

    override fun getCount(): Int {
        return routes.size
    }

    override fun getItemPosition(f: Any): Int {
        val fragment = f as SubmitRouteFragment
        val route = routes.find { it.pagerId == fragment.pagerId }
        return route?.pagerPosition ?: POSITION_NONE
    }

    override fun getItemId(position: Int): Long {
        return routes[position].pagerId.toLong()
    }
}