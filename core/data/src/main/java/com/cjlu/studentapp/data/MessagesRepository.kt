package com.cjlu.studentapp.data

import android.content.Context
import android.util.Log
import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.network.api.MessageDto
import com.cjlu.studentapp.network.api.MessageReadBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object MessagesRepository {
    private const val TAG = "MessagesRepository"

    fun observeMessages(context: Context, studentId: String): Flow<List<MessageDto>> {
        val sid = studentId.trim()
        return AppDatabase.getDatabase(context)
            .inboxMessageDao()
            .observeForStudent(sid)
            .map { rows -> rows.map { it.toDto() } }
    }

    suspend fun syncMessages(context: Context, studentId: String): Pair<List<MessageDto>, Boolean> {
        val sid = studentId.trim()
        val dao = AppDatabase.getDatabase(context).inboxMessageDao()
        return try {
            val apiMessages = RetrofitClient.instance.getMessages(sid)
            dao.deleteForStudent(sid)
            dao.insertAll(apiMessages.map { it.toEntity(sid) })
            apiMessages to true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync messages from API, using local database", e)
            dao.getForStudent(sid).map { it.toDto() } to false
        }
    }

    suspend fun setMessageRead(
        context: Context,
        studentId: String,
        messageId: String,
        read: Boolean,
    ): Boolean {
        val sid = studentId.trim()
        return try {
            RetrofitClient.instance.patchMessageRead(
                sid,
                messageId,
                MessageReadBody(read = read),
            )
            AppDatabase.getDatabase(context)
                .inboxMessageDao()
                .updateRead(sid, messageId, read)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update message read state", e)
            false
        }
    }
}
