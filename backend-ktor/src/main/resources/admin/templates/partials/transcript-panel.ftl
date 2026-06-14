<section class="section transcript-section">
    <h2>Student transcript</h2>
    <p class="hint">
        Select a student to view and edit their grades. Changes are saved to the database and
        the student app is notified in real time.
    </p>

    <form action="/admin" method="GET" class="stack student-picker-form">
        <input type="hidden" name="panel" value="transcript">
        <label class="field">
            <span>Student</span>
            <select name="student" id="transcript-student-select" required>
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
                    Cumulative GPA: <strong>${transcriptGpa?string["0.##"]}</strong>
                </div>
            </header>

            <#if transcriptCourses?size == 0>
                <p class="hint panel-hint">
                    No transcript data for this student yet. Data is seeded on first login.
                </p>
            <#else>
            <h3>Grades</h3>
            <p class="hint">Edit the score and grade point for each course, then save to sync the student app.</p>
            <form action="/admin/student-transcript" method="POST" class="stack" id="transcript-edit-form">
                <input type="hidden" name="studentId" value="${selectedStudentId?html}">
                <table class="admin-data-table">
                    <thead>
                    <tr>
                        <th>Course</th>
                        <th>Credits</th>
                        <th>Score %</th>
                        <th>Grade point</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list transcriptCourses as row>
                        <tr>
                            <td>
                                <input type="hidden" name="courseCode" value="${row.courseCode?html}">
                                <strong>${row.courseName?html}</strong><br>
                                <small class="muted">${row.courseCode?html}</small>
                            </td>
                            <td class="muted">${row.credits}</td>
                            <td>
                                <input type="number" name="scorePercent" min="0" max="100"
                                       value="${row.scorePercent}" required class="input-sm"
                                       aria-label="Score percent for ${row.courseName?html}">
                            </td>
                            <td>
                                <span class="pill pill-active">${row.gradePoint?string["0.##"]}</span>
                            </td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
                <p class="hint">
                    GPA is recalculated automatically from grade points × credits on save.
                </p>
                <button type="submit" class="btn btn-primary">Save transcript &amp; notify app</button>
            </form>
            </#if>
        </div>
    </#if>
</section>
