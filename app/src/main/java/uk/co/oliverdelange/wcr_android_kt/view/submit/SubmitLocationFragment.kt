package uk.co.oliverdelange.wcr_android_kt.view.submit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_submit_location.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentSubmitLocationBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.map.IconHelper
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.view.map.MapsActivity
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmitLocationViewModel
import javax.inject.Inject

class SubmitLocationFragment : androidx.fragment.app.Fragment(), Injectable {
    companion object {
        fun newCragSubmission(): SubmitLocationFragment {
            return SubmitLocationFragment().also { it.locationType = LocationType.CRAG }
        }

        fun newSectorSubmission(): SubmitLocationFragment {
            return SubmitLocationFragment().also { it.locationType = LocationType.SECTOR }
        }
    }

    interface ActivityInteractor {
        fun onLocationSubmitted(locationType: LocationType, submittedLocationId: String)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var activityInteractor: ActivityInteractor? = null

    var parentId: String? = null
    lateinit var locationType: LocationType
    private var newLocationMarker: Marker? = null

    private lateinit var binding: FragmentSubmitLocationBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActivityInteractor) activityInteractor = context
    }

    override fun onDetach() {
        super.onDetach()
        newLocationMarker?.remove()
        newLocationMarker = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSubmitLocationBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SubmitLocationViewModel::class.java)
        binding.vm = viewModel
        binding.vm?.locationType = locationType

        binding.submit.setOnClickListener {
            binding.vm?.submit(parentId)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe(
                            {
                                Timber.d("New location submitted, notifying activity")
                                activityInteractor?.onLocationSubmitted(locationType, it)
                            },
                            {
                                Timber.e(it)
                                Snackbar.make(binding.submit, "Failed to submit location!", Snackbar.LENGTH_SHORT).show()
                            })
        }

        viewModel.locationNameError.observe(this, Observer { _ ->
            location_name_input_layout.error = binding.vm?.locationNameError?.value
        })

        val mapsActivity = activity
        if (mapsActivity is MapsActivity) {
            setupMarker(mapsActivity.map, mapsActivity)
            mapsActivity.map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    binding.vm?.locationLatLng?.value = marker.position
                }
            })
        }

        return binding.root
    }

    private fun setupMarker(map: GoogleMap, context: Context) {
        val mapCenter = map.projection.visibleRegion.latLngBounds.center
        val icon = IconHelper(context).getIcon("Hold and drag me", locationType.icon)
        newLocationMarker = map.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(mapCenter)
                .draggable(true)
        )
        binding.vm?.locationLatLng?.value = mapCenter
    }
}