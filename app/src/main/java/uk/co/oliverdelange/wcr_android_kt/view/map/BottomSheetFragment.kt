package uk.co.oliverdelange.wcr_android_kt.view.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_MASK
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import kotlinx.android.synthetic.main.fragment_bottom_sheet.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentBottomSheetBinding
import uk.co.oliverdelange.wcr_android_kt.databinding.LayoutRouteCardBinding
import uk.co.oliverdelange.wcr_android_kt.databinding.LayoutTopoCardBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.model.flattened
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel
import uk.co.oliverdelange.wcr_android_kt.viewmodel.ScrollToTopo
import javax.inject.Inject

/*
    A fragment which fills the bottom sheet on the main MapsActivity
    It gives information about the selected location, including a list of topos
 */
class BottomSheetFragment : Fragment(), Injectable {
    companion object {
        fun newBottomSheet(): BottomSheetFragment {
            Timber.v("Creating BottomSheetFragment")
            return BottomSheetFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var binding: FragmentBottomSheetBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.v("BottomSheetFragment: onCreateView")
        binding = FragmentBottomSheetBinding.inflate(layoutInflater, container, false)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)
        binding?.vm = viewModel
        binding?.lifecycleOwner = this
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.v("BottomSheetFragment: onActivityCreated")
        val deScrollLinearLayoutManager = DeScrollLinearLayoutManager(activity)
        binding?.topoRecycler?.layoutManager = deScrollLinearLayoutManager
        val recyclerAdapter = TopoRecyclerAdapter(activity, deScrollLinearLayoutManager)
        binding?.topoRecycler?.adapter = recyclerAdapter

        sign_in_button.setOnClickListener {
            binding?.vm?.onClickSignInButton()
        }

        binding?.vm?.topos?.observe(viewLifecycleOwner, Observer { topos ->
            Timber.d("topos changed, new topos: %s", topos?.map { it.topo.name })
            recyclerAdapter.updateTopos(topos ?: emptyList())
        })

        //FIXME THIS WON'T WORK AS THE ACTIVITY SWALLOWS EVENTS. Need a better mechanism for one time events - multi subscriber...
        binding?.vm?.viewEvents?.observe(viewLifecycleOwner, Observer {event->
            if (event is ScrollToTopo) {
                val topoId = event.topoId
                Timber.v("ScrollToTopo: $topoId")
                val position = recyclerAdapter.topos.indexOfFirst { it.topo.id == topoId }
                if (position != -1) {
                    Timber.d("Selecting the right topo in the list: $topoId")
                    binding?.topoRecycler?.scrollToPosition(position)
                } else {
                    Timber.w("Couldn't find topo with id $topoId in topos. Selecting first")
                    binding?.topoRecycler?.scrollToPosition(0)
                }
            }
        })
    }
}

class TopoRecyclerAdapter(val activity: FragmentActivity?, private val recyclerLayoutManager: DeScrollLinearLayoutManager) : RecyclerView.Adapter<TopoRecyclerAdapter.ViewHolder>() {

    var topos: List<TopoAndRoutes> = emptyList()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.v("TopoRecyclerAdapter:onCreateViewHolder ... Creating the topo view")
        val topoCardBinding: LayoutTopoCardBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_topo_card, parent, false)
        topoCardBinding.routeRecycler.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        LinearSnapHelper().attachToRecyclerView(topoCardBinding.routeRecycler)
        topoCardBinding.routeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    val selectedRoutePosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                    if (selectedRoutePosition != NO_POSITION) {
                        val route = (topoCardBinding.routeRecycler.adapter as RouteRecyclerAdapter).routes[selectedRoutePosition]
                        topoCardBinding.topoImage.selectedRoute = route
                    }
                }
            }
        })
        // Enable two finger gestures on topo images
        topoCardBinding.topoImage.setOnTouchListener { _, event ->
            if (event.pointerCount > 1) {
//                    Timber.d(MotionEvent.actionToString(event.action))
                if (event.action and ACTION_MASK == MotionEvent.ACTION_POINTER_DOWN) {
                    recyclerLayoutManager.enableScroll = false
                } else if (event.action and ACTION_MASK == MotionEvent.ACTION_POINTER_UP) {
                    recyclerLayoutManager.enableScroll = true
                }
            }
            false
        }
        return ViewHolder(topoCardBinding)
    }

    override fun getItemCount(): Int {
        return topos.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Timber.v("TopoRecyclerAdapter:onBindViewHolder ... Binding data to topo view: topo name = ${topos[position].topo.name}")
        val topoAndRoutes = topos[position]
        holder.binding.topo = topoAndRoutes.topo
        holder.binding.topoImage.routes = topoAndRoutes.routes
        holder.binding.routeRecycler.adapter = RouteRecyclerAdapter(topoAndRoutes.routes)
    }

    fun updateTopos(newTopos: List<TopoAndRoutes>) {
        if (topos.isEmpty()) {
            topos = newTopos
            notifyItemRangeInserted(0, newTopos.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return topos.size
                }

                override fun getNewListSize(): Int {
                    return newTopos.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val rtn = topos[oldItemPosition].topo.id == newTopos[newItemPosition].topo.id
                    return rtn
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val newData = newTopos[newItemPosition]
                    val oldData = topos[oldItemPosition]
                    return (newData.topo.id == oldData.topo.id &&
                            newData.routes.size == oldData.routes.size)
                }
            })
            topos = newTopos
            result.dispatchUpdatesTo(this)
        }
    }

    inner class ViewHolder(var binding: LayoutTopoCardBinding) : RecyclerView.ViewHolder(binding.root)
}

class RouteRecyclerAdapter(var routes: List<Route>) : RecyclerView.Adapter<RouteRecyclerAdapter.ViewHolder>() {
    init {
        routes = routes.sortedBy { r -> r.path?.flattened()?.firstOrNull()?.first }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.v("RouteRecyclerAdapter:onCreateViewHolder ... Creating a Route view ")
        val binding: LayoutRouteCardBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_route_card, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return routes.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Timber.v("RouteRecyclerAdapter:onBindViewHolder ... Binding route view data: route name = ${routes[position].name} ")
        val route = routes[position]
        holder.binding.route = route
    }

    inner class ViewHolder(var binding: LayoutRouteCardBinding) : RecyclerView.ViewHolder(binding.root)
}

class DeScrollLinearLayoutManager(activity: FragmentActivity?) : LinearLayoutManager(activity) {
    var enableScroll = true
    override fun canScrollVertically(): Boolean {
        return enableScroll && super.canScrollVertically()
    }
}