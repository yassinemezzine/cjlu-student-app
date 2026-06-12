package com.cjlu.studentapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cjlu.studentapp.R
import com.cjlu.studentapp.data.RequestSubmission
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.ui.components.CheckboxAgreementBlock
import com.cjlu.studentapp.ui.components.DatePickerRow
import com.cjlu.studentapp.ui.components.DropdownSelectRow
import com.cjlu.studentapp.ui.components.FormSubmitButton
import com.cjlu.studentapp.ui.components.MultilineInputRow
import com.cjlu.studentapp.ui.components.ReadOnlyRow
import com.cjlu.studentapp.ui.components.RadioGroupRow
import com.cjlu.studentapp.ui.components.RequestStatusBadge
import com.cjlu.studentapp.ui.components.TextInputRow
import com.cjlu.studentapp.ui.components.TipHelpText
import com.cjlu.studentapp.ui.components.UploadAttachmentBox
import com.cjlu.studentapp.ui.components.formatDateForDisplay
import com.cjlu.studentapp.ui.forms.FormFieldUiModel
import com.cjlu.studentapp.ui.forms.PrefillSource
import com.cjlu.studentapp.ui.forms.ServiceFormUiModel
import com.cjlu.studentapp.ui.forms.TextInputValidation
import com.cjlu.studentapp.util.resolveAttachmentDisplayName
import com.cjlu.studentapp.util.tryTakePersistableReadPermission
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AnyFileMimeTypes = arrayOf("*/*")

data class StudentDefaults(
    val studentId: String,
    val studentName: String,
    val studyYear: String,
    val major: String,
    val school: String,
)

@Composable
fun DetailedServiceRequestScreen(
    service: ServiceItem,
    form: ServiceFormUiModel,
    appLocale: Locale,
    studentDefaults: StudentDefaults,
    onSubmitRequest: suspend (RequestSubmission, Uri?) -> Result<StudentRequest>,
    onBack: () -> Unit,
    prefillCourse: String? = null,
) {
    val context = LocalContext.current
    val requestDatePattern = stringResource(R.string.request_date_pattern)
    val requestDateFormatter = remember(appLocale, requestDatePattern) {
        DateTimeFormatter.ofPattern(requestDatePattern, appLocale)
    }
    val serviceTitle = service.resolveTitle(context, appLocale)
    val serviceDescription = service.resolveDescription(context, appLocale)
    val formFields = remember(form) { form.fields }
    val textValues = remember(service.id, studentDefaults, prefillCourse) {
        mutableStateMapOf<String, String>().apply {
            formFields.forEach { field ->
                when (field) {
                    is FormFieldUiModel.TextInput -> {
                        this[field.key] = if (field.key == "course_name" && !prefillCourse.isNullOrBlank()) {
                            prefillCourse
                        } else {
                            prefillText(field.prefillSource, studentDefaults)
                        }
                    }

                    is FormFieldUiModel.MultilineInput -> {
                        this[field.key] = ""
                    }

                    else -> Unit
                }
            }
        }
    }
    val dateValues = remember(service.id) {
        mutableStateMapOf<String, Long?>().apply {
            formFields.filterIsInstance<FormFieldUiModel.DatePicker>().forEach { field ->
                this[field.key] = null
            }
        }
    }
    val selectionValues = remember(service.id) {
        mutableStateMapOf<String, String>().apply {
            formFields.filterIsInstance<FormFieldUiModel.Dropdown>().forEach { field ->
                this[field.key] = field.defaultOptionId.orEmpty()
            }
            formFields.filterIsInstance<FormFieldUiModel.RadioGroup>().forEach { field ->
                this[field.key] = field.defaultOptionId.orEmpty()
            }
        }
    }
    val uploadValues = remember(service.id) {
        mutableStateMapOf<String, String>().apply {
            form.uploads.forEach { upload ->
                this[upload.key] = ""
            }
        }
    }
    var agreementValues = remember(service.id) {
        mutableStateMapOf<String, Boolean>().apply {
            form.agreements.forEach { agreement ->
                this[agreement.key] = false
            }
        }
    }
    var createdRequest by remember(service.id) { mutableStateOf<StudentRequest?>(null) }
    var showSuccessDialog by remember(service.id) { mutableStateOf(false) }
    var infoMessage by remember(service.id) { mutableStateOf<String?>(null) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var selectedAttachmentUri by remember(service.id) { mutableStateOf<Uri?>(null) }
    var selectedAttachmentName by remember(service.id) { mutableStateOf("") }
    var pendingUploadKey by remember(service.id) { mutableStateOf<String?>(null) }
    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                context.tryTakePersistableReadPermission(uri)
                selectedAttachmentUri = uri
                selectedAttachmentName = context.resolveAttachmentDisplayName(uri)
                pendingUploadKey?.let { key -> uploadValues[key] = selectedAttachmentName }
                pendingUploadKey = null
                createdRequest = null
            } else {
                pendingUploadKey = null
            }
        }
    )

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = localizedText(appLocale, "Submitted Successfully", "提交成功"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = localizedText(
                        appLocale,
                        "Your request for \"$serviceTitle\" has been submitted successfully.",
                        "您关于“$serviceTitle”的申请已成功提交。"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text(text = localizedText(appLocale, "OK", "确定"))
                }
            }
        )
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text(text = stringResource(R.string.dialog_submit_request_title)) },
            text = { Text(text = stringResource(R.string.dialog_submit_request_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmitDialog = false
                        isSubmitting = true
                        infoMessage = null
                        val submissionStudentId = textValues["student_id"]
                            ?.takeIf { it.isNotBlank() }
                            ?: studentDefaults.studentId

                        scope.launch {
                            val result = onSubmitRequest(
                                RequestSubmission(
                                    serviceId = service.id,
                                    studentId = submissionStudentId,
                                    contactInfo = buildContactInfo(
                                        textValues,
                                        selectionValues,
                                        studentDefaults
                                    ),
                                    notes = buildDetailedNotes(
                                        form = form,
                                        locale = appLocale,
                                        textValues = textValues,
                                        dateValues = dateValues,
                                        selectionValues = selectionValues,
                                        uploadValues = uploadValues,
                                        agreementValues = agreementValues,
                                    ),
                                ),
                                selectedAttachmentUri
                            )
                            createdRequest = result.getOrNull()
                            if (result.isSuccess) {
                                showSuccessDialog = true
                                infoMessage = null
                            } else {
                                infoMessage = localizedText(
                                    appLocale,
                                    "Could not submit this request. Check your connection and try again.",
                                    "提交失败，请检查网络后重试。",
                                )
                            }
                            isSubmitting = false
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        )
    }

    val studentIdErrors = remember(formFields, textValues.toMap()) {
        formFields.asSequence()
            .filterIsInstance<FormFieldUiModel.TextInput>()
            .filter {
                (it.validation == TextInputValidation.StudentId) &&
                    textValues[it.key].orEmpty().isNotBlank() &&
                    !isStudentIdValid(textValues[it.key].orEmpty())
            }
            .map { it.key }
            .toSet()
    }
    val hasRequiredFields = formFields.all { field ->
        isFieldCompleted(
            field = field,
            textValues = textValues,
            dateValues = dateValues,
            selectionValues = selectionValues
        )
    } && form.uploads.all { upload ->
        !upload.required || uploadValues[upload.key].orEmpty().isNotBlank()
    } && form.agreements.all { agreement ->
        !agreement.required || agreementValues[agreement.key] == true
    }
    val canSubmit = hasRequiredFields && studentIdErrors.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ServicePageHeader(
            title = serviceTitle,
            onBack = onBack
        )

        ServiceHeroCard(
            title = serviceTitle,
            description = serviceDescription,
            service = service,
            intro = form.intro.resolve(appLocale)
        )

        ServiceMetaRow(
            service = service,
            appLocale = appLocale,
            channelLabel = stringResource(R.string.service_detail_channel_office),
        )

        SectionHeader(
            title = stringResource(R.string.service_detail_prepare_title),
            subtitle = stringResource(R.string.service_detail_prepare_subtitle)
        )

        service.resolveChecklist(context, appLocale).forEach { line ->
            ChecklistCard(text = line)
        }

        SectionHeader(
            title = stringResource(R.string.service_detail_form_title),
            subtitle = stringResource(R.string.service_detail_form_subtitle)
        )

        form.tips.forEach { tip ->
            TipHelpText(text = tip.resolve(appLocale))
        }

        infoMessage?.let { message ->
            TipHelpText(text = message)
        }

        formFields.forEach { field ->
            when (field) {
                is FormFieldUiModel.ReadOnly -> {
                    ReadOnlyRow(
                        label = requiredLabel(field.label.resolve(appLocale), field.required),
                        value = field.value.resolve(appLocale),
                        trailingActionText = field.trailingAction?.resolve(appLocale),
                        onClick = {
                            createdRequest = null
                            infoMessage = localizedText(
                                appLocale,
                                "Office form template is ready to download.",
                                "办事表格模板已可下载。",
                            )
                        }
                    )
                }

                is FormFieldUiModel.TextInput -> {
                    val isStudentIdField = field.validation == TextInputValidation.StudentId
                    TextInputRow(
                        label = requiredLabel(field.label.resolve(appLocale), field.required),
                        value = textValues[field.key].orEmpty(),
                        onValueChange = {
                            textValues[field.key] = it
                            createdRequest = null
                        },
                        placeholder = field.placeholder?.resolve(appLocale),
                        helperText = if (isStudentIdField && studentIdErrors.contains(field.key)) {
                            stringResource(R.string.student_id_error)
                        } else {
                            field.helper?.resolve(appLocale)
                        },
                        isError = studentIdErrors.contains(field.key),
                        digitsOnly = field.digitsOnly,
                        maxLength = field.maxLength,
                        keyboardType = field.keyboardType
                    )
                }

                is FormFieldUiModel.MultilineInput -> {
                    MultilineInputRow(
                        label = requiredLabel(field.label.resolve(appLocale), field.required),
                        value = textValues[field.key].orEmpty(),
                        onValueChange = {
                            textValues[field.key] = it
                            createdRequest = null
                        },
                        placeholder = field.placeholder?.resolve(appLocale),
                        helperText = field.helper?.resolve(appLocale),
                        maxLength = field.maxLength
                    )
                }

                is FormFieldUiModel.DatePicker -> {
                    DatePickerRow(
                        label = requiredLabel(field.label.resolve(appLocale), field.required),
                        selectedDateText = formatDateForDisplay(dateValues[field.key], appLocale),
                        onDateSelected = {
                            dateValues[field.key] = it
                            createdRequest = null
                        },
                        placeholder = localizedText(appLocale, "Select a date", "请选择日期"),
                        helperText = field.helper?.resolve(appLocale),
                        initialDateMillis = dateValues[field.key],
                        confirmText = localizedText(appLocale, "OK", "确定"),
                        dismissText = localizedText(appLocale, "Cancel", "取消")
                    )
                }

                is FormFieldUiModel.Dropdown -> {
                    DropdownSelectRow(
                        label = requiredLabel(field.label.resolve(appLocale), field.required),
                        selectedText = resolveOptionLabel(field.options, selectionValues[field.key].orEmpty(), appLocale),
                        options = field.options.map { option ->
                            option.id to option.label.resolve(appLocale)
                        },
                        onOptionSelected = {
                            selectionValues[field.key] = it
                            createdRequest = null
                        },
                        helperText = field.helper?.resolve(appLocale),
                        placeholder = localizedText(appLocale, "Select an option", "请选择选项")
                    )
                }

                is FormFieldUiModel.RadioGroup -> {
                    RadioGroupRow(
                        label = requiredLabel(field.label.resolve(appLocale), field.required),
                        selectedOptionId = selectionValues[field.key].orEmpty(),
                        options = field.options.map { option ->
                            option.id to option.label.resolve(appLocale)
                        },
                        onOptionSelected = {
                            selectionValues[field.key] = it
                            createdRequest = null
                        },
                        helperText = field.helper?.resolve(appLocale)
                    )
                }
            }
        }

        form.uploads.forEach { upload ->
            UploadAttachmentBox(
                label = requiredLabel(upload.label.resolve(appLocale), upload.required),
                fileName = if (selectedAttachmentUri != null) selectedAttachmentName else uploadValues[upload.key].orEmpty(),
                onUploadClick = {
                    pendingUploadKey = upload.key
                    attachmentPickerLauncher.launch(AnyFileMimeTypes)
                },
                helperText = upload.helper?.resolve(appLocale),
                dashedBorder = upload.dashedBorder,
                emptyFileText = localizedText(
                    appLocale,
                    "Tap to attach · ${upload.suggestedFileName.resolve(appLocale)}",
                    "点击添加 · ${upload.suggestedFileName.resolve(appLocale)}",
                )
            )
        }

        form.agreements.forEach { agreement ->
            CheckboxAgreementBlock(
                text = agreement.text.resolve(appLocale),
                checked = agreementValues[agreement.key] == true,
                onCheckedChange = {
                    agreementValues[agreement.key] = it
                    createdRequest = null
                }
            )
        }

        FormSubmitButton(
            text = if (isSubmitting) stringResource(R.string.common_loading) else localizedText(appLocale, "Submit request", "提交申请"),
            enabled = canSubmit && !isSubmitting,
            onClick = { showSubmitDialog = true },
        )

        createdRequest?.let { request ->
            SuccessCard(
                request = request,
                serviceTitle = serviceTitle,
                formatter = requestDateFormatter
            )
        }
    }
}

@Composable
fun GenericServiceRequestScreen(
    service: ServiceItem,
    appLocale: Locale,
    studentDefaults: StudentDefaults,
    onSubmitRequest: suspend (RequestSubmission, Uri?) -> Result<StudentRequest>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val requestDatePattern = stringResource(R.string.request_date_pattern)
    val requestDateFormatter = remember(appLocale, requestDatePattern) {
        DateTimeFormatter.ofPattern(requestDatePattern, appLocale)
    }
    val serviceTitle = service.resolveTitle(context, appLocale)
    val serviceDescription = service.resolveDescription(context, appLocale)
    var studentId by remember(service.id, studentDefaults.studentId) {
        mutableStateOf(studentDefaults.studentId)
    }
    var contactInfo by remember(service.id) { mutableStateOf("") }
    var notes by remember(service.id) { mutableStateOf("") }
    var createdRequest by remember(service.id) { mutableStateOf<StudentRequest?>(null) }
    var showSuccessDialog by remember(service.id) { mutableStateOf(false) }
    var submitErrorMessage by remember(service.id) { mutableStateOf<String?>(null) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var selectedAttachmentUri by remember(service.id) { mutableStateOf<Uri?>(null) }
    var selectedAttachmentName by remember(service.id) { mutableStateOf("") }
    val genericAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                context.tryTakePersistableReadPermission(uri)
                selectedAttachmentUri = uri
                selectedAttachmentName = context.resolveAttachmentDisplayName(uri)
                createdRequest = null
            }
        }
    )

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = localizedText(appLocale, "Submitted Successfully", "提交成功"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = localizedText(
                        appLocale,
                        "Your request for \"$serviceTitle\" has been submitted successfully.",
                        "您关于“$serviceTitle”的申请已成功提交。"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text(text = localizedText(appLocale, "OK", "确定"))
                }
            }
        )
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text(text = stringResource(R.string.dialog_submit_request_title)) },
            text = { Text(text = stringResource(R.string.dialog_submit_request_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmitDialog = false
                        isSubmitting = true
                        submitErrorMessage = null
                        scope.launch {
                            val result = onSubmitRequest(
                                RequestSubmission(
                                    serviceId = service.id,
                                    studentId = studentId,
                                    contactInfo = contactInfo,
                                    notes = notes,
                                ),
                                selectedAttachmentUri
                            )
                            createdRequest = result.getOrNull()
                            if (result.isSuccess) {
                                showSuccessDialog = true
                                submitErrorMessage = null
                            } else {
                                submitErrorMessage = localizedText(
                                    appLocale,
                                    "Could not submit this request. Check your connection and try again.",
                                    "提交失败，请检查网络后重试。",
                                )
                            }
                            isSubmitting = false
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        )
    }

    val isStudentIdValid = isStudentIdValid(studentId)
    val canSubmit = isStudentIdValid && contactInfo.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ServicePageHeader(
            title = serviceTitle,
            onBack = onBack
        )

        ServiceHeroCard(
            title = serviceTitle,
            description = serviceDescription,
            service = service,
            intro = localizedText(
                appLocale,
                "This service uses a streamlined request flow.",
                "该服务使用精简的申请流程。",
            )
        )

        ServiceMetaRow(
            service = service,
            appLocale = appLocale,
            channelLabel = stringResource(R.string.service_detail_channel_office),
        )

        submitErrorMessage?.let { msg ->
            TipHelpText(text = msg)
        }

        SectionHeader(
            title = stringResource(R.string.service_detail_prepare_title),
            subtitle = stringResource(R.string.service_detail_prepare_subtitle)
        )

        service.resolveChecklist(context, appLocale).forEach { line ->
            ChecklistCard(text = line)
        }

        SectionHeader(
            title = stringResource(R.string.service_detail_form_title),
            subtitle = localizedText(
                appLocale,
                "Enter the details required for this request.",
                "请填写此申请所需的信息。",
            )
        )

        TextInputRow(
            label = requiredLabel(stringResource(R.string.service_detail_field_student_id), required = true),
            value = studentId,
            onValueChange = {
                studentId = it
                createdRequest = null
            },
            placeholder = stringResource(R.string.student_id_example),
            helperText = if (!isStudentIdValid && studentId.isNotBlank()) {
                stringResource(R.string.student_id_error)
            } else {
                stringResource(R.string.student_id_format_helper)
            },
            isError = !isStudentIdValid && studentId.isNotBlank(),
            digitsOnly = true,
            maxLength = 8,
            keyboardType = KeyboardType.Number
        )

        TextInputRow(
            label = requiredLabel(stringResource(R.string.service_detail_field_contact), true),
            value = contactInfo,
            onValueChange = {
                contactInfo = it
                createdRequest = null
            },
            placeholder = localizedText(
                appLocale,
                "Phone, room, or email",
                "手机号、房间号或邮箱"
            )
        )

        MultilineInputRow(
            label = stringResource(R.string.service_detail_field_notes),
            value = notes,
            onValueChange = {
                notes = it
                createdRequest = null
            },
            placeholder = localizedText(
                appLocale,
                "Add any extra details for this request",
                "补充此申请的其他说明",
            ),
            maxLength = 300
        )

        UploadAttachmentBox(
            label = localizedText(appLocale, "Attachment (optional)", "附件（可选）"),
            fileName = selectedAttachmentName,
            onUploadClick = { genericAttachmentLauncher.launch(AnyFileMimeTypes) },
            helperText = localizedText(
                appLocale,
                "PDF, images, archives, or other files supported.",
                "支持 PDF、图片、压缩包等任意文件类型。"
            ),
            dashedBorder = true,
            emptyFileText = localizedText(appLocale, "Tap to attach a file", "点击选择文件")
        )

        FormSubmitButton(
            text = if (isSubmitting) stringResource(R.string.common_loading) else localizedText(appLocale, "Submit request", "提交申请"),
            enabled = canSubmit && !isSubmitting,
            onClick = { showSubmitDialog = true },
        )

        createdRequest?.let { request ->
            SuccessCard(
                request = request,
                serviceTitle = serviceTitle,
                formatter = requestDateFormatter
            )
        }
    }
}

@Composable
fun MissingServiceScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.service_detail_not_found_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.service_detail_not_found_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onBack) {
                    Text(text = stringResource(R.string.nav_services))
                }
            }
        }
    }
}

@Composable
private fun ServicePageHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ServiceHeroCard(
    title: String,
    description: String,
    service: ServiceItem,
    intro: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = service.category.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!intro.isNullOrBlank()) {
                TipHelpText(text = intro)
            }
        }
    }
}

@Composable
fun ServiceInfoScreen(
    service: ServiceItem,
    appLocale: Locale,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val serviceTitle = service.resolveTitle(context, appLocale)
    val serviceDescription = service.resolveDescription(context, appLocale)
    val form = remember(service.id) { ServiceFormCatalog.find(service.id) }
    val intro = form?.intro?.resolve(appLocale)
        ?: stringResource(R.string.service_detail_office_notice)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ServicePageHeader(
            title = serviceTitle,
            onBack = onBack,
        )

        ServiceHeroCard(
            title = serviceTitle,
            description = serviceDescription,
            service = service,
            intro = intro,
        )

        ServiceMetaRow(
            service = service,
            appLocale = appLocale,
            channelLabel = stringResource(R.string.service_detail_channel_office),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.service_detail_how_to_apply_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.service_detail_office_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionHeader(
            title = stringResource(R.string.service_detail_prepare_title),
            subtitle = stringResource(R.string.service_detail_prepare_subtitle),
        )

        service.resolveChecklist(context, appLocale).forEach { line ->
            ChecklistCard(text = line)
        }

        if (form != null && form.tips.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.service_detail_guidance_title),
                subtitle = stringResource(R.string.service_detail_guidance_subtitle),
            )
            form.tips.forEach { tip ->
                TipHelpText(text = tip.resolve(appLocale))
            }
        }
    }
}

@Composable
private fun ServiceMetaRow(
    service: ServiceItem,
    appLocale: Locale,
    channelLabel: String,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetaCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.service_detail_meta_time),
            value = service.resolveTurnaround(context, appLocale)
        )
        MetaCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.service_detail_meta_channel),
            value = channelLabel,
        )
    }
}

@Composable
private fun MetaCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
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
private fun ChecklistCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(10.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {}
            Text(
                text = text,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SuccessCard(
    request: StudentRequest,
    serviceTitle: String,
    formatter: DateTimeFormatter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.service_detail_success_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.service_detail_success_body, serviceTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RequestDetailRow(
                title = stringResource(R.string.request_label_tracking_id),
                value = request.id
            )
            RequestDetailRow(
                title = stringResource(R.string.request_label_created),
                value = request.createdAtMillis.toRequestDate(formatter)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.request_label_status),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                RequestStatusBadge(status = request.status)
            }
        }
    }
}

@Composable
private fun RequestDetailRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun localizedText(locale: Locale, english: String, chinese: String): String {
    return if (locale.language.startsWith("zh")) chinese else english
}

private fun isFieldCompleted(
    field: FormFieldUiModel,
    textValues: Map<String, String>,
    dateValues: Map<String, Long?>,
    selectionValues: Map<String, String>
): Boolean {
    return when (field) {
        is FormFieldUiModel.ReadOnly -> true
        is FormFieldUiModel.TextInput -> {
            val value = textValues[field.key].orEmpty()
            if (!field.required && value.isBlank()) {
                true
            } else if (field.validation == TextInputValidation.StudentId) {
                isStudentIdValid(value)
            } else {
                value.isNotBlank()
            }
        }

        is FormFieldUiModel.MultilineInput -> {
            val value = textValues[field.key].orEmpty()
            !field.required || value.isNotBlank()
        }

        is FormFieldUiModel.DatePicker -> {
            !field.required || dateValues[field.key] != null
        }

        is FormFieldUiModel.Dropdown -> {
            !field.required || selectionValues[field.key].orEmpty().isNotBlank()
        }

        is FormFieldUiModel.RadioGroup -> {
            !field.required || selectionValues[field.key].orEmpty().isNotBlank()
        }
    }
}

private fun resolveOptionLabel(
    options: List<com.cjlu.studentapp.ui.forms.OptionItem>,
    selectedId: String,
    locale: Locale
): String {
    return options.firstOrNull { it.id == selectedId }?.label?.resolve(locale).orEmpty()
}

private fun buildContactInfo(
    textValues: Map<String, String>,
    selectionValues: Map<String, String>,
    studentDefaults: StudentDefaults
): String {
    val priorityKeys = listOf(
        "phone_number",
        "mobile",
        "email",
        "chinese_phone_number",
        "detail_address",
        "address",
        "location"
    )
    val match = priorityKeys.firstNotNullOfOrNull { key ->
        textValues[key]?.trim()?.takeIf { it.isNotBlank() }
    }

    return match
        ?: selectionValues["collecting_on_behalf"]
        ?: studentDefaults.studentId
}

private fun buildDetailedNotes(
    form: ServiceFormUiModel,
    locale: Locale,
    textValues: Map<String, String>,
    dateValues: Map<String, Long?>,
    selectionValues: Map<String, String>,
    uploadValues: Map<String, String>,
    agreementValues: Map<String, Boolean>
): String {
    val lines = mutableListOf<String>()

    form.fields.forEach { field ->
        when (field) {
            is FormFieldUiModel.ReadOnly -> {
                lines += "${field.label.resolve(locale)}: ${field.value.resolve(locale)}"
            }

            is FormFieldUiModel.TextInput -> {
                textValues[field.key]
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { lines += "${field.label.resolve(locale)}: $it" }
            }

            is FormFieldUiModel.MultilineInput -> {
                textValues[field.key]
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { lines += "${field.label.resolve(locale)}: $it" }
            }

            is FormFieldUiModel.DatePicker -> {
                dateValues[field.key]
                    ?.let {
                        val dateStr = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()
                        lines += "${field.label.resolve(locale)}: $dateStr"
                    }
            }

            is FormFieldUiModel.Dropdown -> {
                selectionValues[field.key]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { selected ->
                        lines += "${field.label.resolve(locale)}: ${resolveOptionLabel(field.options, selected, locale)}"
                    }
            }

            is FormFieldUiModel.RadioGroup -> {
                selectionValues[field.key]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { selected ->
                        lines += "${field.label.resolve(locale)}: ${resolveOptionLabel(field.options, selected, locale)}"
                    }
            }
        }
    }

    form.uploads.forEach { upload ->
        uploadValues[upload.key]
            ?.takeIf { it.isNotBlank() }
            ?.let { lines += "${upload.label.resolve(locale)}: $it" }
    }

    form.agreements.forEach { agreement ->
        if (agreementValues[agreement.key] == true) {
            lines += agreement.text.resolve(locale)
        }
    }

    return lines.joinToString(separator = "\n")
}

private fun isStudentIdValid(studentId: String): Boolean {
    return studentId.length == 8 && studentId.all(Char::isDigit)
}

private fun requiredLabel(label: String, required: Boolean): String {
    return if (required) "$label *" else label
}

private fun prefillText(
    source: PrefillSource,
    studentDefaults: StudentDefaults
): String {
    return when (source) {
        PrefillSource.None -> ""
        PrefillSource.StudentId -> studentDefaults.studentId
        PrefillSource.StudentName -> studentDefaults.studentName
        PrefillSource.StudyYear -> studentDefaults.studyYear
        PrefillSource.Major -> studentDefaults.major
        PrefillSource.School -> studentDefaults.school
    }
}

private fun Long.toRequestDate(formatter: DateTimeFormatter): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}
