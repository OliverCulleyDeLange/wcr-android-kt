package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_submit_location.*
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

    var sectorId: Long = -1

    private lateinit var binding: FragmentSubmitTopoBinding

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is MapsActivity) context.bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
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
            location_name_input_layout.error = binding.vm?.topoNameError?.value
        })

        return binding.root
    }
}