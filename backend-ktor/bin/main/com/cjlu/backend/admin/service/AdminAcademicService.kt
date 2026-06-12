package com.cjlu.backend.admin.service

import com.cjlu.backend.AcademicRepository
import com.cjlu.backend.StudentProfileDto
import com.cjlu.backend.StudentTimetableDto
import com.cjlu.backend.websocket.WebSocketHub

object AdminAcademicService {

    sealed class AttendanceResult {
        data class Success(val profile: StudentProfileDto) : AttendanceResult()
        data object UnknownStudent : AttendanceResult()
        data object NoCourses : AttendanceResult()
        data object InvalidPercent : AttendanceResult()
    }

    sealed class TimetableResult {
        data class Success(val timetable: StudentTimetableDto) : TimetableResult()
        data object UnknownStudent : TimetableResult()
        data object InvalidSlots : TimetableResult()
    }

    sealed class CalendarResult {
        data object Success : CalendarResult()
        data object InvalidEvent : CalendarResult()
    }

    private val timePattern = Regex("""^\d{2}:\d{2}$""")

    suspend fun saveAttendance(
        studentId: String,
        courseCodes: List<String>,
        percents: List<String>,
        sessionsAttended: List<String>,
        sessionsTotal: List<String>,
    ): AttendanceResult {
        val sid = studentId.trim()
        if (sid.isEmpty()) return AttendanceResult.UnknownStudent
        val patches = courseCodes.mapIndexedNotNull { index, code ->
            val rawPercent = percents.getOrNull(index)?.trim().orEmpty()
            val attended = sessionsAttended.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()
            val total = sessionsTotal.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()
            val percent = when {
                attended != null && total != null -> {
                    val t = total.coerceAtLeast(1)
                    val a = attended.coerceIn(0, t)
                    ((a * 100L) / t).toInt().coerceIn(0, 100)
                }
                rawPercent.isNotEmpty() ->
                    rawPercent.toIntOrNull()?.coerceIn(0, 100) ?: return AttendanceResult.InvalidPercent
                else -> return@mapIndexedNotNull null
            }
            AcademicRepository.CourseAttendancePatch(
                courseCode = code.trim(),
                attendancePercent = percent,
                sessionsAttended = attended,
                sessionsTotal = total,
            )
        }
        if (patches.isEmpty()) return AttendanceResult.NoCourses
        val profile = AcademicRepository.patchCourseAttendanceBatch(sid, patches)
            ?: return AttendanceResult.UnknownStudent
        WebSocketHub.notifyAcademicUpdated(sid, "attendance")
        WebSocketHub.notifyLearningAlerts(profile)
        return AttendanceResult.Success(profile)
    }

    suspend fun saveTimetable(
        studentId: String,
        dayOfWeek: List<String>,
        dayLabels: List<String>,
        startTimes: List<String>,
        endTimes: List<String>,
        courseCodes: List<String>,
        roomNames: List<String>,
    ): TimetableResult {
        val sid = studentId.trim()
        if (sid.isEmpty()) return TimetableResult.UnknownStudent
        val slots = mutableListOf<AcademicRepository.TimetableSlotInput>()
        val count = maxOf(
            dayOfWeek.size,
            startTimes.size,
            endTimes.size,
            courseCodes.size,
            roomNames.size,
        )
        for (i in 0 until count) {
            val course = courseCodes.getOrNull(i)?.trim().orEmpty()
            val room = roomNames.getOrNull(i)?.trim().orEmpty()
            val start = startTimes.getOrNull(i)?.trim().orEmpty()
            val end = endTimes.getOrNull(i)?.trim().orEmpty()
            if (course.isEmpty() && room.isEmpty() && start.isEmpty() && end.isEmpty()) continue
            if (course.isEmpty() || room.isEmpty() || !timePattern.matches(start) || !timePattern.matches(end)) {
                return TimetableResult.InvalidSlots
            }
            val dow = dayOfWeek.getOrNull(i)?.trim()?.toIntOrNull()?.coerceIn(1, 7) ?: 1
            slots.add(
                AcademicRepository.TimetableSlotInput(
                    dayOfWeek = dow,
                    dayLabel = dayLabels.getOrNull(i)?.trim().orEmpty(),
                    startTime = start,
                    endTime = end,
                    courseCode = course,
                    roomName = room,
                ),
            )
        }
        val timetable = AcademicRepository.replaceTimetableSlots(sid, slots, preferChinese = false)
            ?: return TimetableResult.UnknownStudent
        WebSocketHub.notifyAcademicUpdated(sid, "timetable")
        return TimetableResult.Success(timetable)
    }

    suspend fun saveAcademicCalendar(
        dates: List<String>,
        titlesEn: List<String>,
        titlesZh: List<String>,
        detailsEn: List<String>,
        detailsZh: List<String>,
        tones: List<String>
    ): CalendarResult {
        val events = mutableListOf<com.cjlu.backend.AcademicDataSeed.AcademicCalendarEventSeed>()
        val count = maxOf(dates.size, titlesEn.size, detailsEn.size)
        for (i in 0 until count) {
            val date = dates.getOrNull(i)?.trim().orEmpty()
            val titleEn = titlesEn.getOrNull(i)?.trim().orEmpty()
            val titleZh = titlesZh.getOrNull(i)?.trim().orEmpty()
            val detailEn = detailsEn.getOrNull(i)?.trim().orEmpty()
            val detailZh = detailsZh.getOrNull(i)?.trim().orEmpty()
            val tone = tones.getOrNull(i)?.trim().orEmpty()

            if (date.isEmpty() && titleEn.isEmpty() && detailEn.isEmpty()) continue
            if (date.isEmpty() || titleEn.isEmpty()) return CalendarResult.InvalidEvent

            events.add(
                com.cjlu.backend.AcademicDataSeed.AcademicCalendarEventSeed(
                    date = date,
                    titleEn = titleEn,
                    titleZh = titleZh,
                    detailEn = detailEn,
                    detailZh = detailZh,
                    tone = tone.ifEmpty { "blue" }
                )
            )
        }

        val currentData = com.cjlu.backend.Database.getAcademicCalendarData()
        val newData = currentData.copy(events = events)
        com.cjlu.backend.Database.saveAcademicCalendarData(newData)
        
        // Broadcast calendar update to all students
        com.cjlu.backend.Database.listStudentSummaries().forEach { student ->
            WebSocketHub.notifyAcademicUpdated(student.studentId, "calendar")
        }

        return CalendarResult.Success
    }
}
