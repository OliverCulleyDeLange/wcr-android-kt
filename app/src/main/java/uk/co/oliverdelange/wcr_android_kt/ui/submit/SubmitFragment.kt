package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_submit.*
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import javax.inject.Inject

class SubmitFragment : Fragment(), Injectable {
    companion object {
        fun newInstance(): SubmitFragment {
            return SubmitFragment()
        }
    }

    interface ActivityInteractor {
        fun onSubmitFragmentReady(vm: SubmitViewModel?)
        fun removeSubmitFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var activityInteractor: ActivityInteractor
    private lateinit var binding: FragmentSubmitBinding

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is ActivityInteractor) {
            activityInteractor = context
        } else {
            throw ClassCastException(context!!.toString() + " must implement ActivityInteractor")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSubmitBinding.inflate(layoutInflater, container, false)
        binding.setLifecycleOwner(this)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SubmitViewModel::class.java)
        binding.vm = viewModel

        binding.submit.setOnClickListener { _: View? ->
            binding.vm?.submit()?.let {
                if (it) {
                    Snackbar.make(binding.submit, "Crag submitted!", Snackbar.LENGTH_SHORT).show()
                    activityInteractor.removeSubmitFragment()

                } else {
                    Snackbar.make(binding.submit, "Failed to submit crag!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.cragNameError.observe(this, Observer { _ ->
            crag_name_input_layout.error = binding.vm?.cragNameError?.value
        })

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityInteractor.onSubmitFragmentReady(binding.vm)
    }
}

//onAttach: When the fragment attaches to its host activity.
//onCreate: When a new fragment instance initializes, which always happens after it attaches to the host — fragments are a bit like viruses.
//onCreateView: When a fragment creates its portion of the view hierarchy, which is added to its activity’s view hierarchy.
//onActivityCreated: When the fragment’s activity has finished its own onCreate event.
//onStart: When the fragment is visible; a fragment starts only after its activity starts and often starts immediately after its activity does.
//onResume: When the fragment is visible and interactable; a fragment resumes only after its activity resumes and often resumes immediately after the activity does.