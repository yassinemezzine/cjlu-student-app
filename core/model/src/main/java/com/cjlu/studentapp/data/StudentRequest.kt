package com.cjlu.studentapp.data

import androidx.annotation.StringRes
import com.cjlu.core.resources.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RequestStatus(
    val storageKey: String,
    @param:StringRes val labelRes: Int,
) {
    @SerialName("submitted")
    Submitted(
        storageKey = "submitted",
        labelRes = R.string.request_status_submitted
    ),
    @SerialName("in_review")
    InReview(
        storageKey = "in_review",
        labelRes = R.string.request_status_in_review
    ),
    @SerialName("action_needed")
    ActionNeeded(
        storageKey = "action_needed",
        labelRes = R.string.request_status_action_needed
    ),
    @SerialName("completed")
    Completed(
        storageKey = "completed",
        labelRes = R.string.request_status_completed
    );

    companion object {
        fun fromStorageKey(value: String): RequestStatus {
            return entries.firstOrNull { it.storageKey == value } ?: Submitted
        }
    }
}

@Serializable
data class StudentRequest(
    val id: String,
    val serviceId: String,
    val studentId: String,
    val contactInfo: String,
    val notes: String,
    val status: RequestStatus,
    val createdAtMillis: Long,
    val attachmentUrl: String? = null
)

typealias RequestSubmission = com.cjlu.contract.RequestSubmission
