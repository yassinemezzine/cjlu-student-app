package com.cjlu.studentapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.cjlu.core.resources.R
import com.cjlu.studentapp.data.RequestFilters
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.ui.components.RequestSummaryCard
import com.cjlu.studentapp.ui.components.RequestStatusBadge
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.network.api.StudentAttendanceDetailDto
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val readOnlyServiceIds = setOf(
    "attendance_rate",
    "transcripts",
    "class_schedule",
)

private val requestServiceIds = setOf(
    "changing_room",
    "ask_leave",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHubScreen(
    serviceId: String,
    serviceItems: List<ServiceItem>,
    requests: List<StudentRequest>,
    onNavigateToSubmit: (String?) -> Unit,
    onBack: () -> Unit,
    studentId: String = "",
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val appLocale = remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }
    val requestDatePattern = stringResource(R.string.request_date_pattern)
    val requestDateFormatter = remember(appLocale, requestDatePattern) {
        DateTimeFormatter.ofPattern(requestDatePattern, appLocale)
    }

    val service = remember(serviceId, serviceItems) {
        serviceItems.firstOrNull { it.id == serviceId }
    }
    val serviceRequests = remember(requests, serviceId) {
        RequestFilters.forService(requests, serviceId)
    }
    val hasForm = remember(serviceId) { ServiceFormCatalog.find(serviceId) != null }
    val isReadOnly = serviceId in readOnlyServiceIds
    val isRequestService = serviceId in requestServiceIds

    var selectedTab by rememberSaveable(serviceId) { mutableIntStateOf(if (isReadOnly) 1 else 0) }
    var selectedRequestForDetail by remember { mutableStateOf<StudentRequest?>(null) }

    if (service == null) {
        MissingServiceScreen(onBack = onBack)
        return
    }

    val applyTabLabel = when {
        isReadOnly -> stringResource(R.string.service_hub_tab_view)
        isRequestService || hasForm -> stringResource(R.string.service_hub_tab_apply)
        else -> stringResource(R.string.service_hub_tab_info)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = service.resolveTitle(context, appLocale),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(service.category.labelRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.service_hub_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(
                                R.string.service_hub_tab_requests,
                                serviceRequests.size,
                            ),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(text = applyTabLabel) },
                )
            }

            when (selectedTab) {
                0 -> ServiceRequestsTab(
                    service = service,
                    serviceRequests = serviceRequests,
                    requestDateFormatter = requestDateFormatter,
                    hasForm = hasForm,
                    isReadOnly = isReadOnly,
                    onStartApply = {
                        selectedTab = 1
                        onNavigateToSubmit(null)
                    },
                    onRequestClick = { selectedRequestForDetail = it }
                )
                1 -> ServiceApplyTab(
                    service = service,
                    appLocale = appLocale,
                    hasForm = hasForm,
                    isReadOnly = isReadOnly,
                    isRequestService = isRequestService,
                    studentId = studentId,
                    onNavigateToSubmit = onNavigateToSubmit,
                )
            }
        }
    }

    selectedRequestForDetail?.let { req ->
        RequestDetailDialog(
            request = req,
            serviceTitle = service.resolveTitle(context, appLocale),
            dateText = req.createdAtMillis.toRequestDateTime(appLocale),
            appLocale = appLocale,
            onDismiss = { selectedRequestForDetail = null }
        )
    }
}

@Composable
private fun ServiceRequestsTab(
    service: ServiceItem,
    serviceRequests: List<StudentRequest>,
    requestDateFormatter: DateTimeFormatter,
    hasForm: Boolean,
    isReadOnly: Boolean,
    onStartApply: () -> Unit,
    onRequestClick: (StudentRequest) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val appLocale = remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }

    if (serviceRequests.isEmpty()) {
        EmptyServiceRequestsCard(
            hasForm = hasForm,
            isReadOnly = isReadOnly,
            onStartApply = onStartApply,
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.service_hub_requests_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(serviceRequests, key = { it.id }) { request ->
            RequestSummaryCard(
                request = request,
                supportingText = "${request.id} • ${request.createdAtMillis.toRequestDateTime(appLocale)}",
                onClick = { onRequestClick(request) },
                serviceItems = listOf(service),
            )
        }
    }
}

@Composable
private fun EmptyServiceRequestsCard(
    hasForm: Boolean,
    isReadOnly: Boolean,
    onStartApply: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.service_hub_requests_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.service_hub_requests_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasForm || isReadOnly) {
                Button(onClick = onStartApply) {
                    Text(
                        text = when {
                            isReadOnly -> stringResource(R.string.service_hub_open_service)
                            else -> stringResource(R.string.service_hub_start_request)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceApplyTab(
    service: ServiceItem,
    appLocale: Locale,
    hasForm: Boolean,
    isReadOnly: Boolean,
    isRequestService: Boolean,
    studentId: String = "",
    onNavigateToSubmit: (String?) -> Unit,
) {
    val context = LocalContext.current

    if (service.id == "attendance_rate") {
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var detail by remember { mutableStateOf<StudentAttendanceDetailDto?>(null) }

        LaunchedEffect(studentId) {
            loading = true
            error = null
            try {
                detail = AcademicRepository.loadAttendance(context, studentId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load"
            } finally {
                loading = false
            }
        }

        when {
            loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (error!!.contains("Academic data not found")) {
                            "No local academic record is available for this student yet."
                        } else {
                            error!!
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            detail != null -> {
                val d = detail!!
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.attendance_detail_overall),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${d.overallAttendancePercent}%",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = attendanceColor(d.overallAttendancePercent),
                                )
                                Text(
                                    text = stringResource(R.string.attendance_detail_class, d.classSection),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.attendance_detail_weekly_chart),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Source: cached API response · time range: current week trend",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        WeeklyAttendanceChart(weeklyTrend = d.weeklyTrend)
                    }

                    item {
                        Text(
                            text = stringResource(R.string.attendance_detail_by_course),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(d.courses, key = { it.courseCode }) { course ->
                        CourseAttendanceCard(
                            course = course,
                            onReportMistake = { onNavigateToSubmit(course.courseName) }
                        )
                    }

                    item {
                        Button(
                            onClick = { onNavigateToSubmit(null) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = localizedText(appLocale, "Submit Correction / Appeal", "提交考勤修正/申诉"))
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = stringResource(service.category.labelRes),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        text = service.resolveDescription(context, appLocale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.service_hub_turnaround,
                            service.resolveTurnaround(context, appLocale),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onNavigateToSubmit(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = when {
                        isReadOnly -> stringResource(R.string.service_hub_open_service)
                        isRequestService || hasForm -> stringResource(R.string.service_hub_start_request)
                        else -> stringResource(R.string.service_hub_view_details)
                    },
                )
            }
        }
    }
}

private fun Long.toRequestDateTime(locale: Locale): String {
    val formatter = if (locale.language.startsWith("zh")) {
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", locale)
    } else {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale)
    }
    return try {
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    } catch (e: Exception) {
        ""
    }
}


@Composable
private fun RequestDetailDialog(
    request: StudentRequest,
    serviceTitle: String,
    dateText: String,
    appLocale: Locale,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = serviceTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = request.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedText(appLocale, "Status", "状态"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    RequestStatusBadge(status = request.status)
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                
                Column {
                    Text(
                        text = localizedText(appLocale, "Submitted Date", "提交日期"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (request.contactInfo.isNotBlank()) {
                    Column {
                        Text(
                            text = localizedText(appLocale, "Contact Information", "联系信息"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = request.contactInfo,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (request.notes.isNotBlank() && request.notes != "—") {
                    Column {
                        Text(
                            text = localizedText(appLocale, "Details", "详细内容"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = request.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                
                val attachmentUrl = request.attachmentUrl
                if (!attachmentUrl.isNullOrBlank()) {
                    Column {
                        Text(
                            text = localizedText(appLocale, "Attachment", "附件"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "📎 ${attachmentUrl.substringAfterLast("/")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_confirm))
            }
        }
    )
}

private fun localizedText(locale: Locale, en: String, zh: String): String {
    return if (locale.language.startsWith("zh")) zh else en
}
