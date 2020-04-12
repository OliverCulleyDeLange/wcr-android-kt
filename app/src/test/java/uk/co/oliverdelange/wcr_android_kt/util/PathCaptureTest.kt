package uk.co.oliverdelange.wcr_android_kt.util

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

data class Thing(val name:String)

@ExtendWith(MockKExtension::class)
internal class PathCaptureTest {

    @MockK
    lateinit var thing: Thing

    @Before
    fun setUp() {
        println("Before Each")
    }

    @AfterEach
    fun tearDown() {
        println("After Each")
    }

    @Test
    fun undoAction() {
        println("undoAction Test")
        every { thing.name } returns "Blah"
        assertThat(thing.name).isEqualTo("Blahh")
    }

    @Test
    fun endAction() {
    }

    @Test
    fun reset() {
    }

    @Test
    fun moveTo() {
    }

    @Test
    fun quadTo() {
    }

    @Test
    fun lineTo() {
    }
}