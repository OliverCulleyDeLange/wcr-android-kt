package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.app.Activity.RESULT_OK
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_submit_topo.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitTopoBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.FontGrade
import uk.co.oliverdelange.wcr_android_kt.model.GradeType
import uk.co.oliverdelange.wcr_android_kt.model.VGrade
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapsActivity
import uk.co.oliverdelange.wcr_android_kt.util.fontToV
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
        fun onTopoSubmitted(submittedTopoAndRouteIds: Pair<Long, Array<Long>>?)
    }

    var sectorId: Long? = null
    private fun withSectorId(id: Long): SubmitTopoFragment = apply { this.sectorId = id }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var activityInteractor: ActivityInteractor? = null
    private var routeFragments: MutableList<SubmitRouteFragment> = mutableListOf(SubmitRouteFragment.newRouteFragment())
    private lateinit var binding: FragmentSubmitTopoBinding

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is MapsActivity) context.bottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
        if (context is ActivityInteractor) activityInteractor = context
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
                        Snackbar.make(binding.submit, "topo submitted!", Snackbar.LENGTH_SHORT).show()
                        activityInteractor?.onTopoSubmitted(it)
                    } else {
                        Snackbar.make(binding.submit, "failed to submit topo!", Snackbar.LENGTH_SHORT).show()
                    }
                })
            }
        }

        binding.topoImage.setOnClickListener { selectImage() }

        viewModel.topoNameError.observe(this, Observer { _ ->
            binding.topoNameInputLayout.error = binding.vm?.topoNameError?.value
        })

        val pagerAdapter = SubmitRoutePagerAdapter(childFragmentManager, routeFragments)
        binding.routePager.adapter = pagerAdapter
        binding.routePager.clipToPadding = false
        binding.routePager.setPadding(100, 20, 100, 20)
        binding.routePager.pageMargin = 25
        binding.routePager.offscreenPageLimit = 99 // TODO More elegant way of fixing this

        binding.addRoute.setOnClickListener({
            routeFragments.add(SubmitRouteFragment.newRouteFragment())
            pagerAdapter.notifyDataSetChanged()
        })

        binding.vm?.boulderingGradeType?.observe(this, Observer {
            it?.let {
                val submitRouteFragment = routeFragments[route_pager.currentItem]
                binding.vm?.autoGradeChange = true
                when (it) {
                    GradeType.FONT -> {
                        val convertedGrade = fontToV(FontGrade.values()[submitRouteFragment.binding.fGradeSpinner.selectedItemPosition])
                        submitRouteFragment.binding.vGradeSpinner.setSelection(convertedGrade.ordinal, false)
                    }
                    GradeType.V -> {
                        val convertedGrade = vToFont(VGrade.values()[submitRouteFragment.binding.vGradeSpinner.selectedItemPosition])
                        submitRouteFragment.binding.fGradeSpinner.setSelection(convertedGrade.ordinal, false)
                    }
                }
            }
        })

        return binding.root
    }

    fun removeRouteFragment(routeFragment: SubmitRouteFragment) {
        routeFragments.remove(routeFragment)
        binding.routePager.adapter?.notifyDataSetChanged()
    }

    fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Timber.d("User selected picture: %s", data?.data)
                binding.vm?.topoImage?.value = data?.data
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
