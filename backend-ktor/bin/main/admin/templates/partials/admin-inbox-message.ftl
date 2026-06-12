<section class="section admin-inbox-section">
    <h2>Student inbox message</h2>
    <p class="hint">Send a message into the student app <strong>Messages</strong> inbox. Broadcast rows are visible to everyone;
        targeted rows are visible only to the selected student. Connected devices receive a WebSocket refresh.</p>

    <form action="/admin/student-inbox-message" method="POST" class="stack admin-inbox-form" id="admin-inbox-form">
        <label class="field field-check">
            <input type="checkbox" name="audienceAll" id="inbox-audience-all">
            <span>Send to <strong>all students</strong> (broadcast)</span>
        </label>

        <label class="field">
            <span>Target student (when not broadcasting)</span>
            <select name="targetStudentId" id="inbox-target-student">
                <option value="">— Select student —</option>
                <#list rosterStudents as st>
                    <option value="${st.studentId?html}">${st.studentId?html} — ${st.displayName?html}</option>
                </#list>
            </select>
        </label>

        <label class="field">
            <span>Category</span>
            <select name="messageCategory">
                <option value="Announcements">Announcements</option>
                <option value="International">International</option>
                <option value="Academic">Academic</option>
                <option value="Learning">Learning</option>
                <option value="Campus">Campus</option>
            </select>
        </label>

        <label class="field">
            <span>Title (English) <span class="muted">required</span></span>
            <input type="text" name="messageTitleEn" maxlength="200" required placeholder="Short subject line">
        </label>
        <label class="field">
            <span>Body (English) <span class="muted">required</span></span>
            <textarea name="messageBodyEn" rows="4" required placeholder="Message text shown in the app"></textarea>
        </label>

        <label class="field">
            <span>Title (Chinese) <span class="muted">optional</span></span>
            <input type="text" name="messageTitleZh" maxlength="200" placeholder="中文标题（可空，空则用英文）">
        </label>
        <label class="field">
            <span>Body (Chinese) <span class="muted">optional</span></span>
            <textarea name="messageBodyZh" rows="4" placeholder="中文正文（可空，空则用英文）"></textarea>
        </label>

        <label class="field">
            <span>Related service id <span class="muted">optional</span></span>
            <input type="text" name="messageRelatedServiceId" maxlength="50" placeholder="e.g. visa_extension">
        </label>

        <label class="field field-check">
            <input type="checkbox" name="messageRequiresAction" id="inbox-requires-action">
            <span>Mark as <strong>requires action</strong> in the app</span>
        </label>

        <button type="submit" class="btn btn-primary">Send message &amp; notify apps</button>
    </form>
</section>
