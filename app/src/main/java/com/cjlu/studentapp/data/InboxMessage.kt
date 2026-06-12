package com.cjlu.studentapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cjlu.studentapp.network.api.MessageDto

@Entity(tableName = "inbox_messages")
data class InboxMessage(
    @PrimaryKey val id: String,
    val studentId: String,
    val category: String,
    val sender: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val relatedServiceId: String?,
    val requiresAction: Boolean,
    val isRead: Boolean,
)

fun MessageDto.toEntity(studentId: String): InboxMessage =
    InboxMessage(
        id = id,
        studentId = studentId.trim(),
        category = category,
        sender = sender,
        title = title,
        body = body,
        timeLabel = timeLabel,
        relatedServiceId = relatedServiceId,
        requiresAction = requiresAction,
        isRead = isRead,
    )

fun InboxMessage.toDto(): MessageDto =
    MessageDto(
        id = id,
        category = category,
        sender = sender,
        title = title,
        body = body,
        timeLabel = timeLabel,
        relatedServiceId = relatedServiceId,
        requiresAction = requiresAction,
        isRead = isRead,
    )
