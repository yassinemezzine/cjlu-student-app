package com.cjlu.studentapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun FormRow(
    label: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            content()
            if (!helperText.isNullOrBlank()) {
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReadOnlyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingActionText: String? = null,
    onClick: (() -> Unit)? = null
) {
    FormRow(label = label, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            if (!trailingActionText.isNullOrBlank()) {
                Text(
                    text = trailingActionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
fun TextInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    isError: Boolean = false,
    digitsOnly: Boolean = false,
    maxLength: Int = 80,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    FormRow(
        label = label,
        modifier = modifier,
        helperText = helperText
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                val filtered = if (digitsOnly) input.filter(Char::isDigit) else input
                onValueChange(filtered.take(maxLength))
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                if (!placeholder.isNullOrBlank()) {
                    Text(text = placeholder)
                }
            },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation
        )
    }
}

@Composable
fun MultilineInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    maxLength: Int = 240
) {
    FormRow(
        label = label,
        modifier = modifier,
        helperText = helperText
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                if (!placeholder.isNullOrBlank()) {
                    Text(text = placeholder)
                }
            },
            minLines = 4
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerRow(
    label: String,
    selectedDateText: String,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helperText: String? = null,
    initialDateMillis: Long? = null,
    confirmText: String = "OK",
    dismissText: String = "Cancel"
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateSelected(pickerState.selectedDateMillis)
                        showPicker = false
                    }
                ) {
                    Text(text = confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = dismissText)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    FormRow(
        label = label,
        modifier = modifier,
        helperText = helperText
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDateText.isBlank()) placeholder else selectedDateText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedDateText.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelectRow(
    label: String,
    selectedText: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    placeholder: String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    FormRow(
        label = label,
        modifier = modifier,
        helperText = helperText
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                readOnly = true,
                placeholder = {
                    if (selectedText.isBlank() && placeholder.isNotBlank()) {
                        Text(text = placeholder)
                    }
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.second) },
                        onClick = {
                            onOptionSelected(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RadioGroupRow(
    label: String,
    selectedOptionId: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null
) {
    FormRow(
        label = label,
        modifier = modifier,
        helperText = helperText
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option.first) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOptionId == option.first,
                        onClick = { onOptionSelected(option.first) }
                    )
                    Text(
                        text = option.second,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CheckboxAgreementBlock(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UploadAttachmentBox(
    label: String,
    fileName: String,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    dashedBorder: Boolean = true,
    emptyFileText: String = "Tap to upload an attachment"
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    FormRow(
        label = label,
        modifier = modifier,
        helperText = helperText
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = if (dashedBorder) {
                            PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
                        } else {
                            null
                        },
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                        style = stroke
                    )
                }
                .clickable(onClick = onUploadClick)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (fileName.isBlank()) emptyFileText else fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (fileName.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
fun TipHelpText(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FormSubmitButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text = text)
    }
}

@Composable
fun ProfileMenuRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled && onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    )
}

fun formatDateForDisplay(
    dateMillis: Long?,
    locale: Locale
): String {
    if (dateMillis == null) {
        return ""
    }

    return Instant.ofEpochMilli(dateMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}
