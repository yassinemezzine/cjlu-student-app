package com.cjlu.studentapp.ui.screens

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector
import com.cjlu.studentapp.R
import com.cjlu.studentapp.ui.forms.UiText
import java.util.Locale

enum class ServiceCategory(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    @param:StringRes val turnaroundRes: Int,
    val checklistRes: List<Int>
) {
    International(
        labelRes = R.string.services_section_international_title,
        icon = Icons.Filled.Public,
        turnaroundRes = R.string.service_time_international,
        checklistRes = listOf(
            R.string.service_check_international_1,
            R.string.service_check_international_2,
            R.string.service_check_international_3
        )
    ),
    Academic(
        labelRes = R.string.services_section_academic_title,
        icon = Icons.Filled.School,
        turnaroundRes = R.string.service_time_academic,
        checklistRes = listOf(
            R.string.service_check_academic_1,
            R.string.service_check_academic_2,
            R.string.service_check_academic_3
        )
    ),
    Learning(
        labelRes = R.string.services_section_learning_title,
        icon = Icons.AutoMirrored.Filled.MenuBook,
        turnaroundRes = R.string.service_time_learning,
        checklistRes = listOf(
            R.string.service_check_learning_1,
            R.string.service_check_learning_2,
            R.string.service_check_learning_3
        )
    ),
    Campus(
        labelRes = R.string.services_section_campus_title,
        icon = Icons.Filled.HomeWork,
        turnaroundRes = R.string.service_time_campus,
        checklistRes = listOf(
            R.string.service_check_campus_1,
            R.string.service_check_campus_2,
            R.string.service_check_campus_3
        )
    )
}

data class ServiceItem(
    val id: String,
    @param:StringRes val titleRes: Int = 0,
    @param:StringRes val descriptionRes: Int = 0,
    val titleText: UiText? = null,
    val descriptionText: UiText? = null,
    val category: ServiceCategory,
    val isPopular: Boolean = false,
    /** When set (e.g. from API), overrides category turnaround strings. */
    val networkTurnaround: String? = null,
    /** When non-empty (e.g. from API), overrides category checklist string resources. */
    val networkChecklist: List<String>? = null,
) {
    fun resolveTitle(context: Context, locale: Locale): String {
        return when {
            titleRes != 0 -> context.getString(titleRes)
            titleText != null -> titleText.resolve(locale)
            else -> id
        }
    }

    fun resolveDescription(context: Context, locale: Locale): String {
        return when {
            descriptionRes != 0 -> context.getString(descriptionRes)
            descriptionText != null -> descriptionText.resolve(locale)
            else -> ""
        }
    }

    fun resolveTurnaround(context: Context, @Suppress("UNUSED_PARAMETER") locale: Locale): String {
        val fromNet = networkTurnaround?.trim().orEmpty()
        if (fromNet.isNotEmpty()) return fromNet
        return context.getString(category.turnaroundRes)
    }

    fun resolveChecklist(context: Context, locale: Locale): List<String> {
        val fromNet = networkChecklist
        if (!fromNet.isNullOrEmpty()) return fromNet
        return category.checklistRes.map { context.getString(it) }
    }
}

object ServiceCatalog {
    val categories = listOf(
        ServiceCategory.International,
        ServiceCategory.Academic,
        ServiceCategory.Learning,
        ServiceCategory.Campus
    )
}
