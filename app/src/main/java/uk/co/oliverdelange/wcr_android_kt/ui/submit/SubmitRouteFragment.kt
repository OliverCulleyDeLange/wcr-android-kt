package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_submit_route.*
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitRouteBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.*
import javax.inject.Inject

class SubmitRouteFragment : Fragment(), Injectable {
    companion object {
        var routeFragmentIdCounter: Int = 0
        fun newRouteFragment(): SubmitRouteFragment {
            return SubmitRouteFragment().withId(routeFragmentIdCounter++)
        }
    }

    var fragmentId: Int? = null
    private fun withId(id: Int): SubmitRouteFragment = apply { this.fragmentId = id }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    var binding: FragmentSubmitRouteBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSubmitRouteBinding.inflate(layoutInflater, container, false)
        binding?.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SubmitTopoViewModel::class.java)
        binding?.vm = viewModel

        binding?.routeTypeSpinner?.adapter = ArrayAdapter(activity, R.layout.element_spinner_simple, RouteType.values())
        binding?.vGradeSpinner?.adapter = ArrayAdapter(activity, R.layout.element_spinner_simple, VGrade.values().map { it.textRepresentation })
        binding?.fGradeSpinner?.adapter = ArrayAdapter(activity, R.layout.element_spinner_simple, FontGrade.values().map { it.textRepresentation })
        binding?.sportGradeSpinner?.adapter = ArrayAdapter(activity, R.layout.element_spinner_simple, SportGrade.values().map { it.textRepresentation })
        binding?.tradAdjectivalGradeSpinner?.adapter = ArrayAdapter(activity, R.layout.element_spinner_simple, TradAdjectivalGrade.values().map { it.textRepresentation })
        binding?.tradTechnicalGradeSpinner?.adapter = ArrayAdapter(activity, R.layout.element_spinner_simple, TradTechnicalGrade.values().map { it.textRepresentation })

        binding?.fragmentId = fragmentId

        return binding?.root
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.vm?.routes?.remove(fragmentId)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val parent = parentFragment
        if (parent is SubmitTopoFragment) {
            remove_fragment.setOnClickListener({
                parent.removeRouteFragment(this)
            })
        }
    }
}