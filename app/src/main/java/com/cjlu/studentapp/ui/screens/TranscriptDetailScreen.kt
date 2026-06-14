package com.cjlu.studentapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import com.cjlu.studentapp.util.adaptiveContentWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cjlu.studentapp.R
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.network.api.StudentTranscriptDto
import com.cjlu.studentapp.network.api.TranscriptCourseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptDetailScreen(
    studentId: String,
    refreshNonce: Int = 0,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var transcript by remember { mutableStateOf<StudentTranscriptDto?>(null) }
    val context = LocalContext.current

    LaunchedEffect(studentId, refreshNonce) {
        loading = true
        error = null
        try {
            transcript = AcademicRepository.loadTranscript(context, studentId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = { Text(stringResource(R.string.transcript_detail_title)) },
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
                        "No local transcript record is available for this student yet."
                    } else {
                        error!!
                    },
                    modifier = Modifier.padding(padding).padding(20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            transcript != null -> {
                TranscriptContent(
                    transcript = transcript!!,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun TranscriptContent(
    transcript: StudentTranscriptDto,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .adaptiveContentWidth()
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.transcript_detail_gpa),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = String.format("%.2f", transcript.cumulativeGpa),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = transcript.semesterLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.transcript_detail_class, transcript.classSection),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.transcript_col_course),
                    modifier = Modifier.weight(1.2f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.transcript_col_score),
                    modifier = Modifier.weight(0.5f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.transcript_col_gpa),
                    modifier = Modifier.weight(0.5f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        items(transcript.courses, key = { it.courseCode }) { course ->
            TranscriptCourseRow(course)
        }
    }
}
}

@Composable
private fun TranscriptCourseRow(course: TranscriptCourseDto) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(text = course.courseName, fontWeight = FontWeight.Medium)
                Text(
                    text = "${course.courseCode} · ${course.credits} cr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${course.scorePercent}%",
                modifier = Modifier.weight(0.5f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = String.format("%.1f", course.gradePoint),
                modifier = Modifier.weight(0.5f),
            )
        }
    }
}
