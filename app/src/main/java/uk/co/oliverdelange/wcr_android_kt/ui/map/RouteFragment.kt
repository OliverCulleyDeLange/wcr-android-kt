package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import uk.co.oliverdelange.wcr_android_kt.databinding.RouteCardBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.Route

class RouteFragment : Fragment(), Injectable {
    companion object {
        fun newRouteFragment(route: Route): RouteFragment {
            return RouteFragment().withRoute(route)
        }
    }

    var route: Route? = null
    private fun withRoute(route: Route): RouteFragment = apply { this.route = route }

    private lateinit var binding: RouteCardBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = RouteCardBinding.inflate(layoutInflater, container, false)
        binding.setLifecycleOwner(this)
        binding.route = route

        return binding.root
    }
}