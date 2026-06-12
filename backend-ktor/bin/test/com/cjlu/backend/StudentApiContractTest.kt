package com.cjlu.backend

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import com.cjlu.backend.admin.service.RequestSubmissionService

/**
 * Verifies student API responses deserialize into the same DTOs used by the Android app
 * ([com.cjlu.studentapp.network.api.ApiModels] mirrors [Models.kt]).
 */
class StudentApiContractTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private lateinit var apiKey: String
    private lateinit var bearerToken: String

    private val studentId = "20230901"

    @BeforeTest
    fun setUp() {
        apiKey = Database.getStudentApiKey()
        bearerToken = loginToken()
    }

    private fun loginToken(): String {
        var token = ""
        testApplication {
            application { module() }
            val response = client.post("/auth/login") {
                header("X-API-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody("""{"studentId":"$studentId","password":"$studentId"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            token = json.decodeFromString<LoginResponse>(response.bodyAsText()).token
        }
        return token
    }

    private fun authHeaders(builder: io.ktor.client.request.HttpRequestBuilder, bearer: String = bearerToken) {
        builder.header("X-API-Key", apiKey)
        builder.header(HttpHeaders.Authorization, "Bearer $bearer")
        builder.header("Accept-Language", "en-US,en;q=0.9")
    }

    @Test
    fun loginResponse_matchesAppDto() = testApplication {
        application { module() }
        val response = client.post("/auth/login") {
            header("X-API-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody("""{"studentId":"$studentId","password":"$studentId"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<LoginResponse>(response.bodyAsText())
        assertEquals(studentId, body.profile.studentId)
        assertTrue(body.token.isNotBlank())
    }

    @Test
    fun servicesResponse_matchesAppDto() = testApplication {
        application { module() }
        val response = client.get("/services") {
            header("X-API-Key", apiKey)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val list = json.decodeFromString(ListSerializer(CatalogServiceDto.serializer()), response.bodyAsText())
        assertTrue(list.isNotEmpty())
        assertNotNull(list.first().id)
    }

    @Test
    fun profileResponse_matchesAppDto() = testApplication {
        application { module() }
        val response = client.get("/students/$studentId/profile") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, response.status)
        val profile = json.decodeFromString<StudentProfileDto>(response.bodyAsText())
        assertEquals(studentId, profile.studentId)
    }

    @Test
    fun academicEndpoints_matchAppDtos() = testApplication {
        application { module() }
        val attendance = client.get("/students/$studentId/academic/attendance") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, attendance.status)
        json.decodeFromString<StudentAttendanceDetailDto>(attendance.bodyAsText())

        val transcript = client.get("/students/$studentId/academic/transcript") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, transcript.status)
        json.decodeFromString<StudentTranscriptDto>(transcript.bodyAsText())

        val timetable = client.get("/students/$studentId/academic/timetable") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, timetable.status)
        json.decodeFromString<StudentTimetableDto>(timetable.bodyAsText())

        val dorm = client.get("/students/$studentId/dormitory") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, dorm.status)
        json.decodeFromString<StudentDormitoryDto>(dorm.bodyAsText())
    }

    @Test
    fun messagesAndRequests_matchAppDtos() = testApplication {
        application { module() }
        val messages = client.get("/students/$studentId/messages") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, messages.status)
        json.decodeFromString(ListSerializer(MessageDto.serializer()), messages.bodyAsText())

        val requests = client.get("/students/$studentId/requests") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, requests.status)
        json.decodeFromString(ListSerializer(StudentRequest.serializer()), requests.bodyAsText())
    }

    @Test
    fun targetedInboxMessage_visibleOnlyToRecipient() = testApplication {
        application { module() }
        val recipientId = "20230928"
        val messageId = Database.insertAdminInboxMessage(
            recipientStudentId = recipientId,
            category = "Academic",
            senderEn = "Test",
            senderZh = "测试",
            titleEn = "Private admin note",
            titleZh = "私信",
            bodyEn = "Only one student should see this.",
            bodyZh = "仅一人可见。",
            relatedServiceId = null,
            requiresAction = false,
        )
        val outsiderList = json.decodeFromString(
            ListSerializer(MessageDto.serializer()),
            client.get("/students/$studentId/messages") { authHeaders(this) }.bodyAsText(),
        )
        assertTrue(outsiderList.none { it.id == messageId })

        val loginOther = client.post("/auth/login") {
            header("X-API-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody("""{"studentId":"$recipientId","password":"$recipientId"}""")
        }
        assertEquals(HttpStatusCode.OK, loginOther.status)
        val otherToken = json.decodeFromString<LoginResponse>(loginOther.bodyAsText()).token

        val insiderList = json.decodeFromString(
            ListSerializer(MessageDto.serializer()),
            client.get("/students/$recipientId/messages") { authHeaders(this, otherToken) }.bodyAsText(),
        )
        assertTrue(insiderList.any { it.id == messageId })
    }

    @Test
    fun submitRequest_matchesAppDto() = testApplication {
        application { module() }
        val response = client.post("/requests") {
            authHeaders(this)
            contentType(ContentType.Application.Json)
            setBody(
                """{"serviceId":"visa_extension","studentId":"$studentId","contactInfo":"test","notes":"contract"}""",
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val request = json.decodeFromString<StudentRequest>(response.bodyAsText())
        assertEquals(studentId, request.studentId)
        assertEquals(RequestStatus.Submitted, request.status)
    }

    @Test
    fun changePassword_returnsOkResponse() = testApplication {
        application { module() }
        val response = client.post("/auth/change-password") {
            authHeaders(this)
            contentType(ContentType.Application.Json)
            setBody("""{"currentPassword":"$studentId","newPassword":"$studentId"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        json.decodeFromString<OkResponse>(response.bodyAsText())
    }

    @Test
    fun adminAcademicPatch_surfacesOnStudentGetEndpoints() = testApplication {
        application { module() }
        val patched = AcademicRepository.patchCourseAttendanceBatch(
            studentId = studentId,
            patches = listOf(
                AcademicRepository.CourseAttendancePatch(
                    courseCode = "CS301",
                    attendancePercent = 55,
                    sessionsAttended = 20,
                    sessionsTotal = 36,
                ),
            ),
        )
        assertNotNull(patched)

        val attendanceResponse = client.get("/students/$studentId/academic/attendance") {
            authHeaders(this)
        }
        assertEquals(HttpStatusCode.OK, attendanceResponse.status)
        val attendance = json.decodeFromString<StudentAttendanceDetailDto>(attendanceResponse.bodyAsText())
        val cs301 = attendance.courses.first { it.courseCode == "CS301" }
        assertEquals(55, cs301.attendancePercent)
        assertEquals(20, cs301.sessionsAttended)
        assertEquals(36, cs301.sessionsTotal)
        assertEquals(patched!!.overallAttendancePercent, attendance.overallAttendancePercent)

        val timetableBefore = json.decodeFromString<StudentTimetableDto>(
            client.get("/students/$studentId/academic/timetable") { authHeaders(this) }.bodyAsText(),
        )
        val firstSlot = timetableBefore.slots.first()
        val updatedTimetable = AcademicRepository.replaceTimetableSlots(
            studentId = studentId,
            slots = listOf(
                AcademicRepository.TimetableSlotInput(
                    dayOfWeek = firstSlot.dayOfWeek,
                    dayLabel = firstSlot.dayLabel,
                    startTime = firstSlot.startTime,
                    endTime = firstSlot.endTime,
                    courseCode = firstSlot.courseCode,
                    roomName = "Admin Test Room 999",
                ),
            ),
        )
        assertNotNull(updatedTimetable)

        val timetableResponse = client.get("/students/$studentId/academic/timetable") {
            authHeaders(this)
        }
        assertEquals(HttpStatusCode.OK, timetableResponse.status)
        val timetable = json.decodeFromString<StudentTimetableDto>(timetableResponse.bodyAsText())
        assertEquals("Admin Test Room 999", timetable.slots.first().roomName)
    }

    @Test
    fun fcmToken_registerAndList() = testApplication {
        application { module() }
        val response = client.post("/students/$studentId/fcm-token") {
            authHeaders(this)
            contentType(ContentType.Application.Json)
            setBody("""{"token":"test-fcm-token-abc"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(Database.getFcmTokensForStudent(studentId).contains("test-fcm-token-abc"))
    }

    @Test
    fun patchProfile_matchesAppDto() = testApplication {
        application { module() }
        val response = client.patch("/students/$studentId/profile") {
            authHeaders(this)
            contentType(ContentType.Application.Json)
            setBody("""{"major":"Computer Science","school":"CJLU"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        json.decodeFromString<StudentProfileDto>(response.bodyAsText())
    }

    @Test
    fun leaveRequestApproval_updatesDormitoryStatus() = testApplication {
        application { module() }
        val notes = "Illness / symptoms: High Fever\nSick leave starts: 2026-06-10\nSick leave ends: 2026-06-15"
        val created = RequestSubmissionService.createRequest(
            studentId = studentId,
            serviceId = "ask_leave",
            contactInfo = "International Dormitory",
            notes = notes
        )
        
        RequestSubmissionService.updateStatus(created.id, RequestStatus.Completed)
        
        val dormResponse = client.get("/students/$studentId/dormitory") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, dormResponse.status)
        val dorm = json.decodeFromString<StudentDormitoryDto>(dormResponse.bodyAsText())
        
        assertTrue(dorm.hasActiveLeave)
        assertEquals("High Fever", dorm.leaveReason)
        assertEquals("2026-06-10", dorm.leaveFromDate)
        assertEquals("2026-06-15", dorm.leaveToDate)
        
        RequestSubmissionService.updateStatus(created.id, RequestStatus.InReview)
        
        val dormResponse2 = client.get("/students/$studentId/dormitory") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, dormResponse2.status)
        val dorm2 = json.decodeFromString<StudentDormitoryDto>(dormResponse2.bodyAsText())
        assertTrue(!dorm2.hasActiveLeave)
    }

    @Test
    fun attendanceCorrectionApproval_updatesAttendanceAndInbox() = testApplication {
        application { module() }
        AcademicRepository.seedStudentIfMissing(studentId, preferChinese = false)
        AcademicRepository.ensureDefaultCourseAttendance(studentId)
        
        val detailBefore = AcademicRepository.getAttendanceDetail(studentId, preferChinese = false)
        val targetCourse = detailBefore?.courses?.firstOrNull() ?: fail("No registered courses found")
        val initialAttended = targetCourse.sessionsAttended
        
        val notes = "Course name / 课程名称: ${targetCourse.courseName}\nWhat you want checked / 希望核对或说明的问题: Missed scan corrected"
        val created = RequestSubmissionService.createRequest(
            studentId = studentId,
            serviceId = "attendance_rate",
            contactInfo = "13812345678",
            notes = notes
        )
        
        RequestSubmissionService.updateStatus(created.id, RequestStatus.Completed)
        
        val detailAfter = AcademicRepository.getAttendanceDetail(studentId, preferChinese = false)
        val updatedCourse = detailAfter?.courses?.firstOrNull { it.courseCode == targetCourse.courseCode }
        
        assertNotNull(updatedCourse)
        assertEquals(minOf(initialAttended + 1, targetCourse.sessionsTotal), updatedCourse!!.sessionsAttended)
        
        val inboxResponse = client.get("/students/$studentId/messages") { authHeaders(this) }
        assertEquals(HttpStatusCode.OK, inboxResponse.status)
        val inbox = json.decodeFromString<List<com.cjlu.contract.MessageDto>>(inboxResponse.bodyAsText())
        val confirmMsg = inbox.firstOrNull { it.relatedServiceId == "attendance_rate" }
        assertNotNull(confirmMsg)
    }
}
