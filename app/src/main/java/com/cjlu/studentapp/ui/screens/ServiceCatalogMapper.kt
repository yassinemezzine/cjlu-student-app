package com.cjlu.studentapp.ui.screens

import com.cjlu.studentapp.network.api.CatalogServiceDto
import com.cjlu.studentapp.ui.forms.UiText
import java.util.Locale

private fun normalizeCategory(raw: String): ServiceCategory {
    val normalized = raw.trim().replace("_", "").replace("-", "").replace(" ", "").lowercase()
    return when (normalized) {
        "international" -> ServiceCategory.International
        "academic" -> ServiceCategory.Academic
        "learning", "studentlearning" -> ServiceCategory.Learning
        "campus" -> ServiceCategory.Campus
        else -> runCatching { ServiceCategory.valueOf(raw) }.getOrElse { ServiceCategory.Campus }
    }
}

private fun normalizeServiceId(raw: String): String {
    return when (raw.trim().lowercase()) {
        "student_learning", "studentlearning" -> "class_schedule"
        else -> raw
    }
}

fun CatalogServiceDto.toServiceItem(): ServiceItem {
    val cat = normalizeCategory(category)
    val t = title
    val d = description
    return ServiceItem(
        id = normalizeServiceId(id),
        titleText = UiText(en = t, zh = t),
        descriptionText = UiText(en = d, zh = d),
        category = cat,
        isPopular = isPopular,
        networkTurnaround = turnaround,
        networkChecklist = checklist,
    )
}

fun CatalogServiceDto.isRequestService(): Boolean =
    id == "changing_room" || id == "ask_leave"

fun List<CatalogServiceDto>.resolveServiceTitle(serviceId: String, @Suppress("UNUSED_PARAMETER") locale: Locale): String =
    firstOrNull { it.id == serviceId }?.title ?: serviceId
