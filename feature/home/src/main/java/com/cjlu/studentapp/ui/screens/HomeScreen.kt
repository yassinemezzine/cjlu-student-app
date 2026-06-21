package com.cjlu.studentapp.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.cjlu.core.resources.R
import com.cjlu.studentapp.data.RequestFilters
import com.cjlu.studentapp.data.RequestStatus
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.ui.theme.CJLUStudentAppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    studentId: String = "",
    studentName: String = "",
    activeRequests: List<StudentRequest> = emptyList(),
    serviceItems: List<ServiceItem> = emptyList(),
    overallAttendancePercent: Int = 96,
    classUpdateNotice: String? = null,
    refreshNonce: Int = 0,
    onRefresh: () -> Unit = {},
    onServiceSelected: (String) -> Unit = {},
    onAttendanceClick: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val appLocale = remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }
    val datePattern = stringResource(R.string.home_date_pattern)
    val headerSubtitle = remember(appLocale, datePattern) {
        LocalDate.now().format(DateTimeFormatter.ofPattern(datePattern, appLocale))
    }
    val activeRequestCount = activeRequests.count { it.status != RequestStatus.Completed }
    val requestCountByService = remember(activeRequests) { RequestFilters.countByService(activeRequests) }
    val nextClassLine = remember(studentId, refreshNonce) {
        null
    }
    val servicesWithActiveRequests = remember(activeRequests, serviceItems) {
        serviceItems.filter { requestCountByService[it.id] != null }
    }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val triggerRefresh = {
        isRefreshing = true
        onRefresh()
        isRefreshing = false
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = triggerRefresh,
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HomeHeader(
                    title = if (studentName.isNotBlank()) {
                        stringResource(R.string.home_welcome, studentName)
                    } else {
                        stringResource(R.string.home_title)
                    },
                    subtitle = headerSubtitle,
                    onRefresh = triggerRefresh,
                )
            }

            item {
                HeroCard(
                    nextClassLine = nextClassLine,
                ) {
                    onServiceSelected("class_schedule")
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_metric_attendance_title),
                        value = "${overallAttendancePercent}%",
                        detail = stringResource(R.string.home_metric_attendance_detail),
                        valueColor = if (overallAttendancePercent < 75) {
                            Color(0xFFC62828)
                        } else {
                            null
                        },
                        onClick = onAttendanceClick,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_metric_requests_title),
                        value = activeRequestCount.toString(),
                        detail = stringResource(R.string.home_metric_requests_detail)
                    )
                }
            }

            if (!classUpdateNotice.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.home_schedule_notice_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = classUpdateNotice.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.home_section_quick_actions_title),
                    subtitle = stringResource(R.string.home_section_quick_actions_subtitle)
                )
            }

            item {
                QuickActionGrid(onServiceSelected = onServiceSelected)
            }

            if (servicesWithActiveRequests.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = stringResource(R.string.home_section_active_title),
                        subtitle = stringResource(R.string.home_section_active_subtitle),
                    )
                }
                items(servicesWithActiveRequests, key = { "active-${it.id}" }) { service ->
                    ServiceDashboardTile(
                        service = service,
                        activeCount = requestCountByService[service.id] ?: 0,
                        appLocale = appLocale,
                        onClick = { onServiceSelected(service.id) },
                    )
                }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.home_section_dashboard_title),
                    subtitle = stringResource(R.string.home_section_dashboard_subtitle),
                )
            }

            ServiceCatalog.categories.forEach { category ->
                val categoryServices = serviceItems.filter { it.category == category }
                if (categoryServices.isNotEmpty()) {
                    item(key = "header-${category.name}") {
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    item(key = "grid-${category.name}") {
                        ServiceDashboardGrid(
                            services = categoryServices,
                            requestCountByService = requestCountByService,
                            appLocale = appLocale,
                            onServiceSelected = onServiceSelected,
                        )
                    }
                }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.home_section_attention_title),
                    subtitle = stringResource(R.string.home_section_attention_subtitle)
                )
            }

            item {
                AttentionCard(
                    title = stringResource(R.string.home_attention_title),
                    detail = stringResource(R.string.home_attention_detail),
                    onClick = { onServiceSelected("back_to_cjlu") }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(title: String, subtitle: String, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.home_action_refresh),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onRefresh() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                modifier = Modifier.padding(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HeroCard(
    nextClassLine: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F6FFF),
                        Color(0xFF2D9CDB),
                        Color(0xFF25B8A8)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.home_hero_overline),
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.home_hero_body),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nextClassLine ?: stringResource(R.string.home_next_class),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    detail: String,
    valueColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionGrid(onServiceSelected: (String) -> Unit) {
    val actions = quickActionItems()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { action ->
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        item = action,
                        onClick = { onServiceSelected(action.serviceId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    item: QuickActionItem,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(item.titleRes),
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(item.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_action_open),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ServiceDashboardGrid(
    services: List<ServiceItem>,
    requestCountByService: Map<String, Int>,
    appLocale: Locale,
    onServiceSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        services.chunked(2).forEach { rowServices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowServices.forEach { service ->
                    ServiceDashboardTile(
                        modifier = Modifier.weight(1f),
                        service = service,
                        activeCount = requestCountByService[service.id] ?: 0,
                        appLocale = appLocale,
                        onClick = { onServiceSelected(service.id) },
                    )
                }
                if (rowServices.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ServiceDashboardTile(
    modifier: Modifier = Modifier,
    service: ServiceItem,
    activeCount: Int,
    appLocale: Locale,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        imageVector = service.category.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (activeCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.home_service_requests_badge, activeCount),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Text(
                text = service.resolveTitle(context, appLocale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AttentionCard(
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_action_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun quickActionItems(): List<QuickActionItem> {
    return listOf(
        QuickActionItem(
            titleRes = R.string.quick_action_leave_title,
            subtitleRes = R.string.quick_action_leave_subtitle,
            icon = Icons.Filled.Description,
            serviceId = "ask_leave"
        ),
        QuickActionItem(
            titleRes = R.string.quick_action_calendar_title,
            subtitleRes = R.string.quick_action_calendar_subtitle,
            icon = Icons.Filled.CalendarToday,
            serviceId = "school_calendar"
        ),
        QuickActionItem(
            titleRes = R.string.quick_action_schedule_title,
            subtitleRes = R.string.quick_action_schedule_subtitle,
            icon = Icons.Filled.Schedule,
            serviceId = "class_schedule"
        ),
        QuickActionItem(
            titleRes = R.string.quick_action_repair_title,
            subtitleRes = R.string.quick_action_repair_subtitle,
            icon = Icons.Filled.Construction,
            serviceId = "repair_request"
        )
    )
}

private data class QuickActionItem(
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val serviceId: String
)


@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    CJLUStudentAppTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen()
    }
}
