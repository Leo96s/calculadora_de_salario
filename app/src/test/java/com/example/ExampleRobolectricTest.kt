package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  // MainViewModel toca AuthManager/FirebaseManager já no construtor
  // (initSession); sem isto crasha com "Default FirebaseApp is not
  // initialized" antes de qualquer teste correr.
  @Before
  fun setUpFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("1:0:android:test")
          .setApiKey("test-api-key")
          .setProjectId("test-project")
          .build()
      )
    }
  }

  @Test
  fun testStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Calculadora de Salário", appName)
  }

  // Nota: não testa viewModel.register()/login() aqui — chamam o Firebase
  // Auth real (rede), o que não é viável nem apropriado num teste unitário
  // sem isolar AuthManager atrás de uma interface injetável e substituí-la
  // por um fake (mudança maior, não feita nesta correção — ver
  // docs/bugfix-plan.md item 3.1).
  @Test
  fun testViewModelDefaultCalculationState() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)

    // Test default initial state
    assertEquals("22", viewModel.days8hInput.value)
    assertEquals("0", viewModel.days4hInput.value)
  }
}

