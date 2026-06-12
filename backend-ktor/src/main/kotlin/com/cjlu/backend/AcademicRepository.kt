package com.cjlu.backend

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.cjlu.contract.AcademicCalendarEventDto
import com.cjlu.contract.AcademicCalendarMonthDto
import com.cjlu.contract.StudentAcademicCalendarDto
import kotlin.random.Random

object AcademicRepository {

    private const val DEFAULT_SESSIONS_TOTAL = 30
    private const val DATA_SOURCE_API = "api"
    private const val DATA_SOURCE_LOCAL = "local"

    data class ClassCourseOption(
        val code: String,
        val nameEn: String,
        val nameZh: String,
    )

    data class TimetableSlotInput(
        val dayOfWeek: Int,
        val dayLabel: String,
        val startTime: String,
        val endTime: String,
        val courseCode: String,
        val roomName: String,
    )

    data class CourseAttendancePatch(
        val courseCode: String,
        val attendancePercent: Int,
        val sessionsAttended: Int?,
        val sessionsTotal: Int?,
    )

    private object ClassCourses : Table("class_courses") {
        val courseCode = varchar("course_code", 16)
        val nameEn = varchar("name_en", 200)
        val nameZh = varchar("name_zh", 200)
        val credits = integer("credits")
        val sortOrder = integer("sort_order")

        override val primaryKey = PrimaryKey(courseCode)
    }

    private object StudentCourseAttendance : Table("student_course_attendance") {
        val studentId = varchar("student_id", 20)
        val courseCode = varchar("course_code", 16)
        val attendancePercent = integer("attendance_percent")
        val sessionsAttended = integer("sessions_attended")
        val sessionsTotal = integer("sessions_total")

        override val primaryKey = PrimaryKey(studentId, courseCode)
    }

    private object StudentWeeklyAttendance : Table("student_weekly_attendance") {
        val studentId = varchar("student_id", 20)
        val weekIndex = integer("week_index")
        val weekLabel = varchar("week_label", 8)
        val percent = integer("percent")

        override val primaryKey = PrimaryKey(studentId, weekIndex)
    }

    private object StudentTranscriptGrades : Table("student_transcript_grades") {
        val studentId = varchar("student_id", 20)
        val courseCode = varchar("course_code", 16)
        val scorePercent = integer("score_percent")
        val gradePoint = double("grade_point")

        override val primaryKey = PrimaryKey(studentId, courseCode)
    }

    private object StudentTimetableSlots : Table("student_timetable_slots") {
        val studentId = varchar("student_id", 20)
        val slotIndex = integer("slot_index")
        val dayOfWeek = integer("day_of_week")
        val dayLabel = varchar("day_label", 8)
        val startTime = varchar("start_time", 8)
        val endTime = varchar("end_time", 8)
        val courseCode = varchar("course_code", 16)
        val roomName = varchar("room_name", 64)

        override val primaryKey = PrimaryKey(studentId, slotIndex)
    }

    private object StudentDormitory : Table("student_dormitory") {
        val studentId = varchar("student_id", 20)
        val buildingName = varchar("building_name", 120)
        val roomNumber = varchar("room_number", 16)
        val floor = integer("floor")
        val bedLabel = varchar("bed_label", 4)
        val hasActiveLeave = bool("has_active_leave")
        val leaveReason = text("leave_reason").nullable()
        val leaveFromDate = varchar("leave_from_date", 16).nullable()
        val leaveToDate = varchar("leave_to_date", 16).nullable()

        override val primaryKey = PrimaryKey(studentId)
    }

    fun ensureTables() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                ClassCourses,
                StudentCourseAttendance,
                StudentWeeklyAttendance,
                StudentTranscriptGrades,
                StudentTimetableSlots,
                StudentDormitory,
            )
            seedSharedCoursesIfEmpty()
        }
    }

    fun seedStudentIfMissing(studentId: String, preferChinese: Boolean) {
        val sid = studentId.trim()
        val hasAttendance = transaction {
            StudentCourseAttendance.selectAll().where { StudentCourseAttendance.studentId eq sid }.any()
        }
        if (hasAttendance) {
            backfillTimetableIfMissing(sid, preferChinese)
            return
        }
        transaction {
            generateAndInsertForStudent(sid, preferChinese)
        }
    }

    fun hasSeededAcademicData(studentId: String): Boolean {
        val sid = studentId.trim()
        return transaction {
            StudentCourseAttendance.selectAll().where { StudentCourseAttendance.studentId eq sid }.any()
        }
    }

    private fun backfillTimetableIfMissing(studentId: String, preferChinese: Boolean) {
        val missing = transaction {
            StudentTimetableSlots.selectAll().where { StudentTimetableSlots.studentId eq studentId }.empty()
        }
        if (!missing) return
    }

    private fun insertTimetableSlots(studentId: String, preferChinese: Boolean) {
        AcademicDataSeed.timetableTemplatesForStudent(studentId).forEachIndexed { index, template ->
            val course = AcademicDataSeed.sharedCourses[template.courseIndex % AcademicDataSeed.sharedCourses.size]
            StudentTimetableSlots.insert {
                it[StudentTimetableSlots.studentId] = studentId
                it[slotIndex] = index
                it[StudentTimetableSlots.dayOfWeek] = template.dayOfWeek
                it[dayLabel] = if (preferChinese) template.dayLabelZh else template.dayLabelEn
                it[startTime] = template.startTime
                it[endTime] = template.endTime
                it[courseCode] = course.code
                it[roomName] = AcademicDataSeed.timetableRoomName(template.roomSuffix)
            }
        }
    }

    fun seedAllRosterStudents() = Unit

    private fun seedSharedCoursesIfEmpty() {
        if (ClassCourses.selectAll().any()) return
        AcademicDataSeed.sharedCourses.forEachIndexed { index, course ->
            ClassCourses.insert {
                it[courseCode] = course.code
                it[nameEn] = course.nameEn
                it[nameZh] = course.nameZh
                it[credits] = course.credits
                it[sortOrder] = index
            }
        }
    }

    private fun generateAndInsertForStudent(studentId: String, preferChinese: Boolean): Int {
        val rng = Random(studentId.hashCode().toLong())
        val overall = 0
        val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

        for (course in AcademicDataSeed.sharedCourses) {
            StudentCourseAttendance.insert {
                it[StudentCourseAttendance.studentId] = studentId
                it[courseCode] = course.code
                it[attendancePercent] = 0
                it[sessionsAttended] = 0
                it[sessionsTotal] = DEFAULT_SESSIONS_TOTAL
            }
            val score = AcademicDataSeed.scorePercent(studentId, course.code)
            StudentTranscriptGrades.insert {
                it[StudentTranscriptGrades.studentId] = studentId
                it[StudentTranscriptGrades.courseCode] = course.code
                it[scorePercent] = score
                it[gradePoint] = AcademicDataSeed.gradePointFromScore(score)
            }
        }

        AcademicDataSeed.weeklyTrend(studentId, overall).forEachIndexed { index, (label, percent) ->
            StudentWeeklyAttendance.insert {
                it[StudentWeeklyAttendance.studentId] = studentId
                it[weekIndex] = index
                it[weekLabel] = label
                it[StudentWeeklyAttendance.percent] = percent
            }
        }

        val hasLeave = AcademicDataSeed.hasActiveLeave(studentId)
        val leaveFrom: String?
        val leaveTo: String?
        val leaveReason: String?
        if (hasLeave) {
            val start = LocalDate.now().minusDays(rng.nextLong(1, 5))
            val end = start.plusDays(rng.nextLong(1, 8))
            leaveFrom = start.format(dateFmt)
            leaveTo = end.format(dateFmt)
            leaveReason = if (preferChinese) {
                when (AcademicDataSeed.leaveReasonEn(studentId)) {
                    "Family visit" -> "探亲"
                    "Medical appointment" -> "就医"
                    "Personal leave" -> "事假"
                    else -> "周末外出"
                }
            } else {
                AcademicDataSeed.leaveReasonEn(studentId)
            }
        } else {
            leaveFrom = null
            leaveTo = null
            leaveReason = null
        }

        StudentDormitory.insert {
            it[StudentDormitory.studentId] = studentId
            it[buildingName] = AcademicDataSeed.DORM_BUILDING
            it[roomNumber] = AcademicDataSeed.roomNumberFor(studentId)
            it[StudentDormitory.floor] = AcademicDataSeed.floorFor(studentId)
            it[bedLabel] = AcademicDataSeed.bedLabelFor(studentId)
            it[StudentDormitory.hasActiveLeave] = hasLeave
            it[StudentDormitory.leaveReason] = leaveReason
            it[leaveFromDate] = leaveFrom
            it[leaveToDate] = leaveTo
        }

        insertTimetableSlots(studentId, preferChinese)

        return overall
    }

    /** Ensures every catalog course has an attendance row (neutral defaults for new rows). */
    fun ensureDefaultCourseAttendance(studentId: String) {
        val sid = studentId.trim()
        if (Database.getStudentProfile(sid) == null) return
        ensureTables()
        val added = transaction {
            val catalog = ClassCourses.selectAll()
                .orderBy(ClassCourses.sortOrder to SortOrder.ASC)
                .toList()
            val existing = StudentCourseAttendance.selectAll()
                .where { StudentCourseAttendance.studentId eq sid }
                .map { it[StudentCourseAttendance.courseCode] }
                .toSet()
            var count = 0
            for (row in catalog) {
                val code = row[ClassCourses.courseCode]
                if (code in existing) continue
                StudentCourseAttendance.insert {
                    it[StudentCourseAttendance.studentId] = sid
                    it[courseCode] = code
                    it[attendancePercent] = 0
                    it[sessionsAttended] = 0
                    it[sessionsTotal] = DEFAULT_SESSIONS_TOTAL
                }
                count++
            }
            count
        }
        if (added > 0) {
            recomputeOverallAndWeekly(sid)
        }
    }

    fun getAcademicSource(studentId: String): String =
        if (transaction { StudentCourseAttendance.selectAll().where { StudentCourseAttendance.studentId eq studentId.trim() }.any() }) {
            DATA_SOURCE_LOCAL
        } else {
            DATA_SOURCE_API
        }

    fun getAttendanceDetail(studentId: String, preferChinese: Boolean): StudentAttendanceDetailDto? {
        val sid = studentId.trim()
        seedStudentIfMissing(sid, preferChinese)
        ensureDefaultCourseAttendance(sid)
        return transaction {
            val profile = Database.getStudentProfile(sid) ?: return@transaction null
            val courses = StudentCourseAttendance.innerJoin(
                ClassCourses,
                { StudentCourseAttendance.courseCode },
                { ClassCourses.courseCode },
            )
                .selectAll()
                .where { StudentCourseAttendance.studentId eq sid }
                .orderBy(ClassCourses.sortOrder to SortOrder.ASC)
                .map { row ->
                    CourseAttendanceDto(
                        courseCode = row[StudentCourseAttendance.courseCode],
                        courseName = if (preferChinese) row[ClassCourses.nameZh] else row[ClassCourses.nameEn],
                        attendancePercent = row[StudentCourseAttendance.attendancePercent],
                        sessionsAttended = row[StudentCourseAttendance.sessionsAttended],
                        sessionsTotal = row[StudentCourseAttendance.sessionsTotal],
                    )
                }
            val weekly = StudentWeeklyAttendance.selectAll()
                .where { StudentWeeklyAttendance.studentId eq sid }
                .orderBy(StudentWeeklyAttendance.weekIndex to SortOrder.ASC)
                .map {
                    WeeklyAttendanceDto(
                        weekLabel = it[StudentWeeklyAttendance.weekLabel],
                        percent = it[StudentWeeklyAttendance.percent],
                    )
                }
            StudentAttendanceDetailDto(
                studentId = sid,
                classSection = profile.classSection,
                overallAttendancePercent = profile.overallAttendancePercent,
                courses = courses,
                weeklyTrend = weekly,
            )
        }
    }

    fun getTranscript(studentId: String, preferChinese: Boolean): StudentTranscriptDto? {
        val sid = studentId.trim()
        seedStudentIfMissing(sid, preferChinese)
        return transaction {
            val profile = Database.getStudentProfile(sid) ?: return@transaction null
            val courses = StudentTranscriptGrades.innerJoin(
                ClassCourses,
                { StudentTranscriptGrades.courseCode },
                { ClassCourses.courseCode },
            )
                .selectAll()
                .where { StudentTranscriptGrades.studentId eq sid }
                .orderBy(ClassCourses.sortOrder to SortOrder.ASC)
                .map { row ->
                    TranscriptCourseDto(
                        courseCode = row[StudentTranscriptGrades.courseCode],
                        courseName = if (preferChinese) row[ClassCourses.nameZh] else row[ClassCourses.nameEn],
                        credits = row[ClassCourses.credits],
                        scorePercent = row[StudentTranscriptGrades.scorePercent],
                        gradePoint = row[StudentTranscriptGrades.gradePoint],
                    )
                }
            val gpa = if (courses.isEmpty()) {
                0.0
            } else {
                val weighted = courses.sumOf { it.gradePoint * it.credits }
                val credits = courses.sumOf { it.credits }
                kotlin.math.round(weighted / credits * 100) / 100.0
            }
            StudentTranscriptDto(
                studentId = sid,
                classSection = profile.classSection,
                semesterLabel = AcademicDataSeed.CURRENT_SEMESTER,
                courses = courses,
                cumulativeGpa = gpa,
            )
        }
    }

    fun getTimetable(studentId: String, preferChinese: Boolean): StudentTimetableDto? {
        val sid = studentId.trim()
        seedStudentIfMissing(sid, preferChinese)
        return transaction {
            val profile = Database.getStudentProfile(sid) ?: return@transaction null
            val slots = StudentTimetableSlots.innerJoin(
                ClassCourses,
                { StudentTimetableSlots.courseCode },
                { ClassCourses.courseCode },
            )
                .selectAll()
                .where { StudentTimetableSlots.studentId eq sid }
                .orderBy(StudentTimetableSlots.slotIndex to SortOrder.ASC)
                .map { row ->
                    TimetableSlotDto(
                        dayOfWeek = row[StudentTimetableSlots.dayOfWeek],
                        dayLabel = row[StudentTimetableSlots.dayLabel],
                        startTime = row[StudentTimetableSlots.startTime],
                        endTime = row[StudentTimetableSlots.endTime],
                        courseCode = row[StudentTimetableSlots.courseCode],
                        courseName = if (preferChinese) row[ClassCourses.nameZh] else row[ClassCourses.nameEn],
                        roomName = row[StudentTimetableSlots.roomName],
                    )
                }
            StudentTimetableDto(
                studentId = sid,
                classSection = profile.classSection,
                semesterLabel = AcademicDataSeed.CURRENT_SEMESTER,
                slots = slots,
            )
        }
    }

    fun getDormitory(studentId: String): StudentDormitoryDto? {
        val sid = studentId.trim()
        seedStudentIfMissing(sid, preferChinese = false)
        return transaction {
            StudentDormitory.selectAll().where { StudentDormitory.studentId eq sid }.singleOrNull()?.let { row ->
                StudentDormitoryDto(
                    studentId = sid,
                    buildingName = row[StudentDormitory.buildingName],
                    roomNumber = row[StudentDormitory.roomNumber],
                    floor = row[StudentDormitory.floor],
                    bedLabel = row[StudentDormitory.bedLabel],
                    hasActiveLeave = row[StudentDormitory.hasActiveLeave],
                    leaveReason = row[StudentDormitory.leaveReason],
                    leaveFromDate = row[StudentDormitory.leaveFromDate],
                    leaveToDate = row[StudentDormitory.leaveToDate],
                )
            }
        }
    }

    fun updateDormitoryLeave(
        studentId: String,
        hasLeave: Boolean,
        reason: String?,
        fromDate: String? = null,
        toDate: String? = null
    ): Boolean = transaction {
        val sid = studentId.trim()
        val existing = StudentDormitory.selectAll().where { StudentDormitory.studentId eq sid }.any()
        if (!existing) return@transaction false
        StudentDormitory.update({ StudentDormitory.studentId eq sid }) {
            it[hasActiveLeave] = hasLeave
            it[leaveReason] = reason
            it[leaveFromDate] = fromDate
            it[leaveToDate] = toDate
        }
        true
    }

    fun getAcademicCalendar(preferChinese: Boolean): StudentAcademicCalendarDto {
        val data = Database.getAcademicCalendarData()
        return StudentAcademicCalendarDto(
            academicYearLabel = data.academicYearLabel,
            semesterLabel = data.semesterLabel,
            months = data.months.map { month ->
                AcademicCalendarMonthDto(
                    monthLabel = month.monthLabel,
                    weekHeaders = month.weekHeaders,
                    rows = month.rows,
                )
            },
            events = data.events.map { event ->
                AcademicCalendarEventDto(
                    date = event.date,
                    title = if (preferChinese) event.titleZh else event.titleEn,
                    detail = if (preferChinese) event.detailZh else event.detailEn,
                    tone = event.tone,
                )
            },
        )
    }

    fun listClassCourses(): List<ClassCourseOption> = transaction {
        ClassCourses.selectAll()
            .orderBy(ClassCourses.sortOrder to SortOrder.ASC)
            .map { row ->
                ClassCourseOption(
                    code = row[ClassCourses.courseCode],
                    nameEn = row[ClassCourses.nameEn],
                    nameZh = row[ClassCourses.nameZh],
                )
            }
    }

    fun findCourseCodeByName(courseName: String): String? = transaction {
        val name = courseName.trim()
        if (name.isEmpty()) return@transaction null
        ClassCourses.selectAll()
            .where { (ClassCourses.nameEn eq name) or (ClassCourses.nameZh eq name) }
            .map { it[ClassCourses.courseCode] }
            .firstOrNull()
    }

    fun patchCourseAttendanceBatch(
        studentId: String,
        patches: List<CourseAttendancePatch>,
    ): StudentProfileDto? {
        val sid = studentId.trim()
        if (Database.getStudentProfile(sid) == null) return null
        seedStudentIfMissing(sid, preferChinese = false)
        transaction {
            for (patch in patches) {
                val code = patch.courseCode.trim()
                if (code.isEmpty()) continue
                val fromSessionInput =
                    patch.sessionsAttended != null && patch.sessionsTotal != null
                val existing = StudentCourseAttendance.selectAll()
                    .where {
                        (StudentCourseAttendance.studentId eq sid) and
                            (StudentCourseAttendance.courseCode eq code)
                    }
                    .singleOrNull()
                val (attended, total) = when {
                    fromSessionInput -> {
                        val t = patch.sessionsTotal!!.coerceAtLeast(1)
                        val a = patch.sessionsAttended!!.coerceIn(0, t)
                        a to t
                    }
                    existing != null -> {
                        existing[StudentCourseAttendance.sessionsAttended] to
                            existing[StudentCourseAttendance.sessionsTotal]
                    }
                    else -> {
                        val percent = patch.attendancePercent.coerceIn(0, 100)
                        val rng = Random("$sid|$code".hashCode().toLong())
                        AcademicDataSeed.sessionsForAttendance(percent, rng)
                    }
                }
                val percent = when {
                    fromSessionInput && total > 0 ->
                        ((attended.toLong() * 100L) / total).toInt().coerceIn(0, 100)
                    else -> patch.attendancePercent.coerceIn(0, 100)
                }
                if (existing != null) {
                    StudentCourseAttendance.update({
                        (StudentCourseAttendance.studentId eq sid) and
                            (StudentCourseAttendance.courseCode eq code)
                    }) {
                        it[StudentCourseAttendance.attendancePercent] = percent
                        it[StudentCourseAttendance.sessionsAttended] = attended
                        it[StudentCourseAttendance.sessionsTotal] = total
                    }
                } else if (ClassCourses.selectAll().where { ClassCourses.courseCode eq code }.any()) {
                    StudentCourseAttendance.insert {
                        it[StudentCourseAttendance.studentId] = sid
                        it[StudentCourseAttendance.courseCode] = code
                        it[StudentCourseAttendance.attendancePercent] = percent
                        it[StudentCourseAttendance.sessionsAttended] = attended
                        it[StudentCourseAttendance.sessionsTotal] = total
                    }
                }
            }
        }
        return recomputeOverallAndWeekly(sid)
    }

    fun recomputeOverallAndWeekly(studentId: String): StudentProfileDto? {
        val sid = studentId.trim()
        val overall = transaction {
            val rows = StudentCourseAttendance.selectAll()
                .where { StudentCourseAttendance.studentId eq sid }
                .toList()
            if (rows.isEmpty()) {
                Database.getStudentProfile(sid)?.overallAttendancePercent ?: 96
            } else {
                val totalSessions = rows.sumOf { it[StudentCourseAttendance.sessionsTotal] }
                if (totalSessions <= 0) {
                    rows.map { it[StudentCourseAttendance.attendancePercent] }.average().toInt()
                } else {
                    val weighted = rows.sumOf {
                        it[StudentCourseAttendance.attendancePercent] *
                            it[StudentCourseAttendance.sessionsTotal]
                    }
                    (weighted.toDouble() / totalSessions).toInt().coerceIn(0, 100)
                }
            }
        }
        transaction {
            StudentWeeklyAttendance.deleteWhere { StudentWeeklyAttendance.studentId eq sid }
            AcademicDataSeed.weeklyTrend(sid, overall).forEachIndexed { index, (label, percent) ->
                StudentWeeklyAttendance.insert {
                    it[StudentWeeklyAttendance.studentId] = sid
                    it[weekIndex] = index
                    it[weekLabel] = label
                    it[StudentWeeklyAttendance.percent] = percent
                }
            }
        }
        return Database.patchStudentLearningAlerts(sid, overall, null)
    }

    fun replaceTimetableSlots(
        studentId: String,
        slots: List<TimetableSlotInput>,
        preferChinese: Boolean = false,
    ): StudentTimetableDto? {
        val sid = studentId.trim()
        if (Database.getStudentProfile(sid) == null) return null
        seedStudentIfMissing(sid, preferChinese)
        transaction {
            StudentTimetableSlots.deleteWhere { StudentTimetableSlots.studentId eq sid }
            slots.forEachIndexed { index, slot ->
                StudentTimetableSlots.insert {
                    it[StudentTimetableSlots.studentId] = sid
                    it[slotIndex] = index
                    it[dayOfWeek] = slot.dayOfWeek.coerceIn(1, 7)
                    it[dayLabel] = slot.dayLabel.ifBlank {
                        dayLabelFor(slot.dayOfWeek, preferChinese)
                    }
                    it[startTime] = slot.startTime
                    it[endTime] = slot.endTime
                    it[courseCode] = slot.courseCode
                    it[roomName] = slot.roomName
                }
            }
        }
        return getTimetable(sid, preferChinese)
    }

    private fun dayLabelFor(dayOfWeek: Int, preferChinese: Boolean): String {
        val en = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val zh = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val idx = dayOfWeek.coerceIn(1, 7)
        return if (preferChinese) zh[idx] else en[idx]
    }
}
