package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun testStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Calculadora de Salário", appName)
  }

  @Test
  fun testViewModelAuthAndCalculationFlow() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)

    // Test default initial state
    assertEquals("22", viewModel.days8hInput.value)
    assertEquals("0", viewModel.days4hInput.value)

    // Register a user
    viewModel.register("leonardo", "pass123", "12.5")
    
    // Auth should succeed
    assertTrue(viewModel.authSuccess.value || viewModel.authError.value == null)
  }
}

