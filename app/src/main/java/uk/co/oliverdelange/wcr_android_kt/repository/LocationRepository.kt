package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.model.*
import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors
import javax.inject.Inject
import kotlin.concurrent.thread

class LocationRepository @Inject constructor(val locationDao: LocationDao,
                                             val appExecutors: AppExecutors) {

    fun save(location: Location): LiveData<Long> {
        val result = MutableLiveData<Long>()
        appExecutors.diskIO().execute {
            Timber.d("Saving location: %s", location)
            val locationId = locationDao.insert(location)
            appExecutors.mainThread().execute({ result.value = locationId })

            //TODO Tidy up. Inject etc
            val credentials = AWSMobileClient.getInstance().credentialsProvider
            val config = AWSMobileClient.getInstance().configuration
            val ddbClient = AmazonDynamoDBClient(credentials)
            ddbClient.setRegion(Region.getRegion(Regions.EU_WEST_2))
            val dynamoDBMapper = DynamoDBMapper.builder()
                    .awsConfiguration(config)
                    .dynamoDBClient(ddbClient)
                    .build()

            thread(start = true) {
                //TODO Look at the ID clash scenario - is it possible?
                dynamoDBMapper.save(get(locationId))
            }
        }
        return result
    }

    fun load(selectedLocationId: Long): LiveData<Location> {
        return locationDao.load(selectedLocationId)
    }

    fun get(locationId: Long): Location? {
        return locationDao.get(locationId)
    }

    fun loadCrags(): LiveData<List<Location>> {
        return locationDao.load(LocationType.CRAG)
    }

    fun loadSectorsFor(cragId: Long): LiveData<List<Location>> {
        return locationDao.loadWithParentId(cragId)
    }

    fun updateLocationRouteInfo(toposAndRoutes: List<TopoAndRoutes>, locationId: Long) {
        var boulders = 0
        var sports = 0
        var trads = 0
        var greens = 0
        var oranges = 0
        var reds = 0
        var blacks = 0
        for (topoAndRoute in toposAndRoutes) {
            for (route in topoAndRoute.routes) {
                when (route.type) {
                    RouteType.BOULDERING -> boulders++
                    RouteType.SPORT -> sports++
                    RouteType.TRAD -> trads++
                }
                when (route.grade?.colour) {
                    GradeColour.GREEN -> greens++
                    GradeColour.ORANGE -> oranges++
                    GradeColour.RED -> reds++
                    GradeColour.BLACK -> blacks++
                }
            }
        }
        locationDao.updateRouteInfo(locationId, boulders, sports, trads, greens, oranges, reds, blacks)
    }

    fun search(query: String): LiveData<List<Location>> {
        return locationDao.searchOnName("%$query%")
    }
}