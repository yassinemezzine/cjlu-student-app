package com.cjlu.studentapp

import android.app.NotificationManager
import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cjlu.studentapp.notifications.CjluNotificationHelper
import com.cjlu.studentapp.widget.CjluAppWidgetReceiver
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke-tests Phase-1 Android features (widget + notifications) without UI automation.
 */
@RunWith(AndroidJUnit4::class)
class AppFeaturesInstrumentedTest {

    @Test
    fun glanceWidgetReceiver_isRegistered() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getReceiverInfo(
            ComponentName(context, CjluAppWidgetReceiver::class.java),
            0,
        )
        assertNotNull(info)
    }

    @Test
    fun notificationChannels_canBeCreated() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        CjluNotificationHelper.ensureChannels(context)
        val nm = context.getSystemService(NotificationManager::class.java)!!
        assertTrue(nm.notificationChannels.isNotEmpty())
    }
}
