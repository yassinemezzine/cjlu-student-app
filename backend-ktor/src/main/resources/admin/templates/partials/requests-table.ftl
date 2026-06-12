<section class="section">
    <h2>Student requests</h2>
    <table>
        <thead>
        <tr>
            <th>Student</th>
            <th>Service</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <#list requests as req>
            <tr data-id="${req.id?html}">
                <td><strong>${req.studentId?html}</strong><br><small>${req.id?html}</small></td>
                <td>${req.serviceId?html}</td>
                <td><span class="status status-${req.statusCss}">${req.statusLabel?html}</span></td>
                <td class="actions">
                    <#if req.attachmentUrl??>
                        <a href="${req.attachmentUrl?html}" target="_blank" class="btn btn-sm">View doc</a>
                    </#if>
                    <button type="button" class="btn btn-sm" data-action="review" data-id="${req.id?html}">Review</button>
                    <button type="button" class="btn btn-sm" data-action="notify" data-id="${req.id?html}">Notify</button>
                    <button type="button" class="btn btn-sm" data-action="complete" data-id="${req.id?html}">Complete</button>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>
    <div class="links">
        <button type="button" class="btn btn-sm" id="view-all-json">View all (JSON)</button>
    </div>
</section>
