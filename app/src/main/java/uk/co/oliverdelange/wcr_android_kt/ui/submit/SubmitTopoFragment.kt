package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
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
import uk.co.oliverdelange.wcr_android_kt.MapsActivity
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitTopoBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import javax.inject.Inject


class SubmitTopoFragment : Fragment(), Injectable {
    companion object {
        fun newTopoSubmission(): SubmitTopoFragment {
            return SubmitTopoFragment()
        }
    }

    interface ActivityInteractor {
        fun onTopoSubmitted(submittedTopoId: Long)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var activityInteractor: ActivityInteractor? = null
    private var routeFragments: MutableList<RouteFragment> = mutableListOf(RouteFragment.newRouteFragment())

    var sectorId: Long = -1

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
            binding.vm?.submit(sectorId)?.observe(this, Observer {
                if (it != null) {
                    Snackbar.make(binding.submit, "topo submitted!", Snackbar.LENGTH_SHORT).show()
                    activityInteractor?.onTopoSubmitted(it)
                } else {
                    Snackbar.make(binding.submit, "failed to submit topo!", Snackbar.LENGTH_SHORT).show()
                }
            })
        }

        viewModel.topoNameError.observe(this, Observer { _ ->
            topo_name_input_layout.error = binding.vm?.topoNameError?.value
        })

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        route_pager.adapter = RoutePagerAdapter(childFragmentManager, routeFragments)
        route_pager.clipToPadding = false
        route_pager.setPadding(200, 20, 200, 20)
        route_pager.pageMargin = 25

        add_route.setOnClickListener({
            routeFragments.add(RouteFragment.newRouteFragment())
            route_pager.adapter?.notifyDataSetChanged()
        })
    }

    fun removeRouteFragment(routeFragment: RouteFragment) {
        routeFragments.remove(routeFragment)
        route_pager.adapter?.notifyDataSetChanged()
    }

}

class RoutePagerAdapter(fragmentManager: FragmentManager, val routeFragments: List<RouteFragment>) : FragmentPagerAdapter(fragmentManager) {
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
