package com.cjlu.studentapp.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.cjlu.studentapp.BuildConfig
import com.cjlu.studentapp.network.AuthTokenStore
import com.cjlu.studentapp.notifications.FcmTokenRegistrar
import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.network.api.ChangePasswordRequest
import com.cjlu.studentapp.network.api.LoginRequest
import com.cjlu.studentapp.network.api.PatchProfileRequest
import com.cjlu.studentapp.network.api.StudentProfileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class AuthSession(
    val isLoggedIn: Boolean,
    val studentId: String,
    val studentName: String,
    val studyYear: String,
    val major: String,
    val school: String,
    val overallAttendancePercent: Int = 96,
    val classUpdateNotice: String? = null,
    val classUpdateAtMillis: Long = 0L,
)

object AuthManager {
    const val DEFAULT_STUDENT_ID = ""

    private const val TAG = "AuthManager"
    private const val PREFERENCES_NAME = "cjlu_auth"
    private const val LOGGED_IN_KEY = "logged_in"
    private const val STUDENT_ID_KEY = "student_id"
    private const val STUDENT_NAME_KEY = "student_name"
    private const val STUDY_YEAR_KEY = "study_year"
    private const val MAJOR_KEY = "major"
    private const val SCHOOL_KEY = "school"
    private const val OVERALL_ATTENDANCE_KEY = "overall_attendance_percent"
    private const val CLASS_UPDATE_NOTICE_KEY = "class_update_notice"
    private const val CLASS_UPDATE_AT_KEY = "class_update_at_millis"
    private const val JWT_TOKEN_KEY = "jwt_token"

    fun loadSession(context: Context): AuthSession {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        AuthTokenStore.accessToken = preferences.getString(JWT_TOKEN_KEY, null)
        val storedId = preferences.getString(STUDENT_ID_KEY, DEFAULT_STUDENT_ID).orEmpty()
            .ifBlank { DEFAULT_STUDENT_ID }
        val roster = ClassRoster.find(storedId)
        return AuthSession(
            isLoggedIn = preferences.getBoolean(LOGGED_IN_KEY, false),
            studentId = storedId,
            studentName = preferences.getString(STUDENT_NAME_KEY, null).orEmpty()
                .ifBlank { roster?.displayName ?: "Student" },
            studyYear = preferences.getString(STUDY_YEAR_KEY, null).orEmpty()
                .ifBlank { roster?.classSection ?: "" },
            major = preferences.getString(MAJOR_KEY, null).orEmpty()
                .ifBlank { "Computer Science" },
            school = preferences.getString(SCHOOL_KEY, null).orEmpty()
                .ifBlank { "School of International Students" },
            overallAttendancePercent = preferences.getInt(OVERALL_ATTENDANCE_KEY, 96),
            classUpdateNotice = preferences.getString(CLASS_UPDATE_NOTICE_KEY, null)?.takeIf { it.isNotBlank() },
            classUpdateAtMillis = preferences.getLong(CLASS_UPDATE_AT_KEY, 0L),
        )
    }

    /** @return null on success, or a short user-visible error message */
    suspend fun signIn(context: Context, studentId: String, password: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.login(
                    LoginRequest(studentId = studentId.trim(), password = password.trim()),
                )
                AuthTokenStore.accessToken = response.token
                persistProfile(context, response.profile, loggedIn = true)
                FcmTokenRegistrar.registerCurrentToken(context)
                null
            } catch (e: HttpException) {
                val hint = httpErrorHint(e)
                Log.w(TAG, "login failed http ${e.code()}: $hint", e)
                hint
            } catch (_: UnknownHostException) {
                Log.e(TAG, "login failed: unknown host ${BuildConfig.API_HOST}:${BuildConfig.API_PORT}")
                "Cannot reach the server. On a real phone set cjlu.api.host to your computer's LAN IP (not 10.0.2.2)."
            } catch (_: SocketTimeoutException) {
                "Connection timed out. Is the backend running and is the firewall allowing port ${BuildConfig.API_PORT}?"
            } catch (e: Exception) {
                Log.e(TAG, "login failed", e)
                e.message?.takeIf { it.length <= 120 } ?: "Could not sign in. Check network and server settings."
            }
        }

    private fun httpErrorHint(e: HttpException): String {
        val body = try {
            e.response()?.errorBody()?.string()?.trim()?.take(280)
        } catch (_: Exception) {
            null
        }
        if (!body.isNullOrBlank()) return body
        return when (e.code()) {
            401 ->
                "Not authorized (401). Wrong password, or wrong student API key in local.properties vs server."
            403 -> "Forbidden (403)."
            else -> "Server error (HTTP ${e.code()})."
        }
    }

    private fun persistProfile(context: Context, profile: StudentProfileDto, loggedIn: Boolean) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(LOGGED_IN_KEY, loggedIn)
            putString(JWT_TOKEN_KEY, AuthTokenStore.accessToken)
            putString(STUDENT_ID_KEY, profile.studentId)
            putString(STUDENT_NAME_KEY, profile.displayName)
            putString(STUDY_YEAR_KEY, profile.classSection)
            putString(MAJOR_KEY, profile.major)
            putString(SCHOOL_KEY, profile.school)
            putInt(OVERALL_ATTENDANCE_KEY, profile.overallAttendancePercent)
            putString(CLASS_UPDATE_NOTICE_KEY, profile.classUpdateNotice.orEmpty())
            putLong(CLASS_UPDATE_AT_KEY, profile.classUpdateAtMillis)
        }
    }

    suspend fun changePassword(
        context: Context,
        currentPassword: String,
        newPassword: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                RetrofitClient.instance.changePassword(
                    ChangePasswordRequest(
                        currentPassword = currentPassword.trim(),
                        newPassword = newPassword.trim(),
                    ),
                )
                true
            } catch (e: HttpException) {
                Log.w(TAG, "changePassword http ${e.code()}", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "changePassword failed", e)
                false
            }
        }

    suspend fun refreshProfileFromServer(context: Context): AuthSession? =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val id = prefs.getString(STUDENT_ID_KEY, null)?.trim().orEmpty()
            if (id.isEmpty() || AuthTokenStore.accessToken.isNullOrBlank()) return@withContext null
            try {
                val profile = RetrofitClient.instance.getProfile(id)
                prefs.edit {
                    putString(STUDENT_NAME_KEY, profile.displayName)
                    putString(STUDY_YEAR_KEY, profile.classSection)
                    putString(MAJOR_KEY, profile.major)
                    putString(SCHOOL_KEY, profile.school)
                    putInt(OVERALL_ATTENDANCE_KEY, profile.overallAttendancePercent)
                    putString(CLASS_UPDATE_NOTICE_KEY, profile.classUpdateNotice.orEmpty())
                    putLong(CLASS_UPDATE_AT_KEY, profile.classUpdateAtMillis)
                }
                loadSession(context)
            } catch (e: Exception) {
                Log.e(TAG, "refreshProfile failed", e)
                null
            }
        }

    suspend fun updateProfileOnServer(context: Context, major: String, school: String): Boolean =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val id = prefs.getString(STUDENT_ID_KEY, null)?.trim().orEmpty()
            if (id.isEmpty() || AuthTokenStore.accessToken.isNullOrBlank()) return@withContext false
            try {
                val profile = RetrofitClient.instance.patchProfile(
                    id,
                    PatchProfileRequest(major = major.trim(), school = school.trim()),
                )
                prefs.edit {
                    putString(MAJOR_KEY, profile.major)
                    putString(SCHOOL_KEY, profile.school)
                    putInt(OVERALL_ATTENDANCE_KEY, profile.overallAttendancePercent)
                    putString(CLASS_UPDATE_NOTICE_KEY, profile.classUpdateNotice.orEmpty())
                    putLong(CLASS_UPDATE_AT_KEY, profile.classUpdateAtMillis)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "updateProfile failed", e)
                false
            }
        }

    fun signOut(context: Context) {
        AuthTokenStore.accessToken = null
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(LOGGED_IN_KEY, false)
            remove(JWT_TOKEN_KEY)
            remove(STUDENT_ID_KEY)
            remove(STUDENT_NAME_KEY)
            remove(STUDY_YEAR_KEY)
            remove(MAJOR_KEY)
            remove(SCHOOL_KEY)
            remove(OVERALL_ATTENDANCE_KEY)
            remove(CLASS_UPDATE_NOTICE_KEY)
            remove(CLASS_UPDATE_AT_KEY)
        }
    }
}

fun AuthSession.toStudentProfileDto(): StudentProfileDto =
    StudentProfileDto(
        studentId = studentId,
        displayName = studentName,
        classSection = studyYear,
        major = major,
        school = school,
        overallAttendancePercent = overallAttendancePercent,
        classUpdateNotice = classUpdateNotice,
        classUpdateAtMillis = classUpdateAtMillis,
    )
