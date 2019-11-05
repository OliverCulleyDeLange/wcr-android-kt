package uk.co.oliverdelange.wcr_android_kt.view.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.view.MotionEvent.ACTION_MASK
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
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentBottomSheetBinding
import uk.co.oliverdelange.wcr_android_kt.databinding.LayoutRouteCardBinding
import uk.co.oliverdelange.wcr_android_kt.databinding.LayoutTopoCardBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.view.map.BottomSheetFragment.RouteRecyclerAdapter.ViewHolder
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel
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
        binding?.topoRecycler?.layoutManager = DeScrollLinearLayoutManager(activity)
        val recyclerAdapter = TopoRecyclerAdapter(activity)
        binding?.topoRecycler?.adapter = recyclerAdapter

        binding?.vm?.topos?.observe(this, Observer { topos ->
            Timber.d("topos changed, new topos: %s", topos?.map { it.topo.name })
            recyclerAdapter.updateTopos(topos ?: emptyList())
//            binding?.executePendingBindings()

            binding?.topoRecycler?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    Timber.v("topoRecycler : onGlobalLayout")
                    binding?.vm?.selectedTopoId?.value?.let { selectedTopoId ->
                        val position = recyclerAdapter.topos.indexOfFirst { it.topo.id == selectedTopoId }
                        if (position != -1) {
                            Timber.d("Selecting the right topo in the list: $selectedTopoId")
                            binding?.topoRecycler?.scrollToPosition(position)
                        } else {
                            Timber.w("Couldn't find topo with id $selectedTopoId in topos. Selecting first")
                            binding?.topoRecycler?.scrollToPosition(0)
                        }
                        //Reset after we've scrolled to it so we don't scroll again after
                        binding?.vm?.selectedTopoId?.value = null

                    }
                    binding?.topoRecycler?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            })
        })
    }

    inner class TopoRecyclerAdapter(val activity: FragmentActivity?) : RecyclerView.Adapter<TopoRecyclerAdapter.ViewHolder>() {

        var topos: List<TopoAndRoutes> = emptyList()

        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            Timber.v("TopoRecyclerAdapter:onCreateViewHolder ... Creating the topo view")
            val routeBinding: LayoutTopoCardBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.layout_topo_card, parent, false)
            routeBinding.routeRecycler.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            LinearSnapHelper().attachToRecyclerView(routeBinding.routeRecycler)
            routeBinding.routeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == SCROLL_STATE_IDLE) {
                        val selectedRoutePosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                        if (selectedRoutePosition != NO_POSITION) {
                            val route = (routeBinding.routeRecycler.adapter as RouteRecyclerAdapter).routes[selectedRoutePosition]
                            routeBinding.topoImage.selectedRoute = route
                        }
                    }
                }
            })
            // Enable two finger gestures on topo images
            routeBinding.topoImage.setOnTouchListener { _, event ->
                if (event.pointerCount > 1) {
//                    Timber.d(MotionEvent.actionToString(event.action))
                    if (event.action and ACTION_MASK == MotionEvent.ACTION_POINTER_DOWN) {
                        (binding?.topoRecycler?.layoutManager as DeScrollLinearLayoutManager).enableScroll = false
                    } else if (event.action and ACTION_MASK == MotionEvent.ACTION_POINTER_UP) {
                        (binding?.topoRecycler?.layoutManager as DeScrollLinearLayoutManager).enableScroll = true
                    }
                }
                false
            }
            return ViewHolder(routeBinding)
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

    inner class RouteRecyclerAdapter(val routes: List<Route>) : RecyclerView.Adapter<ViewHolder>() {

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
}

class DeScrollLinearLayoutManager(activity: FragmentActivity?) : LinearLayoutManager(activity) {
    var enableScroll = true
    override fun canScrollVertically(): Boolean {
        return enableScroll && super.canScrollVertically()
    }
}