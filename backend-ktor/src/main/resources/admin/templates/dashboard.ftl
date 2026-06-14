<#import "layout.ftl" as layout>
<@layout.adminPage title="CJLU Administration Portal">
<div class="admin-shell">
    <aside class="admin-sidebar">
        <div class="sidebar-brand">
            <span class="brand-mark">CJLU</span>
            <strong>Admin</strong>
        </div>
        <nav class="sidebar-nav" aria-label="Admin sections">
            <button type="button" class="nav-item is-active" data-panel="overview">Overview</button>
            <button type="button" class="nav-item" data-panel="requests">
                Requests
                <#if activeRequests gt 0>
                    <span class="nav-badge">${activeRequests}</span>
                </#if>
            </button>
            <button type="button" class="nav-item" data-panel="register">Register request</button>
            <button type="button" class="nav-item" data-panel="attendance">Attendance rate</button>
            <button type="button" class="nav-item" data-panel="transcript">Transcript</button>
            <button type="button" class="nav-item" data-panel="learning">Student learning</button>
            <button type="button" class="nav-item" data-panel="calendar">Academic Calendar</button>
            <button type="button" class="nav-item" data-panel="messages">Student inbox</button>
        </nav>
        <a href="/admin/logout" class="sidebar-logout">Sign out</a>
    </aside>

    <div class="admin-main">
        <header class="admin-topbar">
            <div>
                <h1>Administration Portal</h1>
                <p>Manage student service requests by category and service</p>
            </div>
        </header>

        <#if errorMessage?has_content>
            <p class="alert alert-error">${errorMessage?html}</p>
        </#if>
        <#if successMessage?has_content>
            <p class="alert alert-success">${successMessage?html}</p>
        </#if>

        <section class="panel is-visible" data-panel="overview">
            <div class="stats stats-wide">
                <div class="stat-card">
                    <div class="stat-value">${totalRequests}</div>
                    <div class="stat-label">Total requests</div>
                </div>
                <div class="stat-card stat-warn">
                    <div class="stat-value">${pendingReview}</div>
                    <div class="stat-label">Submitted</div>
                </div>
                <div class="stat-card stat-info">
                    <div class="stat-value">${inReviewCount}</div>
                    <div class="stat-label">In review</div>
                </div>
                <div class="stat-card stat-danger">
                    <div class="stat-value">${actionNeededCount}</div>
                    <div class="stat-label">Action needed</div>
                </div>
                <div class="stat-card stat-muted">
                    <div class="stat-value">${completedCount}</div>
                    <div class="stat-label">Completed</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${serviceCount}</div>
                    <div class="stat-label">Services in catalog</div>
                </div>
            </div>

            <h2 class="panel-title">By category</h2>
            <p class="panel-hint">Each category groups independent services. Open Requests to manage them separately.</p>
            <div class="category-grid">
                <#list categories as cat>
                    <article class="category-card" data-category="${cat.slug?html}">
                        <h3>${cat.name?html}</h3>
                        <p class="category-meta">${cat.serviceCount} services · ${cat.requestCount} requests</p>
                        <#if cat.activeCount gt 0>
                            <span class="pill pill-active">${cat.activeCount} active</span>
                        <#else>
                            <span class="pill pill-quiet">No active requests</span>
                        </#if>
                        <button type="button" class="btn btn-sm category-jump" data-category="${cat.slug?html}">
                            View services
                        </button>
                    </article>
                </#list>
            </div>
        </section>

        <section class="panel" data-panel="requests" id="requests-panel">
            <div class="requests-tabs">
                <button type="button" class="req-tab-btn is-active" data-req-tab="unified-active">
                    Unified Active Queue
                    <span class="tab-badge" id="active-queue-badge">0</span>
                </button>
                <button type="button" class="req-tab-btn" data-req-tab="browse-service">
                    Browse by Service
                </button>
                <button type="button" class="req-tab-btn" data-req-tab="archive-queue">
                    Resolved Archive
                    <span class="tab-badge" id="archive-queue-badge">0</span>
                </button>
            </div>

            <div class="requests-toolbar">
                <div class="search-container">
                    <span class="search-icon">🔍</span>
                    <input type="text" id="req-search" class="search-input" placeholder="Search student name, ID, service, or notes...">
                </div>
                <div class="filter-row" id="browse-filters" style="display: none;">
                    <label class="field field-inline">
                        <span>Category</span>
                        <select id="filter-category">
                            <option value="all">All categories</option>
                            <#list categories as cat>
                                <option value="${cat.slug?html}">${cat.name?html}</option>
                            </#list>
                            <#if orphanPanels?size gt 0>
                                <option value="other">Other</option>
                            </#if>
                        </select>
                    </label>
                    <label class="field field-inline">
                        <span>Service</span>
                        <select id="filter-service">
                            <option value="all">All services</option>
                            <#list servicePanels as panel>
                                <option value="${panel.serviceId?html}" data-category="${panel.category?lower_case?html}">
                                    ${panel.title?html} (${panel.totalCount})
                                </option>
                            </#list>
                            <#list orphanPanels as panel>
                                <option value="${panel.serviceId?html}" data-category="other">
                                    ${panel.title?html} (${panel.totalCount})
                                </option>
                            </#list>
                        </select>
                    </label>
                    <label class="field field-inline field-check">
                        <input type="checkbox" id="filter-active-only">
                        <span>Active only</span>
                    </label>
                </div>
            </div>

            <!-- Tab Content 1: Unified Active Queue -->
            <div class="tab-content is-active" data-req-tab="unified-active">
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
                    <#assign activeCount = 0>
                    <#list servicePanels as panel>
                        <#list panel.requests as req>
                            <#if req.statusCss != "completed">
                                <#assign activeCount = activeCount + 1>
                                <#include "partials/request-row-unified.ftl">
                            </#if>
                        </#list>
                    </#list>
                    <#list orphanPanels as panel>
                        <#list panel.requests as req>
                            <#if req.statusCss != "completed">
                                <#assign activeCount = activeCount + 1>
                                <#include "partials/request-row-unified.ftl">
                            </#if>
                        </#list>
                    </#list>
                    <#if activeCount == 0>
                        <tr class="empty-row">
                            <td colspan="5" class="empty-service">No active requests in queue!</td>
                        </tr>
                    </#if>
                    </tbody>
                </table>
            </div>

            <!-- Tab Content 2: Browse by Service -->
            <div class="tab-content" data-req-tab="browse-service">
                <#list categories as cat>
                    <div class="service-category-block" data-category="${cat.slug?html}">
                        <header class="category-block-header">
                            <h3>${cat.name?html}</h3>
                            <span class="category-block-meta">${cat.requestCount} requests</span>
                        </header>
                        <#list cat.services as panel>
                            <#include "partials/service-request-panel.ftl">
                        </#list>
                    </div>
                </#list>

                <#if orphanPanels?size gt 0>
                    <div class="service-category-block" data-category="other">
                        <header class="category-block-header">
                            <h3>Other</h3>
                            <span class="category-block-meta">Unknown service IDs</span>
                        </header>
                        <#list orphanPanels as panel>
                            <#include "partials/service-request-panel.ftl">
                        </#list>
                    </div>
                </#if>
            </div>

            <!-- Tab Content 3: Resolved Archive -->
            <div class="tab-content" data-req-tab="archive-queue">
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
                    <#assign completedCount = 0>
                    <#list servicePanels as panel>
                        <#list panel.requests as req>
                            <#if req.statusCss == "completed">
                                <#assign completedCount = completedCount + 1>
                                <#include "partials/request-row-unified.ftl">
                            </#if>
                        </#list>
                    </#list>
                    <#list orphanPanels as panel>
                        <#list panel.requests as req>
                            <#if req.statusCss == "completed">
                                <#assign completedCount = completedCount + 1>
                                <#include "partials/request-row-unified.ftl">
                            </#if>
                        </#list>
                    </#list>
                    <#if completedCount == 0>
                        <tr class="empty-row">
                            <td colspan="5" class="empty-service">No completed requests yet.</td>
                        </tr>
                    </#if>
                    </tbody>
                </table>
            </div>

            <div class="links">
                <button type="button" class="btn btn-sm" id="view-all-json">View all (JSON)</button>
            </div>
        </section>

        <section class="panel" data-panel="register">
            <#include "partials/register-request.ftl">
        </section>

        <section class="panel" data-panel="attendance" id="attendance">
            <#include "partials/attendance-rate-panel.ftl">
        </section>

        <section class="panel" data-panel="transcript" id="transcript">
            <#include "partials/transcript-panel.ftl">
        </section>

        <section class="panel" data-panel="learning" id="learning">
            <#include "partials/student-learning-panel.ftl">
        </section>

        <section class="panel" data-panel="calendar" id="calendar">
            <#include "partials/academic-calendar-panel.ftl">
        </section>

        <section class="panel" data-panel="messages" id="messages">
            <#include "partials/admin-inbox-message.ftl">
        </section>
    </div>
</div>
</@layout.adminPage>
