package com.cjlu.studentapp.network

import com.cjlu.studentapp.data.RequestStatus
import com.cjlu.studentapp.data.StudentRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class RealtimePushAction {
    data object RefreshRequests : RealtimePushAction()
    data object RefreshMessages : RealtimePushAction()
    data object SyncLearningAlerts : RealtimePushAction()
    data object InvalidateAcademicCache : RealtimePushAction()
    data class RequestUpdated(val request: StudentRequest) : RealtimePushAction()
}

object RealtimePushHandler {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(message: String): RealtimePushAction? {
        if (message == "REFRESH") return RealtimePushAction.RefreshRequests
        if (!message.startsWith("{")) return null

        val obj = json.parseToJsonElement(message).jsonObject
        when (obj.stringOrNull("type")) {
            "messages" -> return RealtimePushAction.RefreshMessages
            "learning_alerts" -> return RealtimePushAction.SyncLearningAlerts
            "academic_updated" -> return RealtimePushAction.InvalidateAcademicCache
        }

        val looksLikeLearningPush =
            obj.containsKey("overallAttendancePercent") &&
                obj.containsKey("classUpdateAtMillis") &&
                !obj.containsKey("serviceId")
        if (looksLikeLearningPush) {
            return RealtimePushAction.SyncLearningAlerts
        }

        if (!obj.containsKey("serviceId")) {
            return RealtimePushAction.RefreshRequests
        }

        return RealtimePushAction.RequestUpdated(
            StudentRequest(
                id = obj.requireString("id"),
                serviceId = obj.requireString("serviceId"),
                studentId = obj.requireString("studentId"),
                contactInfo = obj.requireString("contactInfo"),
                notes = obj.requireString("notes"),
                status = RequestStatus.fromStorageKey(obj.requireString("status").lowercase()),
                createdAtMillis = obj.requireLong("createdAtMillis"),
                attachmentUrl = obj.stringOrNull("attachmentUrl"),
            ),
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }

    private fun JsonObject.requireString(key: String): String =
        stringOrNull(key) ?: error("Missing $key in realtime push")

    private fun JsonObject.requireLong(key: String): Long =
        get(key)?.jsonPrimitive?.content?.toLongOrNull() ?: error("Missing $key in realtime push")
}
