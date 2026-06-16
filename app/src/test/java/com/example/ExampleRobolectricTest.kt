package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Daily", appName)
  }

  @Test
  fun `test overall monthly budget updates in FinanceViewModel`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.FinanceViewModel(application)
    
    // Verify default value is 0.0 or any previously saved configuration (we can reset/verify it)
    viewModel.updateOverallMonthlyBudget(0.0)
    assertEquals(0.0, viewModel.overallMonthlyBudget.value, 0.001)

    // Update to 1500.25
    viewModel.updateOverallMonthlyBudget(1500.25)

    // Verify updated value
    assertEquals(1500.25, viewModel.overallMonthlyBudget.value, 0.001)

    // Clear it
    viewModel.updateOverallMonthlyBudget(0.0)
    assertEquals(0.0, viewModel.overallMonthlyBudget.value, 0.001)
  }
}
