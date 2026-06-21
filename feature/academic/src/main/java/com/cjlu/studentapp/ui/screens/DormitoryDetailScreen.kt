package com.cjlu.studentapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cjlu.core.resources.R
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.network.api.StudentDormitoryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DormitoryDetailScreen(
    studentId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dormitory by remember { mutableStateOf<StudentDormitoryDto?>(null) }

    LaunchedEffect(studentId) {
        loading = true
        error = null
        try {
            dormitory = AcademicRepository.loadDormitory(context, studentId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dormitory_detail_title)) },
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
                        "No local dormitory record is available for this student yet."
                    } else {
                        error!!
                    },
                    modifier = Modifier.padding(padding).padding(20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            dormitory != null -> {
                DormitoryContent(
                    dormitory = dormitory!!,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun DormitoryContent(
    dormitory: StudentDormitoryDto,
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
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = dormitory.buildingName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            R.string.dormitory_detail_room,
                            dormitory.roomNumber,
                            dormitory.floor,
                            dormitory.bedLabel,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dormitory_detail_leave_status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (dormitory.hasActiveLeave) {
                        Text(
                            text = dormitory.leaveReason.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!dormitory.leaveFromDate.isNullOrBlank() && !dormitory.leaveToDate.isNullOrBlank()) {
                            Text(
                                text = stringResource(
                                    R.string.dormitory_detail_leave_dates,
                                    dormitory.leaveFromDate!!,
                                    dormitory.leaveToDate!!,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.dormitory_detail_no_leave),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
