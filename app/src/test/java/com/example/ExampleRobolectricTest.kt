package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ThesisTaskTag
import com.example.data.model.TreeSpecies
import com.example.service.FocusTimerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("Thesis Grove", appName)
  }

  @Test
  fun `timer engine updates duration and task category correctly`() {
    FocusTimerEngine.selectDuration(45)
    assertEquals(45 * 60, FocusTimerEngine.timerState.value.totalDurationSeconds)
    assertEquals(45 * 60, FocusTimerEngine.timerState.value.remainingSeconds)

    FocusTimerEngine.selectTaskTag(ThesisTaskTag.DATA_ANALYSIS)
    assertEquals(ThesisTaskTag.DATA_ANALYSIS, FocusTimerEngine.timerState.value.selectedTaskTag)

    FocusTimerEngine.selectTreeSpecies(TreeSpecies.SCHOLARS_ANCIENT_OAK)
    assertEquals(TreeSpecies.SCHOLARS_ANCIENT_OAK, FocusTimerEngine.timerState.value.selectedTreeSpecies)
  }
}

