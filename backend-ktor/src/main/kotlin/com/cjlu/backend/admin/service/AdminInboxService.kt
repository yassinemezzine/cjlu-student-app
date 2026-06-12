package com.cjlu.backend.admin.service

import com.cjlu.backend.Database
import com.cjlu.backend.websocket.WebSocketHub

object AdminInboxService {

    sealed class SendResult {
        data object Success : SendResult()
        data object EmptyFields : SendResult()
        data object UnknownStudent : SendResult()
    }

    suspend fun sendFromAdminForm(
        audienceAll: Boolean,
        studentId: String,
        categoryRaw: String,
        titleEn: String,
        bodyEn: String,
        titleZh: String,
        bodyZh: String,
        requiresAction: Boolean,
        relatedServiceIdRaw: String,
    ): SendResult {
        val title = titleEn.trim()
        val body = bodyEn.trim()
        if (title.isEmpty() || body.isEmpty()) return SendResult.EmptyFields

        val category = categoryRaw.trim().ifEmpty { "Announcements" }.take(32)
        val zhTitle = titleZh.trim().ifEmpty { title }
        val zhBody = bodyZh.trim().ifEmpty { body }
        val rel = relatedServiceIdRaw.trim().takeIf { it.isNotEmpty() }?.take(50)

        val recipient: String? =
            if (audienceAll) {
                null
            } else {
                val sid = studentId.trim()
                if (sid.isEmpty()) return SendResult.EmptyFields
                if (!Database.studentExists(sid)) return SendResult.UnknownStudent
                sid
            }

        Database.insertAdminInboxMessage(
            recipientStudentId = recipient,
            category = category,
            senderEn = "CJLU Administration",
            senderZh = "浙江理工大学科艺学院 · 管理端",
            titleEn = title,
            titleZh = zhTitle,
            bodyEn = body,
            bodyZh = zhBody,
            relatedServiceId = rel,
            requiresAction = requiresAction,
        )

        if (recipient == null) {
            for (s in Database.listStudentSummaries()) {
                WebSocketHub.notifyMessagesChanged(s.studentId)
            }
            return SendResult.Success
        }
        WebSocketHub.notifyMessagesChanged(recipient)
        return SendResult.Success
    }
}
