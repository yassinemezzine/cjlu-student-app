<section class="section attendance-rate-section">
    <h2>Attendance rate</h2>
    <p class="hint">
        Edit <strong>sessions attended</strong> and <strong>sessions total</strong> per course; attendance % and overall
        update automatically. Save to sync the student app.
    </p>

    <form action="/admin" method="GET" class="stack student-picker-form">
        <input type="hidden" name="panel" value="attendance">
        <label class="field">
            <span>Student</span>
            <select name="student" id="attendance-student-select" required>
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
                <div class="overall-badge">
                    Overall attendance: <strong><span id="attendance-overall-percent">${overallAttendancePercent}</span>%</strong>
                </div>
            </header>

            <#if courseAttendanceRows?size == 0>
                <p class="hint panel-hint">
                    No courses in the class catalog. Restart the backend to seed default courses.
                </p>
            <#else>
            <h3>Per-course attendance</h3>
            <p class="hint">Attendance % is derived from sessions; saving updates weekly trend and notifies the app.</p>
            <form action="/admin/student-attendance" method="POST" class="stack" id="attendance-edit-form">
                <input type="hidden" name="studentId" value="${selectedStudentId?html}">
                <table class="admin-data-table">
                    <thead>
                    <tr>
                        <th>Course</th>
                        <th>Attendance % <span class="muted">(auto)</span></th>
                        <th>Sessions attended</th>
                        <th>Sessions total</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list courseAttendanceRows as row>
                        <tr>
                            <td>
                                <input type="hidden" name="courseCode" value="${row.courseCode?html}">
                                <strong>${row.courseName?html}</strong><br>
                                <small class="muted">${row.courseCode?html}</small>
                            </td>
                            <td>
                                <input type="number" name="attendancePercent" min="0" max="100"
                                       value="${row.attendancePercent}" required readonly class="input-sm input-readonly"
                                       title="Computed from sessions attended ÷ sessions total"
                                       aria-readonly="true">
                            </td>
                            <td>
                                <input type="number" name="sessionsAttended" min="0"
                                       value="${row.sessionsAttended}" class="input-sm">
                            </td>
                            <td>
                                <input type="number" name="sessionsTotal" min="1"
                                       value="${row.sessionsTotal}" class="input-sm">
                            </td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
                <#if weeklyTrend?size gt 0>
                    <div class="weekly-trend-readonly">
                        <span class="label">Weekly trend (read-only)</span>
                        <#list weeklyTrend as w>
                            <span class="pill pill-quiet">${w.weekLabel?html}: ${w.percent}%</span>
                        </#list>
                    </div>
                </#if>
                <button type="submit" class="btn btn-primary">Save attendance &amp; notify app</button>
            </form>
            </#if>
        </div>
    </#if>
</section>
