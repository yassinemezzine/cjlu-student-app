package com.cjlu.studentapp.data

import com.cjlu.studentapp.network.api.MessageDto

fun StudentRequest.toEntity(): StudentRequestEntity =
    StudentRequestEntity(
        id = id,
        serviceId = serviceId,
        studentId = studentId,
        contactInfo = contactInfo,
        notes = notes,
        status = status,
        createdAtMillis = createdAtMillis,
        attachmentUrl = attachmentUrl,
    )

fun StudentRequestEntity.toDomain(): StudentRequest =
    StudentRequest(
        id = id,
        serviceId = serviceId,
        studentId = studentId,
        contactInfo = contactInfo,
        notes = notes,
        status = status,
        createdAtMillis = createdAtMillis,
        attachmentUrl = attachmentUrl,
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
