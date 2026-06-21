package com.cjlu.studentapp.network

import com.cjlu.studentapp.data.RequestSubmission
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.network.api.CatalogServiceDto
import com.cjlu.studentapp.network.api.ChangePasswordRequest
import com.cjlu.studentapp.network.api.FcmTokenRequest
import com.cjlu.studentapp.network.api.LoginRequest
import com.cjlu.studentapp.network.api.LoginResponse
import com.cjlu.studentapp.network.api.MessageReadBody
import com.cjlu.studentapp.network.api.MessageDto
import com.cjlu.studentapp.network.api.OkResponse
import com.cjlu.studentapp.network.api.PatchProfileRequest
import com.cjlu.studentapp.network.api.StudentAcademicCalendarDto
import com.cjlu.studentapp.network.api.StudentAttendanceDetailDto
import com.cjlu.studentapp.network.api.StudentDormitoryDto
import com.cjlu.studentapp.network.api.StudentProfileDto
import com.cjlu.studentapp.network.api.StudentTimetableDto
import com.cjlu.studentapp.network.api.StudentTranscriptDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CJLUService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): OkResponse

    @GET("auth/me")
    suspend fun me(): StudentProfileDto

    @GET("services")
    suspend fun services(): List<CatalogServiceDto>

    @GET("students/{studentId}/profile")
    suspend fun getProfile(@Path("studentId") studentId: String): StudentProfileDto

    @HTTP(method = "PATCH", path = "students/{studentId}/profile", hasBody = true)
    suspend fun patchProfile(
        @Path("studentId") studentId: String,
        @Body body: PatchProfileRequest,
    ): StudentProfileDto

    @GET("students/{studentId}/messages")
    suspend fun getMessages(@Path("studentId") studentId: String): List<MessageDto>

    @HTTP(method = "PATCH", path = "students/{studentId}/messages/{messageId}/read", hasBody = true)
    suspend fun patchMessageRead(
        @Path("studentId") studentId: String,
        @Path("messageId") messageId: String,
        @Body body: MessageReadBody,
    ): OkResponse

    @POST("students/{studentId}/messages/{messageId}/read")
    suspend fun markMessageRead(
        @Path("studentId") studentId: String,
        @Path("messageId") messageId: String,
        @Body body: MessageReadBody,
    ): OkResponse

    @GET("students/{studentId}/academic/attendance")
    suspend fun getAttendanceDetail(@Path("studentId") studentId: String): StudentAttendanceDetailDto

    @GET("students/{studentId}/academic/transcript")
    suspend fun getTranscript(@Path("studentId") studentId: String): StudentTranscriptDto

    @GET("students/{studentId}/academic/timetable")
    suspend fun getTimetable(@Path("studentId") studentId: String): StudentTimetableDto

    @GET("students/{studentId}/academic/calendar")
    suspend fun getAcademicCalendar(@Path("studentId") studentId: String): StudentAcademicCalendarDto

    @GET("students/{studentId}/dormitory")
    suspend fun getDormitory(@Path("studentId") studentId: String): StudentDormitoryDto

    @GET("students/{studentId}/requests")
    suspend fun getRequests(@Path("studentId") studentId: String): List<StudentRequest>

    @POST("requests")
    suspend fun submitRequest(@Body submission: RequestSubmission): StudentRequest

    @Multipart
    @POST("requests/{id}/upload")
    suspend fun uploadAttachment(
        @Path("id") requestId: String,
        @Part file: MultipartBody.Part,
    ): StudentRequest

    @POST("students/{studentId}/fcm-token")
    suspend fun registerFcmToken(
        @Path("studentId") studentId: String,
        @Body body: FcmTokenRequest,
    ): OkResponse

    @DELETE("students/{studentId}/fcm-token")
    suspend fun unregisterFcmToken(
        @Path("studentId") studentId: String,
        @Body body: FcmTokenRequest,
    ): OkResponse
}
