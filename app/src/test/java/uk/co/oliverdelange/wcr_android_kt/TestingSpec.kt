package uk.co.oliverdelange.wcr_android_kt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import javax.inject.Singleton

// This is just here to play around with a basic version of the ViewModel tests when things go wrong.
class TestingSpec : StringSpec() {
    lateinit var vm: MyViewModel

    lateinit var mockRepo: MyRepository
    lateinit var mockApp: Application

    init {
        listener(InstantExecutorListener())

        beforeTest {
            mockApp = mockk()
            mockRepo = mockk()
            vm = MyViewModel(mockApp,mockRepo)
            println("AARRRRGGGGGGGG")
//            throw ExceptionInInitializerError("Fail") // This makes everything go haywire
//            throw RuntimeException("Fail") // This is fine
        }

        "test live data updated when doThing" {
            vm.data.value shouldBe "Loading..."
            val expected = "Something specific"
            every { mockRepo.getStuff() } returns expected
            vm.doThing()
            vm.data.value shouldBe expected

//            true shouldBe false
        }
    }
}

@Singleton
class MyViewModel @Inject constructor(app: Application, private val repo: MyRepository) : AndroidViewModel(app) {
    private val _data = MutableLiveData<String>().apply { value = "Loading..." }

    val data: LiveData<String>
        get() = _data

    fun doThing() {
        _data.postValue(repo.getStuff())
    }
}

class MyRepository {
    fun getStuff(): String {
        return "A thing"
    }
}