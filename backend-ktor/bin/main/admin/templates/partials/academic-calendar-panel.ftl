<div class="panel-header">
    <div>
        <h2 class="panel-title">Academic Calendar</h2>
        <p class="panel-hint">Manage the key dates and events shown on the student's Academic Calendar.</p>
    </div>
</div>

<div class="panel-content">
    <form action="/admin/student-academic-calendar" method="POST" class="standard-form">
        
        <div class="table-responsive">
            <table class="data-table" id="calendar-events-table">
                <thead>
                    <tr>
                        <th style="width: 15%">Date</th>
                        <th style="width: 25%">Title (EN / ZH)</th>
                        <th style="width: 35%">Detail (EN / ZH)</th>
                        <th style="width: 15%">Tone</th>
                        <th style="width: 10%">Actions</th>
                    </tr>
                </thead>
                <tbody id="calendar-events-body">
                    <#list calendarEvents as event>
                        <tr class="event-row">
                            <td>
                                <input type="text" name="eventDate" class="input" value="${event.date?html}" required placeholder="e.g. Sep 8, 2026">
                            </td>
                            <td>
                                <input type="text" name="eventTitleEn" class="input" value="${event.titleEn?html}" required placeholder="Title EN" style="margin-bottom: 4px;">
                                <input type="text" name="eventTitleZh" class="input" value="${event.titleZh?html}" placeholder="Title ZH">
                            </td>
                            <td>
                                <input type="text" name="eventDetailEn" class="input" value="${event.detailEn?html}" placeholder="Detail EN" style="margin-bottom: 4px;">
                                <input type="text" name="eventDetailZh" class="input" value="${event.detailZh?html}" placeholder="Detail ZH">
                            </td>
                            <td>
                                <select name="eventTone" class="input">
                                    <option value="blue" <#if event.tone == "blue">selected</#if>>Blue (Info)</option>
                                    <option value="red" <#if event.tone == "red">selected</#if>>Red (Important)</option>
                                    <option value="orange" <#if event.tone == "orange">selected</#if>>Orange (Warning)</option>
                                    <option value="green" <#if event.tone == "green">selected</#if>>Green (Success)</option>
                                    <option value="purple" <#if event.tone == "purple">selected</#if>>Purple (Special)</option>
                                </select>
                            </td>
                            <td>
                                <button type="button" class="btn btn-sm btn-ghost text-danger remove-event-btn">Remove</button>
                            </td>
                        </tr>
                    </#list>
                </tbody>
            </table>
        </div>
        
        <div class="form-actions" style="justify-content: flex-start; margin-top: 1rem;">
            <button type="button" class="btn btn-secondary" id="add-calendar-event-btn">Add Event</button>
        </div>

        <hr class="divider">

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Save Calendar Events</button>
        </div>
    </form>
</div>

<script>
document.addEventListener('DOMContentLoaded', () => {
    const tableBody = document.getElementById('calendar-events-body');
    const addBtn = document.getElementById('add-calendar-event-btn');

    if (tableBody && addBtn) {
        addBtn.addEventListener('click', () => {
            const row = document.createElement('tr');
            row.className = 'event-row';
            row.innerHTML = `
                <td>
                    <input type="text" name="eventDate" class="input" value="" required placeholder="e.g. Sep 8, 2026">
                </td>
                <td>
                    <input type="text" name="eventTitleEn" class="input" value="" required placeholder="Title EN" style="margin-bottom: 4px;">
                    <input type="text" name="eventTitleZh" class="input" value="" placeholder="Title ZH">
                </td>
                <td>
                    <input type="text" name="eventDetailEn" class="input" value="" placeholder="Detail EN" style="margin-bottom: 4px;">
                    <input type="text" name="eventDetailZh" class="input" value="" placeholder="Detail ZH">
                </td>
                <td>
                    <select name="eventTone" class="input">
                        <option value="blue">Blue (Info)</option>
                        <option value="red">Red (Important)</option>
                        <option value="orange">Orange (Warning)</option>
                        <option value="green">Green (Success)</option>
                        <option value="purple">Purple (Special)</option>
                    </select>
                </td>
                <td>
                    <button type="button" class="btn btn-sm btn-ghost text-danger remove-event-btn">Remove</button>
                </td>
            `;
            tableBody.appendChild(row);
        });

        tableBody.addEventListener('click', (e) => {
            if (e.target.classList.contains('remove-event-btn')) {
                const row = e.target.closest('tr');
                if (row) {
                    row.remove();
                }
            }
        });
    }
});
</script>
