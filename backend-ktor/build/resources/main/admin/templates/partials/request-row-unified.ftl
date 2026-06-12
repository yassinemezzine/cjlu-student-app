<#assign initials = (req.displayName!"Unknown")?substring(0, [2, (req.displayName!"Unknown")?length]?min)?upper_case>
<tr data-id="${req.id?html}" data-status="${req.statusCss?html}" class="req-row">
    <td>
        <div class="student-profile-cell">
            <div class="avatar-circle">${initials?html}</div>
            <div class="student-info">
                <strong>${req.displayName?html}</strong>
                <#if req.classSection?has_content>
                    <small class="muted" style="margin-top: 2px; font-weight: 500;">${req.classSection?html}</small>
                </#if>
                <div style="margin-top: 4px; display: flex; flex-direction: column; gap: 4px;">
                    <span class="student-id-tag" data-copy-id="${req.studentId?html}" title="Click to copy ID" style="align-self: flex-start;">
                        🆔 ${req.studentId?html}
                    </span>
                    <#if req.submittedAt?has_content>
                        <div style="font-size: 0.725rem; color: var(--text-muted); display: flex; align-items: center; gap: 4px; font-weight: 500;">
                            🕒 ${req.submittedAt?html}
                        </div>
                    </#if>
                </div>
            </div>
        </div>
    </td>
    <td>
        <div class="service-badge">
            <span class="category-name">${(categoryName!panel.category!"Other")?html}</span>
            <span class="service-name">${(serviceTitle!panel.title!req.serviceId)?html}</span>
        </div>
        <#if req.contactInfo?has_content>
            <div style="margin-top: 6px; font-size: 0.8rem; color: var(--text-muted); font-weight: 500;">
                📞 ${req.contactInfo?html}
            </div>
        </#if>
    </td>
    <td>
        <#if req.notes?has_content>
            <div class="speech-bubble">
                ${req.notes?html}
            </div>
        </#if>
        <#if req.attachmentUrl??>
            <div>
                <a href="${req.attachmentUrl?html}" target="_blank" class="attachment-badge">
                    📎 View Proof
                </a>
            </div>
        </#if>
    </td>
    <td>
        <span class="status status-${req.statusCss}">${req.statusLabel?html}</span>
    </td>
    <td>
        <div class="segmented-actions">
            <button type="button" class="action-btn" data-action="review" data-id="${req.id?html}" title="Mark as In Review">Review</button>
            <button type="button" class="action-btn" data-action="notify" data-id="${req.id?html}" title="Mark as Action Needed">Notify</button>
            <button type="button" class="action-btn" data-action="complete" data-id="${req.id?html}" title="Mark as Completed">Complete</button>
        </div>
    </td>
</tr>
