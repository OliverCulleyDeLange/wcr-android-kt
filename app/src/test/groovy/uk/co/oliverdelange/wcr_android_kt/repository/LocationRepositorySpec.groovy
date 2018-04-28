package uk.co.oliverdelange.wcr_android_kt.repository

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import spock.lang.Specification
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao
import uk.co.oliverdelange.wcr_android_kt.model.Location
import uk.co.oliverdelange.wcr_android_kt.model.LocationType
import uk.co.oliverdelange.wcr_android_kt.testutil.InstantExecutors

class LocationRepositorySpec extends Specification {

    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule()

    LocationRepository locationRepository

    LocationDao mockLocationDao = Mock(LocationDao)

    void setup() {
        locationRepository = new LocationRepository(mockLocationDao, new InstantExecutors())
    }

    def "save() should save Location to DB"() {
        given:
        def location = new Location("name", 52, -2, LocationType.CRAG, 0, 0, 0, 0, 0, 0, 0)

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
