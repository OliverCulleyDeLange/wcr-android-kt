package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_MASK
import android.view.View
import android.view.ViewGroup
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.BottomSheetBinding
import uk.co.oliverdelange.wcr_android_kt.databinding.RouteCardBinding
import uk.co.oliverdelange.wcr_android_kt.databinding.TopoCardBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.Route
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.ui.map.ToposFragment.RouteRecyclerAdapter.ViewHolder
import javax.inject.Inject

class ToposFragment : Fragment(), Injectable {
    companion object {
        fun newToposFragment(): ToposFragment {
            return ToposFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var binding: BottomSheetBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = BottomSheetBinding.inflate(layoutInflater, container, false)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)
        binding?.vm = viewModel
        binding?.setLifecycleOwner(this)
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding?.topoRecycler?.layoutManager = DeScrollLinearLayoutManager(activity)
        val recyclerAdapter = TopoRecyclerAdapter(activity)
        binding?.topoRecycler?.adapter = recyclerAdapter
        binding?.vm?.topos?.observe(this, Observer {
            recyclerAdapter.updateTopos(it ?: emptyList())
            binding?.executePendingBindings()
            binding?.topoRecycler?.scrollToPosition(0)
        })
    }

    inner class TopoRecyclerAdapter(val activity: FragmentActivity?) : RecyclerView.Adapter<TopoRecyclerAdapter.ViewHolder>() {

        var topos: List<TopoAndRoutes> = emptyList()

        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val routeBinding: TopoCardBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.topo_card, parent, false)
            routeBinding.routeRecycler.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            LinearSnapHelper().attachToRecyclerView(routeBinding.routeRecycler)
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
            val topoAndRoutes = topos.get(position)
            holder.binding.topo = topoAndRoutes.topo
            holder.binding.topoImage.routes.addAll(topoAndRoutes.routes)
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
                        val rtn = topos.get(oldItemPosition).topo.id == newTopos.get(newItemPosition).topo.id
                        return rtn
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val newData = newTopos.get(newItemPosition)
                        val oldData = topos.get(oldItemPosition)
                        return (newData.topo.id == oldData.topo.id &&
                                newData.routes.size == oldData.routes.size)
                    }
                })
                topos = newTopos
                result.dispatchUpdatesTo(this)
            }
        }

        inner class ViewHolder(var binding: TopoCardBinding) : RecyclerView.ViewHolder(binding.root)
    }

    inner class RouteRecyclerAdapter(val routes: List<Route>) : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding: RouteCardBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.route_card, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return routes.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val route = routes.get(position)
            holder.binding.route = route
        }

        inner class ViewHolder(var binding: RouteCardBinding) : RecyclerView.ViewHolder(binding.root)
    }
}

class DeScrollLinearLayoutManager(activity: FragmentActivity?) : LinearLayoutManager(activity) {
    var enableScroll = true
    override fun canScrollVertically(): Boolean {
        return enableScroll && super.canScrollVertically()
    }
}