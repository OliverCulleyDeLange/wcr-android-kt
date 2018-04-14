package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentViewToposBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import javax.inject.Inject

class ToposFragment : Fragment(), Injectable {
    companion object {
        fun newToposFragment(): ToposFragment {
            return ToposFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentViewToposBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentViewToposBinding.inflate(layoutInflater, container, false)
        binding.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)

        return binding.root
    }
}