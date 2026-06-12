<section class="section">
    <h2>Student learning alerts</h2>
    <p class="hint">Updates profile and pushes to connected devices: attendance (app warns below 75%) and class notices.</p>
    <form action="/admin/student-learning" method="POST" class="stack">
        <label class="field">
            <span>Student ID</span>
            <input type="text" name="studentId" placeholder="e.g. 20230901" pattern="[0-9]{8}" required>
        </label>
        <label class="field">
            <span>Overall attendance % (0–100, optional)</span>
            <input type="number" name="attendancePercent" min="0" max="100" placeholder="e.g. 72">
        </label>
        <label class="field">
            <span>Class / schedule notice</span>
            <textarea name="classUpdateNotice" placeholder="e.g. Calculus moved to Room B-302."></textarea>
        </label>
        <label class="checkbox-row">
            <input type="checkbox" name="clearClassNotice" value="on"> Clear schedule notice
        </label>
        <button type="submit" class="btn btn-primary">Save &amp; notify student app</button>
    </form>
</section>
