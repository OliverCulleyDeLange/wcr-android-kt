package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_view_topos.*
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.databinding.FragmentViewToposBinding
import uk.co.oliverdelange.wcr_android_kt.databinding.TopoCardBinding
import uk.co.oliverdelange.wcr_android_kt.di.Injectable
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.ui.map.RecyclerAdapter.ViewHolder
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)

        topo_recycler.layoutManager = LinearLayoutManager(activity)
        val recyclerAdapter = RecyclerAdapter()
        topo_recycler.adapter = recyclerAdapter
        viewModel.topos.observe(this, Observer {
            it?.let {
                recyclerAdapter.updateTopos(it)
                binding.executePendingBindings()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentViewToposBinding.inflate(layoutInflater, container, false)
        binding.setLifecycleOwner(this)
        return binding.root
    }
}

class RecyclerAdapter : RecyclerView.Adapter<ViewHolder>() {

    var topos: List<TopoAndRoutes> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: TopoCardBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.topo_card, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return topos.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topoAndRoutes = topos.get(position)
        holder.binding.vm = topoAndRoutes
        holder.binding.executePendingBindings()
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

    class ViewHolder(var binding: TopoCardBinding) : RecyclerView.ViewHolder(binding.root)
}