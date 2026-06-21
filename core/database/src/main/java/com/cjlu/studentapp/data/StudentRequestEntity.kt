package com.cjlu.studentapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "student_requests")
data class StudentRequestEntity(
    @PrimaryKey val id: String,
    val serviceId: String,
    val studentId: String,
    val contactInfo: String,
    val notes: String,
    val status: RequestStatus,
    val createdAtMillis: Long,
    val attachmentUrl: String? = null,
)

class RequestStatusConverters {
    @TypeConverter
    fun fromStatus(status: RequestStatus): String = status.storageKey

    @TypeConverter
    fun toStatus(value: String): RequestStatus = RequestStatus.fromStorageKey(value)
}
