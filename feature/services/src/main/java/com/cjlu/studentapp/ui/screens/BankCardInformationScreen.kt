package com.cjlu.studentapp.ui.screens

import android.net.Uri
import androidx.compose.runtime.Composable
import com.cjlu.studentapp.data.RequestSubmission
import com.cjlu.studentapp.data.StudentRequest

@Composable
fun BankCardInformationScreen(
    serviceItems: List<ServiceItem>,
    studentDefaults: StudentDefaults,
    onSubmitServiceRequest: suspend (RequestSubmission, Uri?) -> Result<StudentRequest>,
    onAfterRequestSubmitted: () -> Unit,
    onBack: () -> Unit,
) {
    ServiceDetailScreen(
        serviceId = "bank_card_information",
        serviceItems = serviceItems,
        studentDefaults = studentDefaults,
        onSubmitServiceRequest = onSubmitServiceRequest,
        onAfterRequestSubmitted = onAfterRequestSubmitted,
        onBack = onBack,
    )
}
