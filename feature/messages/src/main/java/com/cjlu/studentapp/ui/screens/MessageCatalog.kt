package com.cjlu.studentapp.ui.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector
import com.cjlu.core.resources.R

enum class MessageCategory(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    International(
        labelRes = R.string.message_category_international,
        icon = Icons.Filled.Public
    ),
    Academic(
        labelRes = R.string.message_category_academic,
        icon = Icons.Filled.School
    ),
    Campus(
        labelRes = R.string.message_category_campus,
        icon = Icons.Filled.Apartment
    ),
    Announcements(
        labelRes = R.string.message_category_announcements,
        icon = Icons.Filled.Campaign
    )
}

data class MessageItem(
    val id: String,
    @param:StringRes val senderRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    @param:StringRes val timeRes: Int,
    val category: MessageCategory,
    val relatedServiceId: String? = null,
    val startsUnread: Boolean = true,
    val requiresAction: Boolean = false
)

object MessageCatalog {
    val messages = listOf(
        MessageItem(
            id = "residence_permit_docs",
            senderRes = R.string.message_1_sender,
            titleRes = R.string.message_1_title,
            bodyRes = R.string.message_1_body,
            timeRes = R.string.message_1_time,
            category = MessageCategory.International,
            relatedServiceId = "residence_permit",
            startsUnread = true,
            requiresAction = true
        ),
        MessageItem(
            id = "repair_request_update",
            senderRes = R.string.message_2_sender,
            titleRes = R.string.message_2_title,
            bodyRes = R.string.message_2_body,
            timeRes = R.string.message_2_time,
            category = MessageCategory.Campus,
            relatedServiceId = "repair_request",
            startsUnread = true,
            requiresAction = false
        ),
        MessageItem(
            id = "classroom_update",
            senderRes = R.string.message_3_sender,
            titleRes = R.string.message_3_title,
            bodyRes = R.string.message_3_body,
            timeRes = R.string.message_3_time,
            category = MessageCategory.Academic,
            relatedServiceId = "class_schedule",
            startsUnread = false,
            requiresAction = false
        ),
        MessageItem(
            id = "return_campus_deadline",
            senderRes = R.string.message_4_sender,
            titleRes = R.string.message_4_title,
            bodyRes = R.string.message_4_body,
            timeRes = R.string.message_4_time,
            category = MessageCategory.International,
            relatedServiceId = "back_to_cjlu",
            startsUnread = true,
            requiresAction = true
        ),
        MessageItem(
            id = "attendance_follow_up",
            senderRes = R.string.message_5_sender,
            titleRes = R.string.message_5_title,
            bodyRes = R.string.message_5_body,
            timeRes = R.string.message_5_time,
            category = MessageCategory.Announcements,
            relatedServiceId = "attendance_rate",
            startsUnread = false,
            requiresAction = true
        )
    )

    fun find(messageId: String): MessageItem? {
        return messages.firstOrNull { it.id == messageId }
    }
}
