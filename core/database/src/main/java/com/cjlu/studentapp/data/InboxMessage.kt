package com.cjlu.studentapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
