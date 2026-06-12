package com.cjlu.studentapp.ui.screens

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import com.cjlu.studentapp.data.RequestSubmission
import com.cjlu.studentapp.data.StudentRequest
import java.util.Locale

@Composable
fun ServiceDetailScreen(
    serviceId: String,
    prefillCourse: String? = null,
    serviceItems: List<ServiceItem>,
    studentDefaults: StudentDefaults,
    academicRefreshNonce: Int = 0,
    onSubmitServiceRequest: suspend (RequestSubmission, Uri?) -> Result<StudentRequest>,
    onAfterRequestSubmitted: () -> Unit,
    onBack: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val appLocale = remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }
    val service = remember(serviceId, serviceItems) {
        serviceItems.firstOrNull { it.id == serviceId }
    }

    if (service == null) {
        MissingServiceScreen(onBack = onBack)
        return
    }

    when (service.id) {
        "transcripts" -> {
            TranscriptDetailScreen(
                studentId = studentDefaults.studentId,
                onBack = onBack,
            )
            return
        }
        "class_schedule" -> {
            SchoolCalendarScreen(
                studentId = studentDefaults.studentId,
                refreshNonce = academicRefreshNonce,
                onBack = onBack,
            )
            return
        }
        "changing_room" -> {
            DormitoryDetailScreen(
                studentId = studentDefaults.studentId,
                onBack = onBack,
            )
            return
        }
        "ask_leave" -> {
            val form = ServiceFormCatalog.find(service.id)
            if (form != null) {
                DetailedServiceRequestScreen(
                    service = service,
                    form = form,
                    appLocale = appLocale,
                    studentDefaults = studentDefaults,
                    prefillCourse = prefillCourse,
                    onSubmitRequest = { submission, uri ->
                        val result = onSubmitServiceRequest(submission, uri)
                        if (result.isSuccess) {
                            onAfterRequestSubmitted()
                        }
                        result
                    },
                    onBack = onBack,
                )
            } else {
                ServiceInfoScreen(
                    service = service,
                    appLocale = appLocale,
                    onBack = onBack,
                )
            }
            return
        }
    }

    val form = remember(service.id) { ServiceFormCatalog.find(service.id) }
    if (form != null) {
        DetailedServiceRequestScreen(
            service = service,
            form = form,
            appLocale = appLocale,
            studentDefaults = studentDefaults,
            prefillCourse = prefillCourse,
            onSubmitRequest = { submission, uri ->
                val result = onSubmitServiceRequest(submission, uri)
                if (result.isSuccess) {
                    onAfterRequestSubmitted()
                }
                result
            },
            onBack = onBack,
        )
    } else {
        ServiceInfoScreen(
            service = service,
            appLocale = appLocale,
            onBack = onBack,
        )
    }
}
