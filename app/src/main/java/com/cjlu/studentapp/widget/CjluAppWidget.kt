package com.cjlu.studentapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import com.cjlu.studentapp.MainActivity
import com.cjlu.core.resources.R
import com.cjlu.studentapp.prefs.WidgetStatsStore

class CjluAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences(WidgetStatsStore.PREFS_NAME, Context.MODE_PRIVATE)
        val unread = prefs.getInt(WidgetStatsStore.KEY_UNREAD_MESSAGE_COUNT, 0)
        val active = prefs.getInt(WidgetStatsStore.KEY_ACTIVE_REQUEST_COUNT, 0)

        provideContent {
            GlanceTheme(colors = ColorProviders(lightColorScheme())) {
                CjluWidgetContent(unread = unread, activeRequests = active)
            }
        }
    }
}

private fun openMainActivityIntent(context: Context, tab: String? = null): Intent {
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (tab != null) {
            putExtra(CjluWidget.EXTRA_OPEN_TAB, tab)
        }
    }
}

@Composable
private fun CjluWidgetContent(unread: Int, activeRequests: Int) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = TextStyle(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(text = context.getString(R.string.widget_unread_line, unread))
        Text(text = context.getString(R.string.widget_requests_line, activeRequests))
        Spacer(modifier = GlanceModifier.height(12.dp))
        androidx.glance.Button(
            text = context.getString(R.string.nav_messages),
            onClick = actionStartActivity(openMainActivityIntent(context, CjluWidget.TAB_MESSAGES)),
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        androidx.glance.Button(
            text = context.getString(R.string.widget_open_app),
            onClick = actionStartActivity(openMainActivityIntent(context, CjluWidget.TAB_HOME)),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

class CjluAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CjluAppWidget()
}
