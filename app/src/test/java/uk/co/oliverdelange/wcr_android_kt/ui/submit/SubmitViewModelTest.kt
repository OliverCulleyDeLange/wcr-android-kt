package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.View
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class SubmitViewModelTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var submitViewModel: SubmitViewModel
    private val mockView = mock<View> {}

    @Before
    fun setUp() {
        submitViewModel = SubmitViewModel()
    }

    @Test
    fun submitShouldSetErrorIfCragNameBlank() {
        submitViewModel.cragName.value = null
        submitViewModel.cragNameError.value = null

        submitViewModel.submit(mockView)

        assertEquals(submitViewModel.cragNameError.value, "Can not be empty")
    }

    @Test
    fun submitShouldResetErrorIfCragNameAvailable() {
        submitViewModel.cragName.value = "cragname"
        submitViewModel.cragNameError.value = "Error message"

        submitViewModel.submit(mockView)

        assertNull(submitViewModel.cragNameError.value)
    }
}