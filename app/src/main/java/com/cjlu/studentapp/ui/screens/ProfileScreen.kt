package com.cjlu.studentapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.cjlu.studentapp.R
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.localization.AppLanguage
import com.cjlu.studentapp.prefs.AppNotificationPrefs
import com.cjlu.studentapp.ui.components.FormSubmitButton
import com.cjlu.studentapp.ui.components.ProfileMenuRow
import com.cjlu.studentapp.ui.components.RequestSummaryCard
import com.cjlu.studentapp.ui.components.SoftDivider
import com.cjlu.studentapp.ui.components.TextInputRow
import com.cjlu.studentapp.ui.components.TipHelpText
import com.cjlu.studentapp.ui.forms.ProfileMenuItemUiModel
import com.cjlu.studentapp.ui.forms.uiText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    studentName: String,
    studentId: String,
    studyYear: String,
    major: String,
    school: String,
    selectedLanguage: AppLanguage,
    requests: List<StudentRequest>,
    serviceItems: List<ServiceItem>,
    onRequestSelected: (String) -> Unit,
    onLogout: () -> Unit,
    onChangePassword: suspend (String, String) -> Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSaveProfile: suspend (String, String) -> Boolean = { _, _ -> true },
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLocale = remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }
    val requestDatePattern = stringResource(R.string.request_date_pattern)
    val requestDateFormatter = remember(appLocale, requestDatePattern) {
        DateTimeFormatter.ofPattern(requestDatePattern, appLocale)
    }
    val initials = remember(studentName) {
        studentName.split(" ")
            .asSequence()
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
    }
    var showLanguageSheet by remember { mutableStateOf(value = false) }
    var showEditProfileSheet by remember { mutableStateOf(value = false) }
    var showPasswordSheet by remember { mutableStateOf(value = false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf<String?>(null) }

    var notificationsOn by remember { mutableStateOf(false) }

    fun readNotifySwitch(): Boolean {
        if (!AppNotificationPrefs.isNotifyUpdatesEnabled(context)) return false
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        AppNotificationPrefs.setNotifyUpdatesEnabled(context, enabled)
        notificationsOn = enabled
    }

    LaunchedEffect(Unit) {
        notificationsOn = readNotifySwitch()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        setNotificationsEnabled(granted)
    }

    val personalItems = remember(requests.size) {
        buildPersonalItems(requests.size)
    }
    val accountItems = remember(selectedLanguage) {
        buildAccountItems(selectedLanguage)
    }

    if (showLanguageSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_language_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                AppLanguage.entries.forEachIndexed { index, language ->
                    LanguageOptionRow(
                        language = language,
                        selectedLanguage = selectedLanguage,
                    ) {
                        showLanguageSheet = false
                        onLanguageSelected(language)
                    }

                    if (index < AppLanguage.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }

    if (showPasswordSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }
        val canSave = currentPassword.isNotBlank() &&
            newPassword.isNotBlank() &&
            confirmPassword.isNotBlank()

        ModalBottomSheet(
            onDismissRequest = { showPasswordSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = localizedText(appLocale, "Change password", "修改密码"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = localizedText(
                        appLocale,
                        "Update the password stored on this device for this account.",
                        "更新当前设备上该账号的密码。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextInputRow(
                    label = localizedText(appLocale, "Current password", "当前密码"),
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        passwordError = null
                    },
                    visualTransformation = PasswordVisualTransformation()
                )
                TextInputRow(
                    label = localizedText(appLocale, "New password", "新密码"),
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        passwordError = null
                    },
                    visualTransformation = PasswordVisualTransformation()
                )
                TextInputRow(
                    label = localizedText(appLocale, "Confirm new password", "确认新密码"),
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        passwordError = null
                    },
                    visualTransformation = PasswordVisualTransformation()
                )

                passwordError?.let { error ->
                    TipHelpText(text = error)
                }

                FormSubmitButton(
                    text = localizedText(appLocale, "Save password", "保存密码"),
                    enabled = canSave,
                    onClick = {
                        passwordError = when {
                            newPassword != confirmPassword -> localizedText(
                                appLocale,
                                "The new passwords do not match.",
                                "两次输入的新密码不一致。"
                            )

                            newPassword.length < 4 -> localizedText(
                                appLocale,
                                "Use at least 4 characters for the new password.",
                                "新密码至少使用4个字符。"
                            )

                            else -> null
                        }

                        if (passwordError != null) return@FormSubmitButton

                        scope.launch {
                            val ok = onChangePassword(currentPassword, newPassword)
                            passwordError = if (!ok) {
                                localizedText(
                                    appLocale,
                                    "Current password is incorrect.",
                                    "当前密码不正确。"
                                )
                            } else {
                                null
                            }
                            if (passwordError == null) {
                                showPasswordSheet = false
                                profileMessage = localizedText(
                                    appLocale,
                                    "Password updated successfully.",
                                    "密码已更新。",
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    if (showEditProfileSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        var editedMajor by remember(major) { mutableStateOf(major) }
        var editedSchool by remember(school) { mutableStateOf(school) }
        var bankCardNum by remember {
            val prefs = context.getSharedPreferences("student_card_prefs", android.content.Context.MODE_PRIVATE)
            mutableStateOf(prefs.getString("bank_card_num_$studentId", "") ?: "")
        }
        var isSavingProfile by remember { mutableStateOf(false) }
        var saveProfileSuccess by remember { mutableStateOf(false) }
        var saveProfileError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { if (!isSavingProfile) showEditProfileSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = localizedText(appLocale, "Edit Academic & Financial Details", "编辑学籍与财务信息"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // QR code card frame
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudentQrCode(
                            studentId = studentId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        Text(
                            text = localizedText(appLocale, "Scan for Campus Services", "扫一扫校园服务码"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Registry info list (Read-only values)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IdentityInfoRow(label = localizedText(appLocale, "Full Name", "学生姓名"), value = studentName)
                        IdentityInfoRow(label = localizedText(appLocale, "Student ID", "学籍学号"), value = studentId)
                    }
                }

                TextInputRow(
                    label = localizedText(appLocale, "Registry Major", "在读专业"),
                    value = editedMajor,
                    onValueChange = {
                        editedMajor = it
                        saveProfileSuccess = false
                    }
                )

                TextInputRow(
                    label = localizedText(appLocale, "School / College", "所属学院"),
                    value = editedSchool,
                    onValueChange = {
                        editedSchool = it
                        saveProfileSuccess = false
                    }
                )

                TextInputRow(
                    label = localizedText(appLocale, "Bank Card Number", "银行卡号"),
                    value = bankCardNum,
                    onValueChange = {
                        bankCardNum = it
                        saveProfileSuccess = false
                    }
                )

                saveProfileError?.let { error ->
                    TipHelpText(text = error)
                }

                FormSubmitButton(
                    text = if (isSavingProfile) {
                        localizedText(appLocale, "Saving...", "保存中...")
                    } else if (saveProfileSuccess) {
                        localizedText(appLocale, "✓ Details Updated Successfully", "✓ 学籍信息已成功更新")
                    } else {
                        localizedText(appLocale, "Save Details", "保存信息")
                    },
                    enabled = editedMajor.isNotBlank() && editedSchool.isNotBlank() && !isSavingProfile && !saveProfileSuccess,
                    onClick = {
                        isSavingProfile = true
                        saveProfileError = null
                        scope.launch {
                            val ok = onSaveProfile(editedMajor.trim(), editedSchool.trim())
                            if (ok) {
                                // Save bank card locally to SharedPreferences
                                val prefs = context.getSharedPreferences("student_card_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putString("bank_card_num_$studentId", bankCardNum.trim()).apply()
                                saveProfileSuccess = true
                                kotlinx.coroutines.delay(1000)
                                showEditProfileSheet = false
                            } else {
                                saveProfileError = localizedText(
                                    appLocale,
                                    "Failed to save profile. Please try again.",
                                    "保存个人学籍信息失败，请重试。"
                                )
                            }
                            isSavingProfile = false
                        }
                    }
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = stringResource(R.string.dialog_logout_title)) },
            text = { Text(text = stringResource(R.string.dialog_logout_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(text = stringResource(R.string.profile_logout_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditProfileSheet = true },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(
                        modifier = Modifier.padding(start = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = studentName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.profile_subtitle, studentId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.profile_academic_summary, studyYear, major),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = school,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            IdentityInfoCard(
                studentId = studentId,
                studyYear = studyYear,
                major = major,
                school = school
            )
        }

        profileMessage?.let { message ->
            item {
                TipHelpText(text = message)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.profile_notify_updates_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.profile_notify_updates_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsOn,
                        onCheckedChange = { want ->
                            if (want) {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            setNotificationsEnabled(true)
                                        }
                                        else -> notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    }
                                } else {
                                    setNotificationsEnabled(true)
                                }
                            } else {
                                setNotificationsEnabled(false)
                            }
                        }
                    )
                }
            }
        }

        item {
            ProfileSectionTitle(
                title = stringResource(R.string.profile_requests_title),
                subtitle = stringResource(R.string.profile_requests_subtitle)
            )
        }

        if (requests.isEmpty()) {
            item {
                EmptyRequestsCard()
            }
        } else {
            items(requests.take(3), key = { it.id }) { request ->
                RequestSummaryCard(
                    request = request,
                    supportingText = "${request.id} - ${request.createdAtMillis.toRequestDate(requestDateFormatter)}",
                    onClick = { onRequestSelected(request.serviceId) }
                )
            }
        }

        item {
            ProfileSectionTitle(
                title = localizedText(appLocale, "Personal details", "个人信息"),
                subtitle = localizedText(
                    appLocale,
                    "A cleaner overview of your current records and services.",
                    "更清晰地查看当前个人记录与服务入口。"
                )
            )
        }

        item {
            MenuCard {
                personalItems.forEachIndexed { index, item ->
                    ProfileMenuRow(
                        title = item.title.resolve(appLocale),
                        subtitle = item.subtitle.resolve(appLocale),
                        trailingText = item.trailingText?.resolve(appLocale),
                        enabled = item.isEnabled,
                        onClick = null
                    )
                    if (index < personalItems.lastIndex) {
                        SoftDivider()
                    }
                }
            }
        }

        item {
            ProfileSectionTitle(
                title = localizedText(appLocale, "Account", "账号"),
                subtitle = localizedText(
                    appLocale,
                    "Language, password, and sign-out actions.",
                    "语言、密码和退出登录操作。"
                )
            )
        }

        item {
            MenuCard {
                accountItems.forEachIndexed { index, item ->
                    ProfileMenuRow(
                        title = item.title.resolve(appLocale),
                        subtitle = item.subtitle.resolve(appLocale),
                        trailingText = item.trailingText?.resolve(appLocale),
                        isDestructive = item.isDestructive,
                        enabled = item.isEnabled,
                        onClick = when (item.id) {
                            "language" -> {
                                { showLanguageSheet = true }
                            }

                            "edit_profile" -> {
                                { showEditProfileSheet = true }
                            }

                            "change_password" -> {
                                { showPasswordSheet = true }
                            }

                            "logout" -> {
                                { showLogoutDialog = true }
                            }

                            else -> null
                        }
                    )
                    if (index < accountItems.lastIndex) {
                        SoftDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityInfoCard(
    studentId: String,
    studyYear: String,
    major: String,
    school: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_identity_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.profile_identity_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IdentityInfoRow(
                label = stringResource(R.string.profile_student_id_label),
                value = studentId
            )
            IdentityInfoRow(
                label = stringResource(R.string.profile_study_year_label),
                value = studyYear
            )
            IdentityInfoRow(
                label = stringResource(R.string.profile_major_label),
                value = major
            )
            IdentityInfoRow(
                label = stringResource(R.string.profile_school_label),
                value = school
            )
            IdentityInfoRow(
                label = stringResource(R.string.student_id_format_label),
                value = stringResource(R.string.student_id_format_value)
            )
            IdentityInfoRow(
                label = stringResource(R.string.student_id_year_label),
                value = stringResource(R.string.student_id_year_value)
            )
            IdentityInfoRow(
                label = stringResource(R.string.student_id_sequence_label),
                value = stringResource(R.string.student_id_sequence_value)
            )
        }
    }
}

@Composable
private fun IdentityInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProfileSectionTitle(title: String, subtitle: String) {
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
private fun EmptyRequestsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_requests_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.profile_requests_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MenuCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            content = content
        )
    }
}

@Composable
private fun LanguageOptionRow(
    language: AppLanguage,
    selectedLanguage: AppLanguage,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = language == selectedLanguage,
            onClick = onSelected
        )
        Text(
            text = stringResource(language.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun buildPersonalItems(requestCount: Int): List<ProfileMenuItemUiModel> {
    return listOf(
        ProfileMenuItemUiModel(
            id = "application_record",
            title = uiText("Application record", "申请记录"),
            subtitle = uiText("Recent requests and submission history", "最近申请与提交记录"),
            trailingText = uiText(requestCount.toString())
        ),
        ProfileMenuItemUiModel(
            id = "manage_method_list",
            title = uiText("Manage method list", "办理方式清单"),
            subtitle = uiText("Reference notes for current student-service flows", "当前学生服务流程参考说明")
        ),
        ProfileMenuItemUiModel(
            id = "awards_punishments",
            title = uiText("Awards and punishments list", "奖惩记录"),
            subtitle = uiText(
                "Scholarship review: no outstanding issues this term.",
                "奖学金与日常表现：本学期暂无待处理事项。",
            )
        ),
        ProfileMenuItemUiModel(
            id = "credit_confirmation",
            title = uiText("Credit confirmation list", "学分确认"),
            subtitle = uiText("Credits and course confirmation overview", "学分与课程确认概览")
        ),
        ProfileMenuItemUiModel(
            id = "student_status_confirmation",
            title = uiText("Student status confirmation list", "学籍确认"),
            subtitle = uiText("Current enrollment and registration state", "当前在读与学籍状态")
        )
    )
}

private fun buildAccountItems(selectedLanguage: AppLanguage): List<ProfileMenuItemUiModel> {
    return listOf(
        ProfileMenuItemUiModel(
            id = "language",
            title = uiText("Language", "语言设置"),
            subtitle = uiText("Switch between English, Chinese, or system", "切换英文、中文或跟随系统"),
            trailingText = uiText(
                selectedLanguage.name.lowercase().replaceFirstChar(Char::titlecase),
                when (selectedLanguage) {
                    AppLanguage.SYSTEM -> "跟随系统"
                    AppLanguage.ENGLISH -> "English"
                    AppLanguage.CHINESE -> "中文"
                }
            )
        ),
        ProfileMenuItemUiModel(
            id = "edit_profile",
            title = uiText("Edit academic details", "编辑学籍信息"),
            subtitle = uiText("Update the current major and school department", "更新当前在读的专业与学院记录")
        ),
        ProfileMenuItemUiModel(
            id = "change_password",
            title = uiText("Change the password", "修改密码"),
            subtitle = uiText("Update the login password stored on this device", "更新保存在本机的登录密码")
        ),
        ProfileMenuItemUiModel(
            id = "logout",
            title = uiText("Log out", "退出登录"),
            subtitle = uiText("Return to the student login screen", "返回学生登录页面"),
            isDestructive = true
        )
    )
}

private fun localizedText(locale: Locale, english: String, chinese: String): String {
    return if (locale.language.startsWith("zh")) chinese else english
}

private fun Long.toRequestDate(formatter: DateTimeFormatter): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

@Composable
fun StudentQrCode(studentId: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val sizePx = size.width
        val cellSize = sizePx / 15f
        
        // Draw background
        drawRect(color = Color.White)
        
        // Function to draw QR finder pattern
        fun drawFinderPattern(x: Float, y: Float) {
            // Outer 7x7 cell block
            drawRect(
                color = Color.Black,
                topLeft = Offset(x * cellSize, y * cellSize),
                size = Size(cellSize * 7, cellSize * 7)
            )
            // Inner white 5x5 cell block
            drawRect(
                color = Color.White,
                topLeft = Offset((x + 1) * cellSize, (y + 1) * cellSize),
                size = Size(cellSize * 5, cellSize * 5)
            )
            // Inner solid black 3x3 cell block
            drawRect(
                color = Color.Black,
                topLeft = Offset((x + 2) * cellSize, (y + 2) * cellSize),
                size = Size(cellSize * 3, cellSize * 3)
            )
        }
        
        // 1. Top-Left finder pattern
        drawFinderPattern(0f, 0f)
        // 2. Top-Right finder pattern
        drawFinderPattern(8f, 0f)
        // 3. Bottom-Left finder pattern
        drawFinderPattern(0f, 8f)
        
        // 4. Alignments and random bits (deterministic based on studentId hash!)
        val hash = studentId.hashCode()
        val random = java.util.Random(hash.toLong())
        for (r in 0 until 15) {
            for (c in 0 until 15) {
                // Skip finder patterns
                if ((r < 8 && c < 8) || (r < 8 && c > 7) || (r > 7 && c < 8)) {
                    continue
                }
                if (random.nextBoolean()) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}
