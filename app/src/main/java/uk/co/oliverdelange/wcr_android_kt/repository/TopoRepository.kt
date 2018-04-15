package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.TopoDao
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import uk.co.oliverdelange.wcr_android_kt.model.TopoAndRoutes
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject

class TopoRepository @Inject constructor(val topoDao: TopoDao, val appExecutors: AppExecutors) {

    fun save(topo: Topo): LiveData<Long> {
        val result = MutableLiveData<Long>()
        appExecutors.diskIO().execute {
            Timber.d("Saving topo: %s", topo)
            val topoId = topoDao.save(topo)
            appExecutors.mainThread().execute({ result.value = topoId })
        }
        return result
    }

    fun getToposForLocation(locationId: Long): LiveData<List<TopoAndRoutes>> {
        return topoDao.loadTopoAndRoutes(locationId)
    }
}