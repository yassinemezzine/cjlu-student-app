package com.cjlu.backend.admin.service

import com.cjlu.backend.AcademicRepository
import com.cjlu.backend.Database
import com.cjlu.backend.RequestStatus
import com.cjlu.backend.StudentRequest
import com.cjlu.backend.websocket.WebSocketHub

object RequestSubmissionService {

    suspend fun createRequest(
        studentId: String,
        serviceId: String,
        contactInfo: String,
        notes: String,
    ): StudentRequest {
        val newRequest = StudentRequest(
            id = "CJLU-${System.currentTimeMillis()}",
            serviceId = serviceId,
            studentId = studentId,
            contactInfo = contactInfo,
            notes = notes,
            status = RequestStatus.Submitted,
            createdAtMillis = System.currentTimeMillis(),
            attachmentUrl = null,
        )
        Database.addRequest(newRequest)
        WebSocketHub.notifyStudentRequest(newRequest)
        return newRequest
    }

    suspend fun updateStatus(id: String, newStatus: RequestStatus): Boolean {
        if (!Database.updateRequestStatus(id, newStatus)) return false
        val req = Database.getRequestById(id)
        if (req != null) {
            WebSocketHub.notifyStudentRequest(req)
            
            // Unify leave status across request workflow and dormitory presentation
            if (req.serviceId == "ask_leave") {
                val hasLeave = newStatus == RequestStatus.Completed
                if (hasLeave) {
                    val symptomsRegex = Regex("""(?:Illness / symptoms|病情说明):\s*([^\n]+)""")
                    val startRegex = Regex("""(?:Sick leave starts|病假开始):\s*(\d{4}-\d{2}-\d{2})""")
                    val endRegex = Regex("""(?:Sick leave ends|病假结束):\s*(\d{4}-\d{2}-\d{2})""")
                    
                    val reason = symptomsRegex.find(req.notes)?.groupValues?.get(1)?.trim() ?: "Sick leave"
                    val fromDate = startRegex.find(req.notes)?.groupValues?.get(1)
                    val toDate = endRegex.find(req.notes)?.groupValues?.get(1)
                    
                    AcademicRepository.updateDormitoryLeave(req.studentId, true, reason, fromDate, toDate)
                } else {
                    AcademicRepository.updateDormitoryLeave(req.studentId, false, null, null, null)
                }
                
                // Trigger client-side cache refresh for dormitory
                WebSocketHub.notifyAcademicUpdated(req.studentId, "dormitory")
            }

            if (req.serviceId == "attendance_rate") {
                val isApproved = newStatus == RequestStatus.Completed
                if (isApproved) {
                    val courseNameRegex = Regex("""(?:Course name|课程名称):\s*([^\n]+)""")
                    val courseName = courseNameRegex.find(req.notes)?.groupValues?.get(1)?.trim()
                    
                    if (!courseName.isNullOrBlank()) {
                        val courseCode = AcademicRepository.findCourseCodeByName(courseName)
                        if (courseCode != null) {
                            val detail = AcademicRepository.getAttendanceDetail(req.studentId, false)
                            val courseDetail = detail?.courses?.firstOrNull { it.courseCode == courseCode }
                            if (courseDetail != null) {
                                val newAttended = minOf(courseDetail.sessionsAttended + 1, courseDetail.sessionsTotal)
                                val patch = AcademicRepository.CourseAttendancePatch(
                                    courseCode = courseCode,
                                    attendancePercent = if (courseDetail.sessionsTotal > 0) ((newAttended * 100) / courseDetail.sessionsTotal) else 100,
                                    sessionsAttended = newAttended,
                                    sessionsTotal = courseDetail.sessionsTotal
                                )
                                val profile = AcademicRepository.patchCourseAttendanceBatch(req.studentId, listOf(patch))
                                if (profile != null) {
                                    WebSocketHub.notifyAcademicUpdated(req.studentId, "attendance")
                                    WebSocketHub.notifyLearningAlerts(profile)
                                    
                                    Database.insertAdminInboxMessage(
                                        recipientStudentId = req.studentId,
                                        category = "Academic",
                                        senderEn = "Academic Registry",
                                        senderZh = "教务处",
                                        titleEn = "Attendance Correction Approved",
                                        titleZh = "考勤修正申请已批准",
                                        bodyEn = "Your attendance correction request for \"$courseName\" has been approved. Your attendance has been successfully corrected.",
                                        bodyZh = "您关于课程“$courseName”的考勤修正申请已批准，考勤状态已成功修正。",
                                        relatedServiceId = "attendance_rate",
                                        requiresAction = false
                                    )
                                }
                            }
                        }
                    }
                }
            } // Close the attendance_rate block
            if (req.serviceId == "live_off_campus") {
                val isApproved = newStatus == RequestStatus.Completed
                if (isApproved) {
                    val addressRegex = Regex("""(?:Address|住址):\s*([^\n]+)""")
                    val address = addressRegex.find(req.notes)?.groupValues?.get(1)?.trim() ?: "Off-campus registered address"
                    AcademicRepository.updateDormitoryOffCampus(req.studentId, true, address)
                } else {
                    AcademicRepository.updateDormitoryOffCampus(req.studentId, false, null)
                }
                
                // Trigger client-side cache refresh for dormitory
                WebSocketHub.notifyAcademicUpdated(req.studentId, "dormitory")
            }
        }
        return true
    }
}
