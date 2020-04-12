package uk.co.oliverdelange.wcr_android_kt.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import org.junit.Rule
import spock.lang.Specification
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.LocationDao
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType

class LocationRepositorySpec extends Specification {

    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule()

    LocationRepository locationRepository

    LocationDao mockLocationDao = Mock(LocationDao)

    void setup() {
        locationRepository = new LocationRepository(mockLocationDao)
    }

    def "save() should save Location to DB"() {
        given:
        def location = new Location("id", "parentId", "name", new LatLng(52l, 51l), LocationType.CRAG, "uploaderid")

        when:
        locationRepository.save(location)
        then:
        1 * mockLocationDao.insert(location)
    }

    def "load() should load crags from DB"() {
        given:

        when:
        locationRepository.loadCrags()

        then:
        1 * mockLocationDao.load(LocationType.CRAG)
    }
}
