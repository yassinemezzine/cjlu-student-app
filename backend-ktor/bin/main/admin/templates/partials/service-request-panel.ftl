<article class="service-panel"
         data-service-id="${panel.serviceId?html}"
         data-category="${panel.category?lower_case?html}"
         data-active-count="${panel.activeCount}"
         data-total-count="${panel.totalCount}">
    <button type="button" class="service-panel-header" aria-expanded="false">
        <div class="service-panel-title">
            <strong>${panel.title?html}</strong>
            <code class="service-id">${panel.serviceId?html}</code>
        </div>
        <div class="service-panel-badges">
            <#if panel.isPopular>
                <span class="pill pill-popular">Popular</span>
            </#if>
            <#if panel.activeCount gt 0>
                <span class="pill pill-active">${panel.activeCount} active</span>
            </#if>
            <span class="pill pill-quiet">${panel.totalCount} total</span>
        </div>
    </button>
    <div class="service-panel-body">
        <#if panel.requests?size gt 0>
            <table class="service-table">
                <thead>
                <tr>
                    <th>Student</th>
                    <th>Service & Category</th>
                    <th>Notes</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <#list panel.requests as req>
                    <#include "request-row-unified.ftl">
                </#list>
                </tbody>
            </table>
        <#else>
            <p class="empty-service">No requests for this service yet.</p>
        </#if>
    </div>
</article>
