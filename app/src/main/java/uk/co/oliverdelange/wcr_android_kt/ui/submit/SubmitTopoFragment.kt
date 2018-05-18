package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.animation.ObjectAnimator
import android.app.Activity.RESULT_OK
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import kotlinx.android.synthetic.main.fragment_submit_topo.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitTopoBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.FontGrade
import uk.co.oliverdelange.wcr_android_kt.model.GradeType
import uk.co.oliverdelange.wcr_android_kt.model.VGrade
import uk.co.oliverdelange.wcr_android_kt.util.fontToV
import uk.co.oliverdelange.wcr_android_kt.util.inTransaction
import uk.co.oliverdelange.wcr_android_kt.util.vToFont
import javax.inject.Inject


val SELECT_PICTURE = 999

class SubmitTopoFragment : Fragment(), Injectable {
    companion object {
        fun newTopoSubmissionFor(sectorId: Long): SubmitTopoFragment {
            return SubmitTopoFragment().withSectorId(sectorId)
        }
    }

    interface ActivityInteractor {
        fun onTopoSubmitted(submittedTopoAndRouteIds: Pair<Long, List<Long>>?)
    }

    var sectorId: Long? = null
    private fun withSectorId(id: Long): SubmitTopoFragment = apply { this.sectorId = id }

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
        binding.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SubmitTopoViewModel::class.java)
        binding.vm = viewModel

        binding.submit.setOnClickListener { _: View? ->
            sectorId?.let { sectorId ->
                binding.vm?.submit(sectorId)?.observe(this, Observer {
                    if (it != null) {
                        Timber.i("Submission Succeeded")
                        activityInteractor?.onTopoSubmitted(it)
                    } else {
                        Snackbar.make(binding.submit, "Failed to submit topo!", Snackbar.LENGTH_SHORT).show()
                    }
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
        binding.routePager.setPadding(120, 0, 120, 20)
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

        binding.vm?.activeRoute?.observe(this, Observer { activeRouteId ->
            activeRouteId?.let { binding.topoImage.controlPath(it) }
        })

        binding.vm?.boulderingGradeType?.observe(this, Observer {
            it?.let { gradeType ->
                val submitRouteFragment = routeFragments[binding.routePager.currentItem]
                submitRouteFragment.binding?.let { binding ->
                    binding.vm?.autoGradeChange = true
                    when (gradeType) {
                        GradeType.FONT -> {
                            val convertedGrade = fontToV(FontGrade.values()[binding.fGradeSpinner.selectedItemPosition])
                            binding.vGradeSpinner.setSelection(convertedGrade.ordinal, false)
                        }
                        GradeType.V -> {
                            val convertedGrade = vToFont(VGrade.values()[binding.vGradeSpinner.selectedItemPosition])
                            binding.fGradeSpinner.setSelection(convertedGrade.ordinal, false)
                        }
                    }
                }
            }
        })

        return binding.root
    }

    fun removeRouteFragment(routeFragment: SubmitRouteFragment) {
        routeFragments.remove(routeFragment)
        fragmentManager?.inTransaction { remove(routeFragment) }
        binding.routePager.adapter?.notifyDataSetChanged()
        binding.topoImage.removePath(routeFragment.fragmentId)
        binding.vm?.setShouldShowAddRouteButton(binding.routePager.adapter?.count)
    }

    private fun addRoute(pagerAdapter: SubmitRoutePagerAdapter) {
        //Create the new route fragment and add it to the view pager
        val routeFragment = SubmitRouteFragment.newRouteFragment()
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