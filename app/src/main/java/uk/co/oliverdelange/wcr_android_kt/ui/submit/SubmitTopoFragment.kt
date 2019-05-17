package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.animation.ObjectAnimator
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_submit_topo.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitTopoBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.util.inTransaction
import javax.inject.Inject
import kotlin.math.roundToInt


const val SELECT_PICTURE = 999
const val ROUTE_PADDING = 0.15

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

    override fun onAttach(context: Context?) {
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

        binding.submit.setOnClickListener {
            sectorId?.let { sectorId ->
                binding.vm?.submit(sectorId)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribe({ submittedTopoId ->
                            Timber.i("Submission Succeeded")
                            activityInteractor?.onTopoSubmitted(submittedTopoId)
                        }, { e ->
                            Timber.e(e, "Submission Failed")
                            Snackbar.make(binding.submit, "Failed to submit topo!", Snackbar.LENGTH_SHORT).show()
                        })
            }
        }

        viewModel.submitting.observe(this, Observer {
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

        binding.selectTopoImage.setOnClickListener {
            if (viewModel.localTopoImage.value == null) selectImage()
        }

        viewModel.topoNameError.observe(this, Observer { _ ->
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
                binding.vm?.activeRoute?.value = pagerAdapter.getItemId(position).toInt()
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val routeCount = binding.routePager.adapter?.count
                binding.vm?.setShouldShowAddRouteButton(routeCount, position, positionOffset)
            }
        })

        addRoute(pagerAdapter)
        binding.addRoute.setOnClickListener { addRoute(pagerAdapter) }

        binding.vm?.activeRoute?.observe(this, Observer { activeRouteFragmentId ->
            activeRouteFragmentId?.let { routeFragmentId ->
                val route = binding.vm?.routes?.get(routeFragmentId)
                route?.let { route ->
                    Timber.d("Active route fragment changed: $activeRouteFragmentId - route name: ${route.name}")
                    binding.topoImage.controlPath(routeFragmentId, route)
                }
            } //FragmentID
        })

        binding.topoImage.setOnTouchImageViewListener { binding.vm?.tryEnableSubmit() }
        // Update the route line colour on the topo
        binding.vm?.routeColourUpdate?.observe(this, Observer {
            binding.topoImage.refresh()
            Timber.d("Refreshed topo due to grade change")
        })

        // Update the grade if the route type changes
        binding.vm?.routeTypeUpdate?.observe(this, Observer { routeType ->
            // Force select the right grade
            Timber.d("Route type changed, force selected the grade")
            val submitRouteFragment = routeFragments[binding.routePager.currentItem]
            submitRouteFragment.binding?.let { routeFragmentBinding ->
                when (routeType) {
                    RouteType.TRAD -> {
                        // Only have to do one, as the grade gets set when either are selected
                        Timber.d("Route type now TRAD, setting trad adj grade")
                        val selectedTradAdjectivalGrade = TradAdjectivalGrade.values()[routeFragmentBinding.tradAdjectivalGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.tradAdjectivalGradeSpinner.setSelection(selectedTradAdjectivalGrade.ordinal, false)
                        routeFragmentBinding.vm?.gradeChanged(routeFragmentBinding.fragmentId!!, selectedTradAdjectivalGrade.ordinal, GradeDropDown.TRAD_ADJ)
                    }
                    RouteType.SPORT -> {
                        Timber.d("Route type now SPORT, setting sport grade")
                        val selectedSportGrade = SportGrade.values()[routeFragmentBinding.sportGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.sportGradeSpinner.setSelection(selectedSportGrade.ordinal, false)
                        routeFragmentBinding.vm?.gradeChanged(routeFragmentBinding.fragmentId!!, selectedSportGrade.ordinal, GradeDropDown.SPORT)
                    }
                    RouteType.BOULDERING -> {
                        if (binding.vm?.useVGradeForBouldering == true) {
                            Timber.d("Route type now BOULDERING, setting V grade")
                            val selectedVGrade = VGrade.values()[routeFragmentBinding.vGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.vGradeSpinner.setSelection(selectedVGrade.ordinal, false)
                            routeFragmentBinding.vm?.gradeChanged(routeFragmentBinding.fragmentId!!, selectedVGrade.ordinal, GradeDropDown.V)
                        } else {
                            Timber.d("Route type now BOULDERING, setting FONT grade")
                            val selectedFGrade = FontGrade.values()[routeFragmentBinding.fGradeSpinner.selectedItemPosition]
//                        routeFragmentBinding.vGradeSpinner.setSelection(selectedVGrade.ordinal, false)
                            routeFragmentBinding.vm?.gradeChanged(routeFragmentBinding.fragmentId!!, selectedFGrade.ordinal, GradeDropDown.FONT)
                        }
                    }
                }
                Unit // .let must return something
            }
        })

        return binding.root
    }

    fun removeRouteFragment(routeFragment: SubmitRouteFragment) {
        binding.vm?.removeRoute(routeFragment.fragmentId)
        // Remove path from topo
        binding.topoImage.removePath(routeFragment.fragmentId)
        // Remove the fragment
        routeFragments.remove(routeFragment)
        fragmentManager?.inTransaction { remove(routeFragment) }
        binding.routePager.adapter?.notifyDataSetChanged()
        // Check if we should now show the add route button
        binding.vm?.setShouldShowAddRouteButton(binding.routePager.adapter?.count)
        binding.vm?.tryEnableSubmit()
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
        binding.vm?.addRoute(activeRouteFragId, binding.topoImage.paths)
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                data?.let { intent ->
                    val uri = intent.data
                    Timber.d("User selected picture: %s", uri)
                    val takeFlags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    activity?.contentResolver?.takePersistableUriPermission(uri, takeFlags)

                    binding.vm?.localTopoImage?.value = uri
                    binding.vm?.tryEnableSubmit()
                }
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