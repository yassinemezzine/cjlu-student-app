package com.cjlu.studentapp.ui.forms

import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale

data class UiText(
    val en: String,
    val zh: String = en
) {
    fun resolve(locale: Locale): String {
        return if (locale.language.startsWith("zh")) zh else en
    }
}

fun uiText(en: String, zh: String = en): UiText {
    return UiText(en = en, zh = zh)
}

enum class PrefillSource {
    None,
    StudentId,
    StudentName,
    StudyYear,
    Major,
    School
}

enum class TextInputValidation {
    None,
    StudentId
}

data class OptionItem(
    val id: String,
    val label: UiText
)

sealed class FormFieldUiModel(
    open val key: String,
    open val label: UiText,
    open val required: Boolean = false
) {
    data class ReadOnly(
        override val key: String,
        override val label: UiText,
        val value: UiText,
        val trailingAction: UiText? = null
    ) : FormFieldUiModel(
        key = key,
        label = label
    )

    data class TextInput(
        override val key: String,
        override val label: UiText,
        val placeholder: UiText? = null,
        val helper: UiText? = null,
        override val required: Boolean = false,
        val digitsOnly: Boolean = false,
        val maxLength: Int = 80,
        val keyboardType: KeyboardType = KeyboardType.Text,
        val prefillSource: PrefillSource = PrefillSource.None,
        val validation: TextInputValidation = TextInputValidation.None
    ) : FormFieldUiModel(
        key = key,
        label = label,
        required = required
    )

    data class MultilineInput(
        override val key: String,
        override val label: UiText,
        val placeholder: UiText? = null,
        val helper: UiText? = null,
        override val required: Boolean = false,
        val maxLength: Int = 240
    ) : FormFieldUiModel(
        key = key,
        label = label,
        required = required
    )

    data class DatePicker(
        override val key: String,
        override val label: UiText,
        val helper: UiText? = null,
        override val required: Boolean = false
    ) : FormFieldUiModel(
        key = key,
        label = label,
        required = required
    )

    data class Dropdown(
        override val key: String,
        override val label: UiText,
        val options: List<OptionItem>,
        val helper: UiText? = null,
        override val required: Boolean = false,
        val defaultOptionId: String? = options.firstOrNull()?.id
    ) : FormFieldUiModel(
        key = key,
        label = label,
        required = required
    )

    data class RadioGroup(
        override val key: String,
        override val label: UiText,
        val options: List<OptionItem>,
        val helper: UiText? = null,
        override val required: Boolean = false,
        val defaultOptionId: String? = options.firstOrNull()?.id
    ) : FormFieldUiModel(
        key = key,
        label = label,
        required = required
    )
}

data class UploadFieldUiModel(
    val key: String,
    val label: UiText,
    val helper: UiText? = null,
    val required: Boolean = false,
    val dashedBorder: Boolean = true,
    val suggestedFileName: UiText = uiText(
        en = "supporting_document.pdf",
        zh = "支撑材料.pdf",
    )
)

data class AgreementBlockUiModel(
    val key: String,
    val text: UiText,
    val required: Boolean = true
)

data class ServiceFormUiModel(
    val serviceId: String,
    val intro: UiText,
    val fields: List<FormFieldUiModel>,
    val uploads: List<UploadFieldUiModel> = emptyList(),
    val agreements: List<AgreementBlockUiModel> = emptyList(),
    val tips: List<UiText> = emptyList()
)

data class ProfileMenuItemUiModel(
    val id: String,
    val title: UiText,
    val subtitle: UiText,
    val trailingText: UiText? = null,
    val isDestructive: Boolean = false,
    val isEnabled: Boolean = true
)
