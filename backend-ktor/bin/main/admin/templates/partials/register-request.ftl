<section class="section">
    <h2>Register request (in-person)</h2>
    <p class="hint">Office walk-ins appear in the student app under that service's request list.</p>
    <form action="/admin/requests" method="POST" class="stack">
        <label class="field">
            <span>Student (name or number)</span>
            <input type="text" name="studentId" list="roster-students" placeholder="e.g. 20230901" pattern="[0-9]{8}" required>
            <datalist id="roster-students">
                <#list rosterStudents as stu>
                    <option value="${stu.studentId?html}">${stu.displayName?html} · ${stu.classSection?html}</option>
                </#list>
            </datalist>
        </label>
        <label class="field">
            <span>Service</span>
            <select name="serviceId" required>
                <option value="">Select a service…</option>
                <#list catalog as svc>
                    <option value="${svc.id?html}">[${svc.category?html}] ${svc.title?html}</option>
                </#list>
            </select>
        </label>
        <label class="field">
            <span>Contact info</span>
            <input type="text" name="contactInfo" placeholder="Phone, room, or email">
        </label>
        <label class="field">
            <span>Notes</span>
            <textarea name="notes" placeholder="Reason, dates, or office remarks"></textarea>
        </label>
        <button type="submit" class="btn btn-primary">Register request</button>
    </form>
</section>
