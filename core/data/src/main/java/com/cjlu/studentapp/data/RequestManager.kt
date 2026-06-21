package com.cjlu.studentapp.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.network.api.RequestSubmission
import com.cjlu.studentapp.util.baseNameWithoutExtension
import com.cjlu.studentapp.util.extensionForUpload
import com.cjlu.studentapp.util.mimeTypeForUri
import com.cjlu.studentapp.util.resolveAttachmentDisplayName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream

object RequestManager {

    fun observeRequests(context: Context, studentId: String): Flow<List<StudentRequest>> =
        AppDatabase.getDatabase(context)
            .studentRequestDao()
            .observeForStudent(studentId.trim())
            .map { requests -> requests.map { it.toDomain() } }

    suspend fun syncRequests(context: Context, studentId: String): Pair<List<StudentRequest>, Boolean> {
        val sid = studentId.trim()
        val dao = AppDatabase.getDatabase(context).studentRequestDao()

        return try {
            val apiRequests = RetrofitClient.instance.getRequests(sid)
            dao.deleteForStudent(sid)
            dao.insertAll(apiRequests.map { it.toEntity() })
            apiRequests to true
        } catch (e: Exception) {
            Log.e("RequestManager", "Failed to sync requests from API, using local database", e)
            dao.getRequestsForStudent(sid).map { it.toDomain() } to false
        }
    }

    suspend fun createRequest(
        context: Context,
        submission: RequestSubmission,
        attachmentUri: Uri? = null,
    ): Result<StudentRequest> {
        val dao = AppDatabase.getDatabase(context).studentRequestDao()

        return try {
            val createdRequest = RetrofitClient.instance.submitRequest(submission)
            val finalRequest = if (attachmentUri != null) {
                try {
                    uploadAttachment(context, createdRequest.id, attachmentUri)
                } catch (e: Exception) {
                    Log.e("RequestManager", "Failed to upload attachment", e)
                    return Result.failure(e)
                }
            } else {
                createdRequest
            }

            dao.insert(finalRequest.toEntity())
            Result.success(finalRequest)
        } catch (e: HttpException) {
            Log.e("RequestManager", "Failed to submit request http ${e.code()}", e)
            Result.failure(Exception(httpSubmitHint(e)))
        } catch (e: Exception) {
            Log.e("RequestManager", "Failed to submit request to API", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadAttachment(context: Context, requestId: String, attachmentUri: Uri): StudentRequest {
        val file = uriToCacheFile(context, attachmentUri)
        val mime = context.mimeTypeForUri(attachmentUri)
        val mediaType = mime.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
        val requestFile = file.asRequestBody(mediaType)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        return RetrofitClient.instance.uploadAttachment(requestId, body)
    }

    private fun httpSubmitHint(e: HttpException): String {
        val body = try {
            e.response()?.errorBody()?.string()?.trim()?.take(280)
        } catch (_: Exception) {
            null
        }
        if (!body.isNullOrBlank()) return body

        return when (e.code()) {
            403 ->
                "This action was denied. Sign in with the correct student account, or check that your request belongs to you."
            401 -> "Session expired or not authorized. Sign in again."
            else -> e.message()?.takeIf { it.isNotBlank() } ?: "Request failed (HTTP ${e.code()})"
        }
    }

    private fun uriToCacheFile(context: Context, uri: Uri): File {
        val displayName = context.resolveAttachmentDisplayName(uri)
        val mime = context.mimeTypeForUri(uri)
        val ext = extensionForUpload(displayName, mime)
        val base = baseNameWithoutExtension(displayName)
        val file = File(context.cacheDir, "${base}_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: error("Could not open attachment for reading")
        return file
    }
}
