package com.cjlu.studentapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cjlu.studentapp.R
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.network.api.CourseAttendanceDto
import com.cjlu.studentapp.network.api.StudentAttendanceDetailDto
import com.cjlu.studentapp.network.api.WeeklyAttendanceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(
    studentId: String,
    refreshNonce: Int = 0,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<StudentAttendanceDetailDto?>(null) }
    val context = LocalContext.current

    LaunchedEffect(studentId, refreshNonce) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.attendance_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text(
                    text = if (error!!.contains("Academic data not found")) {
                        "No local academic record is available for this student yet."
                    } else {
                        error!!
                    },
                    modifier = Modifier.padding(padding).padding(20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            detail != null -> {
                AttendanceContent(
                    detail = detail!!,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun AttendanceContent(
    detail: StudentAttendanceDetailDto,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.attendance_detail_overall),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${detail.overallAttendancePercent}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = attendanceColor(detail.overallAttendancePercent),
                    )
                    Text(
                        text = stringResource(R.string.attendance_detail_class, detail.classSection),
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
            WeeklyAttendanceChart(weeklyTrend = detail.weeklyTrend)
        }

        item {
            Text(
                text = stringResource(R.string.attendance_detail_by_course),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        items(detail.courses, key = { it.courseCode }) { course ->
            CourseAttendanceCard(course)
        }
    }
}

@Composable
fun WeeklyAttendanceChart(weeklyTrend: List<WeeklyAttendanceDto>) {
    if (weeklyTrend.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxPercent = 100f

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                val barCount = weeklyTrend.size
                val gap = size.width * 0.04f
                val barWidth = (size.width - gap * (barCount + 1)) / barCount
                val chartHeight = size.height - 24.dp.toPx()

                weeklyTrend.forEachIndexed { index, week ->
                    val fraction = week.percent / maxPercent
                    val barHeight = chartHeight * fraction
                    val left = gap + index * (barWidth + gap)
                    val top = chartHeight - barHeight + 8.dp.toPx()
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                weeklyTrend.forEach { week ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = week.weekLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                        )
                        Text(
                            text = "${week.percent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseAttendanceCard(
    course: CourseAttendanceDto,
    onReportMistake: (() -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = course.courseName, fontWeight = FontWeight.SemiBold)
                    Text(
                          text = course.courseCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${course.attendancePercent}%",
                    fontWeight = FontWeight.Bold,
                    color = attendanceColor(course.attendancePercent),
                )
            }
            LinearProgressIndicator(
                progress = { course.attendancePercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = attendanceColor(course.attendancePercent),
            )
            Text(
                text = stringResource(
                    R.string.attendance_detail_sessions,
                    course.sessionsAttended,
                    course.sessionsTotal,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            
            if (onReportMistake != null) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val isZh = LocalConfiguration.current.locales[0].language.startsWith("zh")
                    val btnText = if (isZh) "报告错误 / 申诉" else "Report Mistake / Appeal"
                    TextButton(
                        onClick = onReportMistake,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = btnText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

fun attendanceColor(percent: Int): Color = when {
    percent < 75 -> Color(0xFFC62828)
    percent < 85 -> Color(0xFFF57C00)
    else -> Color(0xFF2E7D32)
}
