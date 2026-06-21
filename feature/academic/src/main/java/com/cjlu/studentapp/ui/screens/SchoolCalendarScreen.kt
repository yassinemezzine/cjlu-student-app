package com.cjlu.studentapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cjlu.core.resources.R
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.network.api.StudentAcademicCalendarDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SchoolCalendarScreen(
    studentId: String,
    refreshNonce: Int = 0,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var calendar by remember { mutableStateOf<StudentAcademicCalendarDto?>(null) }
    var isFallback by remember { mutableStateOf(false) }

    // Selected date state. Format: "YYYY-MM-DD"
    var selectedDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(studentId, refreshNonce) {
        val loaded = runCatching { AcademicRepository.loadAcademicCalendar(context, studentId) }.getOrNull()
        if (loaded != null) {
            calendar = loaded
            isFallback = false
        } else {
            calendar = sampleCalendar()
            isFallback = true
        }
    }

    val data = calendar ?: sampleCalendar().also { isFallback = true }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { data.months.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.school_calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.service_hub_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = data.academicYearLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = data.semesterLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = stringResource(R.string.school_calendar_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (isFallback) {
                        Text(
                            text = stringResource(R.string.school_calendar_fallback_banner),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Navigation Header
            if (data.months.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            coroutineScope.launch { 
                                if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            } 
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month")
                    }
                    
                    Text(
                        text = data.months[pagerState.currentPage].monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { 
                            coroutineScope.launch { 
                                if (pagerState.currentPage < data.months.lastIndex) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } 
                        },
                        enabled = pagerState.currentPage < data.months.lastIndex
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            // Calendar Pager
            if (data.months.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    val month = data.months[page]
                    val monthPrefix = month.monthLabel // e.g., "2026-01"
                    val eventsForMonth = data.events.filter { it.date.startsWith(monthPrefix) }
                    
                    CalendarMonthInteractiveCard(
                        monthLabel = monthPrefix,
                        weekHeaders = month.weekHeaders,
                        rows = month.rows,
                        monthEvents = eventsForMonth,
                        selectedDate = selectedDate,
                        onDayClick = { dateStr ->
                            // Toggle selection
                            selectedDate = if (selectedDate == dateStr) null else dateStr
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Events List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedDate != null) "Events for $selectedDate" else stringResource(R.string.school_calendar_events_title), 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold
                )
                if (selectedDate != null) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedDate = null }
                            .padding(4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Events List
            val currentEvents = remember(selectedDate, data.events, pagerState.currentPage) {
                if (selectedDate != null) {
                    data.events.filter { it.date == selectedDate }
                } else {
                    if (data.months.isNotEmpty()) {
                        val activeMonthPrefix = data.months[pagerState.currentPage].monthLabel
                        data.events.filter { it.date.startsWith(activeMonthPrefix) }
                    } else {
                        data.events
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentEvents.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No events", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(currentEvents) { event ->
                        CalendarEventCard(event.date, event.title, event.detail, toneFromName(event.tone))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthInteractiveCard(
    monthLabel: String, 
    weekHeaders: List<String>, 
    rows: List<List<String>>,
    monthEvents: List<com.cjlu.studentapp.network.api.AcademicCalendarEventDto>,
    selectedDate: String?,
    onDayClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Week Headers
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { 
                weekHeaders.forEach { header -> 
                    Text(
                        text = header, 
                        modifier = Modifier.weight(1f), 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ) 
                } 
            }
            // Days
            rows.forEachIndexed { index, week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dayStr -> 
                        if (dayStr.isNotBlank()) {
                            // Format the date string, e.g. "2026-01-05"
                            val dayInt = dayStr.toIntOrNull() ?: 0
                            val fullDateStr = "$monthLabel-${dayInt.toString().padStart(2, '0')}"
                            val isSelected = fullDateStr == selectedDate
                            val dayEvents = monthEvents.filter { it.date == fullDateStr }
                            
                            InteractiveDayCell(
                                label = dayStr, 
                                isSelected = isSelected,
                                events = dayEvents,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onDayClick(fullDateStr) }
                            )
                        } else {
                            // Empty box for padding
                            Box(modifier = Modifier.weight(1f).height(48.dp))
                        }
                    }
                }
                if (index < rows.lastIndex) Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun InteractiveDayCell(
    label: String, 
    isSelected: Boolean, 
    events: List<com.cjlu.studentapp.network.api.AcademicCalendarEventDto>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .height(48.dp), 
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = if (isSelected || events.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            
            // Event indicators
            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    events.take(3).forEach { event ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(toneFromName(event.tone))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEventCard(date: String, title: String, detail: String, tone: Color) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = tone.copy(alpha = 0.15f)) { Box(modifier = Modifier.size(12.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun toneFromName(name: String): Color = when (name.lowercase()) {
    "blue" -> Color(0xFF1565C0)
    "purple" -> Color(0xFF6A1B9A)
    "green" -> Color(0xFF2E7D32)
    "amber" -> Color(0xFFF9A825)
    "red" -> Color(0xFFC62828)
    else -> Color(0xFF00838F)
}

private fun sampleCalendar(): StudentAcademicCalendarDto = StudentAcademicCalendarDto(
    academicYearLabel = "2025–2026",
    semesterLabel = "Full Academic Year",
    months = listOf(
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2025-09",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7"),
                listOf("8", "9", "10", "11", "12", "13", "14"),
                listOf("15", "16", "17", "18", "19", "20", "21"),
                listOf("22", "23", "24", "25", "26", "27", "28"),
                listOf("29", "30", "", "", "", "", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2025-10",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "1", "2", "3", "4", "5"),
                listOf("6", "7", "8", "9", "10", "11", "12"),
                listOf("13", "14", "15", "16", "17", "18", "19"),
                listOf("20", "21", "22", "23", "24", "25", "26"),
                listOf("27", "28", "29", "30", "31", "", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2025-11",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "1", "2"),
                listOf("3", "4", "5", "6", "7", "8", "9"),
                listOf("10", "11", "12", "13", "14", "15", "16"),
                listOf("17", "18", "19", "20", "21", "22", "23"),
                listOf("24", "25", "26", "27", "28", "29", "30")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2025-12",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7"),
                listOf("8", "9", "10", "11", "12", "13", "14"),
                listOf("15", "16", "17", "18", "19", "20", "21"),
                listOf("22", "23", "24", "25", "26", "27", "28"),
                listOf("29", "30", "31", "", "", "", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-01",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "1", "2", "3", "4"),
                listOf("5", "6", "7", "8", "9", "10", "11"),
                listOf("12", "13", "14", "15", "16", "17", "18"),
                listOf("19", "20", "21", "22", "23", "24", "25"),
                listOf("26", "27", "28", "29", "30", "31", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-02",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "", "1"),
                listOf("2", "3", "4", "5", "6", "7", "8"),
                listOf("9", "10", "11", "12", "13", "14", "15"),
                listOf("16", "17", "18", "19", "20", "21", "22"),
                listOf("23", "24", "25", "26", "27", "28", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-03",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "", "1"),
                listOf("2", "3", "4", "5", "6", "7", "8"),
                listOf("9", "10", "11", "12", "13", "14", "15"),
                listOf("16", "17", "18", "19", "20", "21", "22"),
                listOf("23", "24", "25", "26", "27", "28", "29"),
                listOf("30", "31", "", "", "", "", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-04",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "1", "2", "3", "4", "5"),
                listOf("6", "7", "8", "9", "10", "11", "12"),
                listOf("13", "14", "15", "16", "17", "18", "19"),
                listOf("20", "21", "22", "23", "24", "25", "26"),
                listOf("27", "28", "29", "30", "", "", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-05",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "1", "2", "3"),
                listOf("4", "5", "6", "7", "8", "9", "10"),
                listOf("11", "12", "13", "14", "15", "16", "17"),
                listOf("18", "19", "20", "21", "22", "23", "24"),
                listOf("25", "26", "27", "28", "29", "30", "31")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-06",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7"),
                listOf("8", "9", "10", "11", "12", "13", "14"),
                listOf("15", "16", "17", "18", "19", "20", "21"),
                listOf("22", "23", "24", "25", "26", "27", "28"),
                listOf("29", "30", "", "", "", "", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-07",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "1", "2", "3", "4", "5"),
                listOf("6", "7", "8", "9", "10", "11", "12"),
                listOf("13", "14", "15", "16", "17", "18", "19"),
                listOf("20", "21", "22", "23", "24", "25", "26"),
                listOf("27", "28", "29", "30", "31", "", "")
            )
        ),
        com.cjlu.studentapp.network.api.AcademicCalendarMonthDto(
            monthLabel = "2026-08",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "1", "2"),
                listOf("3", "4", "5", "6", "7", "8", "9"),
                listOf("10", "11", "12", "13", "14", "15", "16"),
                listOf("17", "18", "19", "20", "21", "22", "23"),
                listOf("24", "25", "26", "27", "28", "29", "30"),
                listOf("31", "", "", "", "", "", "")
            )
        ),
    ),
    events = listOf(
        com.cjlu.studentapp.network.api.AcademicCalendarEventDto("2026-01-05", "Winter vacation begins", "Campus services switch to holiday mode.", "blue"),
        com.cjlu.studentapp.network.api.AcademicCalendarEventDto("2026-02-15", "Register", "Students complete semester registration.", "purple"),
        com.cjlu.studentapp.network.api.AcademicCalendarEventDto("2026-02-23", "Class startup", "Spring classes begin.", "green"),
        com.cjlu.studentapp.network.api.AcademicCalendarEventDto("2026-04-05", "Holiday", "Ching Ming Festival break.", "amber"),
        com.cjlu.studentapp.network.api.AcademicCalendarEventDto("2026-06-20", "Final exams", "End-of-term examinations start.", "red"),
        com.cjlu.studentapp.network.api.AcademicCalendarEventDto("2026-07-10", "Summer vacation begins", "Term closes and campus services pause.", "blue"),
    )
)
