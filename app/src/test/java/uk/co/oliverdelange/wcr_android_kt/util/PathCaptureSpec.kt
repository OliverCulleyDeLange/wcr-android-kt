package uk.co.oliverdelange.wcr_android_kt.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK

data class Thing(val name: String)

// https://github.com/kotest/kotest/issues/583
class PathCaptureSpec : FreeSpec() {
    @MockK
    lateinit var thing: Thing
    init {
        beforeTest {
            MockKAnnotations.init(this)
        }
        "Test something"{
            every { thing.name } returns "Blah"
            thing.name shouldBe "Blahh"
        }
    }
}