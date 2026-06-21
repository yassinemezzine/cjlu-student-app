package com.cjlu.studentapp.data

import android.content.Context
import android.util.Log
import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.network.api.StudentAcademicCalendarDto
import com.cjlu.studentapp.network.api.StudentAttendanceDetailDto
import com.cjlu.studentapp.network.api.StudentDormitoryDto
import com.cjlu.studentapp.network.api.StudentTimetableDto
import com.cjlu.studentapp.network.api.StudentTranscriptDto
import kotlinx.serialization.serializer
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object AcademicRepository {

    private const val TAG = "AcademicRepository"

    suspend fun loadAttendance(
        context: Context,
        studentId: String,
    ): StudentAttendanceDetailDto = loadCached(
        context = context,
        studentId = studentId,
        cacheKey = AcademicCacheKeys.ATTENDANCE,
        fetch = { RetrofitClient.instance.getAttendanceDetail(studentId) },
    )

    suspend fun loadTranscript(
        context: Context,
        studentId: String,
    ): StudentTranscriptDto = loadCached(
        context = context,
        studentId = studentId,
        cacheKey = AcademicCacheKeys.TRANSCRIPT,
        fetch = { RetrofitClient.instance.getTranscript(studentId) },
    )

    suspend fun loadAcademicCalendar(
        context: Context,
        studentId: String,
    ): StudentAcademicCalendarDto = loadCached(
        context = context,
        studentId = studentId,
        cacheKey = AcademicCacheKeys.CALENDAR,
        fetch = { RetrofitClient.instance.getAcademicCalendar(studentId) },
    )

    suspend fun loadDormitory(
        context: Context,
        studentId: String,
    ): StudentDormitoryDto = loadCached(
        context = context,
        studentId = studentId,
        cacheKey = AcademicCacheKeys.DORMITORY,
        fetch = { RetrofitClient.instance.getDormitory(studentId) },
    )

    suspend fun loadTimetable(
        context: Context,
        studentId: String,
    ): StudentTimetableDto = loadCached(
        context = context,
        studentId = studentId,
        cacheKey = AcademicCacheKeys.TIMETABLE,
        fetch = { RetrofitClient.instance.getTimetable(studentId) },
    )

    suspend fun clearCacheForStudent(context: Context, studentId: String) {
        AppDatabase.getDatabase(context).academicCacheDao().deleteForStudent(studentId.trim())
    }

    suspend fun invalidate(context: Context, studentId: String, keys: Set<String>) {
        val dao = AppDatabase.getDatabase(context).academicCacheDao()
        val sid = studentId.trim()
        keys.forEach { dao.deleteKey(sid, it) }
    }

    suspend fun invalidateAttendanceAndTimetable(context: Context, studentId: String) {
        invalidate(
            context,
            studentId,
            setOf(AcademicCacheKeys.ATTENDANCE, AcademicCacheKeys.TIMETABLE),
        )
    }

    suspend fun invalidateAll(context: Context, studentId: String) {
        invalidate(
            context,
            studentId,
            setOf(
                AcademicCacheKeys.ATTENDANCE,
                AcademicCacheKeys.TIMETABLE,
                AcademicCacheKeys.TRANSCRIPT,
                AcademicCacheKeys.DORMITORY,
                AcademicCacheKeys.CALENDAR,
            ),
        )
    }

    private suspend inline fun <reified T> loadCached(
        context: Context,
        studentId: String,
        cacheKey: String,
        fetch: () -> T,
    ): T {
        val sid = studentId.trim()
        val dao = AppDatabase.getDatabase(context).academicCacheDao()
        return try {
            val fresh = fetch()
            dao.put(
                AcademicCacheEntry(
                    studentId = sid,
                    cacheKey = cacheKey,
                    payloadJson = academicJson.encodeToString(serializer<T>(), fresh),
                    fetchedAtMillis = System.currentTimeMillis(),
                    sourceVersion = "api-v1",
                ),
            )
            fresh
        } catch (e: Exception) {
            val offline = e is UnknownHostException || e is ConnectException || e is SocketTimeoutException || e is HttpException
            Log.w(TAG, "Failed to load $cacheKey from API, using cache if available", e)
            val cached = dao.get(sid, cacheKey)
            if (cached != null) {
                val decoded = academicJson.decodeFromString(serializer<T>(), cached.payloadJson)
                if (offline && cached.sourceVersion.startsWith("seeded")) {
                    Log.i(TAG, "Using seeded academic cache for $cacheKey while backend is unavailable")
                }
                return decoded
            }
            throw e
        }
    }
}
