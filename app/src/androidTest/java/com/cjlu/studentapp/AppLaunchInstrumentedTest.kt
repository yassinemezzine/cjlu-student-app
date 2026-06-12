package com.cjlu.studentapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchInstrumentedTest {

    @Test
    fun mainActivity_launchesWithoutCrashing() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertNotNull(context.packageName)
        }
    }
}
