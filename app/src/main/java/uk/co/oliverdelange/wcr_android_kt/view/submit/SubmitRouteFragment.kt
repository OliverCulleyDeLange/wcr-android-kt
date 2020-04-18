package uk.co.oliverdelange.wcr_android_kt.view.submit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitRouteBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmitTopoViewModel
import javax.inject.Inject

/*
    This fragment is used when submitting a new topo.
    It gathers information for a new route, including name, grade, description etc
    It takes the form of a small card, which are paged horizontally
 */
class SubmitRouteFragment(
        private val viewModelOwner: ViewModelStoreOwner,
        val pagerId: Int
) : Fragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    var binding: FragmentSubmitRouteBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSubmitRouteBinding.inflate(layoutInflater, container, false)
        binding?.lifecycleOwner = this
        val viewModel = ViewModelProvider(viewModelOwner, viewModelFactory).get(SubmitTopoViewModel::class.java)
        binding?.vm = viewModel
        binding?.fragmentId = pagerId

        binding?.routeTypeSpinner?.adapter = ArrayAdapter(requireContext(), R.layout.element_spinner_simple, RouteType.values())
        binding?.vGradeSpinner?.adapter = ArrayAdapter(requireContext(), R.layout.element_spinner_simple, VGrade.values().map { it.textRepresentation })
        binding?.fGradeSpinner?.adapter = ArrayAdapter(requireContext(), R.layout.element_spinner_simple, FontGrade.values().map { it.textRepresentation })
        binding?.sportGradeSpinner?.adapter = ArrayAdapter(requireContext(), R.layout.element_spinner_simple, SportGrade.values().map { it.textRepresentation })
        binding?.tradAdjectivalGradeSpinner?.adapter = ArrayAdapter(requireContext(), R.layout.element_spinner_simple, TradAdjectivalGrade.values().map { it.textRepresentation })
        binding?.tradTechnicalGradeSpinner?.adapter = ArrayAdapter(requireContext(), R.layout.element_spinner_simple, TradTechnicalGrade.values().map { it.textRepresentation })

        return binding?.root
    }
}