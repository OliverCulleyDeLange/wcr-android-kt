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
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.RouteType
import javax.inject.Inject

class SubmitRouteFragment : Fragment(), Injectable {
    companion object {
        private var id: Int = 0
        fun newRouteFragment(): SubmitRouteFragment {
            return SubmitRouteFragment().withId(id++)
        }
    }

    var fragmentId: Int? = null
    private fun withId(id: Int): SubmitRouteFragment = apply { this.fragmentId = id }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentSubmitRouteBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSubmitRouteBinding.inflate(layoutInflater, container, false)
        binding.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SubmitTopoViewModel::class.java)
        binding.vm = viewModel

        binding.routeTypeSpinner.adapter = ArrayAdapter(activity, R.layout.element_spinner_simple, RouteType.values())

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val parent = parentFragment
        if (parent is SubmitTopoFragment) {
            remove_fragment.setOnClickListener({
                parent.removeRouteFragment(this)
                binding.vm?.routes?.value?.remove(fragmentId)
            })
        }
        fragmentId?.let {
            if (binding.vm?.routes?.value?.containsKey(it) == false) {
                binding.vm?.routes?.value?.put(it, Route())
            }
            binding.fragmentId = fragmentId
        }
    }
}