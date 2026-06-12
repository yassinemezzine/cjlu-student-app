package com.cjlu.backend

import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Shared class catalog and deterministic per-student academic mock data.
 * Class section comes from each student's roster profile; attendance and grades vary by student.
 */
object AcademicDataSeed {

    const val DORM_BUILDING = "International Dormitory Building 13"
    const val CURRENT_SEMESTER = "2025–2026 Spring"
    const val CURRENT_ACADEMIC_YEAR = "2025–2026"

    data class SharedCourse(
        val code: String,
        val nameEn: String,
        val nameZh: String,
        val credits: Int,
    )

    val sharedCourses: List<SharedCourse> = listOf(
        SharedCourse("CS301", "Data Structures", "数据结构", 4),
        SharedCourse("CS302", "Operating Systems", "操作系统", 3),
        SharedCourse("CS303", "Database Systems", "数据库系统", 3),
        SharedCourse("CS304", "Computer Networks", "计算机网络", 3),
        SharedCourse("MATH201", "Discrete Mathematics", "离散数学", 4),
        SharedCourse("ENG205", "Academic English Writing", "学术英语写作", 2),
        SharedCourse("CS305", "Software Engineering", "软件工程", 3),
    )

    private fun studentRandom(studentId: String): Random =
        Random(studentId.trim().hashCode().toLong())

    fun roomNumberFor(studentId: String): String {
        val r = studentRandom(studentId)
        val floor = r.nextInt(3, 13)
        val room = r.nextInt(1, 30)
        return "${floor}${room.toString().padStart(2, '0')}"
    }

    fun floorFor(studentId: String): Int =
        roomNumberFor(studentId).take(2).toIntOrNull() ?: studentRandom(studentId).nextInt(3, 13)

    fun bedLabelFor(studentId: String): String {
        val beds = listOf("A", "B", "C", "D")
        return beds[studentRandom(studentId).nextInt(beds.size)]
    }

    fun courseAttendancePercent(studentId: String, courseCode: String): Int {
        val r = studentRandom("$studentId|$courseCode")
        return r.nextInt(68, 100)
    }

    fun sessionsForAttendance(percent: Int, rng: Random): Pair<Int, Int> {
        val total = rng.nextInt(28, 36)
        val attended = (total * percent / 100.0).roundToInt().coerceIn(0, total)
        return attended to total
    }

    fun weeklyTrend(studentId: String, overall: Int): List<Pair<String, Int>> {
        val r = studentRandom("$studentId|weeks")
        val labels = listOf("W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8")
        var value = (overall + r.nextInt(-8, 8)).coerceIn(60, 100)
        return labels.map { label ->
            value = (value + r.nextInt(-6, 7)).coerceIn(55, 100)
            label to value
        }
    }

    fun scorePercent(studentId: String, courseCode: String): Int {
        val r = studentRandom("$studentId|score|$courseCode")
        return r.nextInt(62, 99)
    }

    fun gradePointFromScore(score: Int): Double = when {
        score >= 93 -> 4.0
        score >= 90 -> 3.7
        score >= 87 -> 3.3
        score >= 83 -> 3.0
        score >= 80 -> 2.7
        score >= 77 -> 2.3
        score >= 73 -> 2.0
        score >= 70 -> 1.7
        score >= 67 -> 1.3
        score >= 60 -> 1.0
        else -> 0.0
    }

    fun cumulativeGpa(studentId: String): Double {
        val points = sharedCourses.map { gradePointFromScore(scorePercent(studentId, it.code)) * it.credits }
        val credits = sharedCourses.sumOf { it.credits }
        val gpa = points.sum() / credits
        return (gpa * 100).roundToInt() / 100.0
    }

    fun overallAttendance(studentId: String): Int {
        val percents = sharedCourses.map { courseAttendancePercent(studentId, it.code) }
        return percents.average().roundToInt()
    }

    fun hasActiveLeave(studentId: String): Boolean =
        studentRandom("$studentId|leave").nextInt(100) < 28

    fun leaveReasonEn(studentId: String): String {
        val reasons = listOf(
            "Family visit",
            "Medical appointment",
            "Personal leave",
            "Weekend travel",
        )
        return reasons[studentRandom("$studentId|reason").nextInt(reasons.size)]
    }

    data class TimetableSlotTemplate(
        val dayOfWeek: Int,
        val dayLabelEn: String,
        val dayLabelZh: String,
        val startTime: String,
        val endTime: String,
        val courseIndex: Int,
        val roomSuffix: String,
    )

    private val timetableTemplates: List<TimetableSlotTemplate> = listOf(
        TimetableSlotTemplate(1, "Mon", "周一", "08:30", "10:10", 0, "201"),
        TimetableSlotTemplate(1, "Mon", "周一", "10:30", "12:10", 1, "305"),
        TimetableSlotTemplate(2, "Tue", "周二", "14:00", "15:40", 2, "201"),
        TimetableSlotTemplate(3, "Wed", "周三", "08:30", "10:10", 3, "412"),
        TimetableSlotTemplate(3, "Wed", "周三", "10:30", "12:10", 4, "305"),
        TimetableSlotTemplate(4, "Thu", "周四", "14:00", "15:40", 5, "201"),
        TimetableSlotTemplate(5, "Fri", "周五", "08:30", "10:10", 6, "412"),
    )

    fun timetableTemplatesForStudent(studentId: String): List<TimetableSlotTemplate> {
        val r = studentRandom("$studentId|timetable")
        return if (r.nextBoolean()) timetableTemplates else timetableTemplates.drop(1)
    }

    fun timetableRoomName(suffix: String): String = "Building 13 — Room $suffix"

    @Serializable
    data class AcademicCalendarMonthSeed(
        val monthLabel: String,
        val weekHeaders: List<String>,
        val rows: List<List<String>>,
    )

    @Serializable
    data class AcademicCalendarEventSeed(
        val date: String,
        val titleEn: String,
        val titleZh: String,
        val detailEn: String,
        val detailZh: String,
        val tone: String,
    )

    val academicCalendarMonths: List<AcademicCalendarMonthSeed> = listOf(
        AcademicCalendarMonthSeed(
            monthLabel = "2025-09",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7"),
                listOf("8", "9", "10", "11", "12", "13", "14"),
                listOf("15", "16", "17", "18", "19", "20", "21"),
                listOf("22", "23", "24", "25", "26", "27", "28"),
                listOf("29", "30", "", "", "", "", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2025-10",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "1", "2", "3", "4", "5"),
                listOf("6", "7", "8", "9", "10", "11", "12"),
                listOf("13", "14", "15", "16", "17", "18", "19"),
                listOf("20", "21", "22", "23", "24", "25", "26"),
                listOf("27", "28", "29", "30", "31", "", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2025-11",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "1", "2"),
                listOf("3", "4", "5", "6", "7", "8", "9"),
                listOf("10", "11", "12", "13", "14", "15", "16"),
                listOf("17", "18", "19", "20", "21", "22", "23"),
                listOf("24", "25", "26", "27", "28", "29", "30")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2025-12",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7"),
                listOf("8", "9", "10", "11", "12", "13", "14"),
                listOf("15", "16", "17", "18", "19", "20", "21"),
                listOf("22", "23", "24", "25", "26", "27", "28"),
                listOf("29", "30", "31", "", "", "", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-01",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "1", "2", "3", "4"),
                listOf("5", "6", "7", "8", "9", "10", "11"),
                listOf("12", "13", "14", "15", "16", "17", "18"),
                listOf("19", "20", "21", "22", "23", "24", "25"),
                listOf("26", "27", "28", "29", "30", "31", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-02",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "", "1"),
                listOf("2", "3", "4", "5", "6", "7", "8"),
                listOf("9", "10", "11", "12", "13", "14", "15"),
                listOf("16", "17", "18", "19", "20", "21", "22"),
                listOf("23", "24", "25", "26", "27", "28", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-03",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "", "1"),
                listOf("2", "3", "4", "5", "6", "7", "8"),
                listOf("9", "10", "11", "12", "13", "14", "15"),
                listOf("16", "17", "18", "19", "20", "21", "22"),
                listOf("23", "24", "25", "26", "27", "28", "29"),
                listOf("30", "31", "", "", "", "", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-04",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "1", "2", "3", "4", "5"),
                listOf("6", "7", "8", "9", "10", "11", "12"),
                listOf("13", "14", "15", "16", "17", "18", "19"),
                listOf("20", "21", "22", "23", "24", "25", "26"),
                listOf("27", "28", "29", "30", "", "", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-05",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "1", "2", "3"),
                listOf("4", "5", "6", "7", "8", "9", "10"),
                listOf("11", "12", "13", "14", "15", "16", "17"),
                listOf("18", "19", "20", "21", "22", "23", "24"),
                listOf("25", "26", "27", "28", "29", "30", "31")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-06",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7"),
                listOf("8", "9", "10", "11", "12", "13", "14"),
                listOf("15", "16", "17", "18", "19", "20", "21"),
                listOf("22", "23", "24", "25", "26", "27", "28"),
                listOf("29", "30", "", "", "", "", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-07",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "1", "2", "3", "4", "5"),
                listOf("6", "7", "8", "9", "10", "11", "12"),
                listOf("13", "14", "15", "16", "17", "18", "19"),
                listOf("20", "21", "22", "23", "24", "25", "26"),
                listOf("27", "28", "29", "30", "31", "", "")
            )
        ),
        AcademicCalendarMonthSeed(
            monthLabel = "2026-08",
            weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S"),
            rows = listOf(
                listOf("", "", "", "", "", "1", "2"),
                listOf("3", "4", "5", "6", "7", "8", "9"),
                listOf("10", "11", "12", "13", "14", "15", "16"),
                listOf("17", "18", "19", "20", "21", "22", "23"),
                listOf("24", "25", "26", "27", "28", "29", "30"),
                listOf("31", "", "", "", "", "", "")
            )
        )
    )

    val academicCalendarEvents: List<AcademicCalendarEventSeed> = listOf(
        AcademicCalendarEventSeed("2026-01-05", "Winter vacation begins", "寒假开始", "Campus services switch to holiday mode.", "校园服务切换为假期模式。", "blue"),
        AcademicCalendarEventSeed("2026-02-15", "Register", "注册报到", "Students complete semester registration.", "学生完成学期注册。", "purple"),
        AcademicCalendarEventSeed("2026-02-23", "Class startup", "开学上课", "Spring classes begin.", "春季学期开始上课。", "green"),
        AcademicCalendarEventSeed("2026-04-05", "Holiday", "节假日", "Ching Ming Festival break.", "清明节假期。", "amber"),
        AcademicCalendarEventSeed("2026-06-20", "Final exams", "期末考试", "End-of-term examinations start.", "学期末考试开始。", "red"),
        AcademicCalendarEventSeed("2026-07-10", "Summer vacation begins", "暑假开始", "Term closes and campus services pause.", "学期结束，校园服务暂停。", "teal"),
    )
}
