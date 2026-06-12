<section class="section">
    <h2>Student learning</h2>
    <p class="hint">
        Pick a student to edit the weekly class schedule and home banner notice.
        For per-course attendance, use the <strong>Attendance rate</strong> section.
    </p>

    <form action="/admin" method="GET" class="stack student-picker-form">
        <input type="hidden" name="panel" value="learning">
        <label class="field">
            <span>Student</span>
            <select name="student" id="learning-student-select" required>
                <option value="">Select a student…</option>
                <#list rosterStudents as stu>
                    <option value="${stu.studentId?html}"
                            <#if selectedStudentId == stu.studentId>selected</#if>>
                        ${stu.displayName?html} · ${stu.studentId?html} · ${stu.classSection?html}
                    </option>
                </#list>
            </select>
        </label>
        <button type="submit" class="btn btn-primary">Load student</button>
    </form>

    <#if hasSelectedStudent>
        <div class="student-learning-detail">
            <header class="student-learning-header">
                <div>
                    <strong>${selectedStudentName?html}</strong>
                    <span class="muted">Student no. ${selectedStudentId?html}</span>
                    <#if selectedStudentClass?has_content>
                        <span class="pill pill-quiet">${selectedStudentClass?html}</span>
                    </#if>
                </div>
                <div class="overall-badge">Overall attendance: <strong>${overallAttendancePercent}%</strong></div>
            </header>

            <p class="hint">
                Overall attendance: <strong>${overallAttendancePercent}%</strong>
                — edit per-course values under <a href="/admin?student=${selectedStudentId?html}&amp;panel=attendance#attendance">Attendance rate</a>.
            </p>

            <h3>Class schedule (weekly)</h3>
            <form action="/admin/student-timetable" method="POST" class="stack" id="timetable-form">
                <input type="hidden" name="studentId" value="${selectedStudentId?html}">
                <table class="admin-data-table" id="timetable-slots-table">
                    <thead>
                    <tr>
                        <th>Day</th>
                        <th>Start</th>
                        <th>End</th>
                        <th>Course</th>
                        <th>Room</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody id="timetable-slots-body">
                    <#list timetableSlots as slot>
                        <tr class="timetable-slot-row">
                            <td>
                                <select name="dayOfWeek" class="input-sm slot-day">
                                    <option value="1" <#if slot.dayOfWeek == 1>selected</#if>>Mon</option>
                                    <option value="2" <#if slot.dayOfWeek == 2>selected</#if>>Tue</option>
                                    <option value="3" <#if slot.dayOfWeek == 3>selected</#if>>Wed</option>
                                    <option value="4" <#if slot.dayOfWeek == 4>selected</#if>>Thu</option>
                                    <option value="5" <#if slot.dayOfWeek == 5>selected</#if>>Fri</option>
                                    <option value="6" <#if slot.dayOfWeek == 6>selected</#if>>Sat</option>
                                    <option value="7" <#if slot.dayOfWeek == 7>selected</#if>>Sun</option>
                                </select>
                            </td>
                            <td><input type="text" name="startTime" value="${slot.startTime?html}" pattern="\d{2}:\d{2}" placeholder="08:30" class="input-sm" required></td>
                            <td><input type="text" name="endTime" value="${slot.endTime?html}" pattern="\d{2}:\d{2}" placeholder="10:10" class="input-sm" required></td>
                            <td>
                                <select name="courseCode" class="input-sm slot-course" required>
                                    <#list classCourses as c>
                                        <option value="${c.code?html}" <#if slot.courseCode == c.code>selected</#if>>${c.name?html}</option>
                                    </#list>
                                </select>
                            </td>
                            <td><input type="text" name="roomName" value="${slot.roomName?html}" class="input-sm" required></td>
                            <td><button type="button" class="btn btn-sm btn-remove-slot" aria-label="Remove slot">×</button></td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
                <button type="button" class="btn btn-sm" id="add-timetable-slot">Add slot</button>
                <button type="submit" class="btn btn-primary">Save schedule &amp; notify app</button>
            </form>

            <h3>Schedule notice (home banner)</h3>
            <form action="/admin/student-learning" method="POST" class="stack">
                <input type="hidden" name="studentId" value="${selectedStudentId?html}">
                <label class="field">
                    <span>Overall attendance % (optional quick override)</span>
                    <input type="number" name="attendancePercent" min="0" max="100"
                           value="${overallAttendancePercent}" placeholder="e.g. 72">
                </label>
                <label class="field">
                    <span>Class / schedule notice</span>
                    <textarea name="classUpdateNotice" placeholder="e.g. Calculus moved to Room B-302."></textarea>
                </label>
                <label class="checkbox-row">
                    <input type="checkbox" name="clearClassNotice" value="on"> Clear schedule notice
                </label>
                <button type="submit" class="btn">Save notice &amp; notify app</button>
            </form>
        </div>
    </#if>
</section>

<#if hasSelectedStudent && classCourses?size gt 0>
<template id="timetable-slot-template">
    <tr class="timetable-slot-row">
        <td>
            <select name="dayOfWeek" class="input-sm slot-day">
                <option value="1">Mon</option>
                <option value="2">Tue</option>
                <option value="3">Wed</option>
                <option value="4">Thu</option>
                <option value="5">Fri</option>
                <option value="6">Sat</option>
                <option value="7">Sun</option>
            </select>
        </td>
        <td><input type="text" name="startTime" pattern="\d{2}:\d{2}" placeholder="08:30" class="input-sm" required></td>
        <td><input type="text" name="endTime" pattern="\d{2}:\d{2}" placeholder="10:10" class="input-sm" required></td>
        <td>
            <select name="courseCode" class="input-sm slot-course" required>
                <#list classCourses as c>
                    <option value="${c.code?html}">${c.name?html}</option>
                </#list>
            </select>
        </td>
        <td><input type="text" name="roomName" placeholder="Room" class="input-sm" required></td>
        <td><button type="button" class="btn btn-sm btn-remove-slot" aria-label="Remove slot">×</button></td>
    </tr>
</template>
</#if>
