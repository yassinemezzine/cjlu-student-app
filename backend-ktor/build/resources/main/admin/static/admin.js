(function () {
    const STATUS_CSS = {
        Submitted: 'submitted',
        InReview: 'in_review',
        ActionNeeded: 'action_needed',
        Completed: 'completed',
    };
    const STATUS_BY_ACTION = {
        review: 'InReview',
        notify: 'ActionNeeded',
        complete: 'Completed',
    };
    const API_REQUESTS = '/admin/api/requests';

    function showPanel(panelId) {
        document.querySelectorAll('.panel').forEach(function (el) {
            el.classList.toggle('is-visible', el.getAttribute('data-panel') === panelId);
        });
        document.querySelectorAll('.nav-item').forEach(function (btn) {
            btn.classList.toggle('is-active', btn.getAttribute('data-panel') === panelId);
        });
    }

    document.querySelectorAll('.nav-item[data-panel]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            showPanel(btn.getAttribute('data-panel'));
        });
    });

    document.querySelectorAll('.category-jump').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const category = btn.getAttribute('data-category');
            showPanel('requests');
            const categorySelect = document.getElementById('filter-category');
            if (categorySelect && category) {
                categorySelect.value = category;
                applyFilters();
            }
        });
    });

    function applyFilters() {
        const category = document.getElementById('filter-category')?.value || 'all';
        const service = document.getElementById('filter-service')?.value || 'all';
        const activeOnly = document.getElementById('filter-active-only')?.checked || false;

        document.querySelectorAll('.service-category-block').forEach(function (block) {
            const blockCat = block.getAttribute('data-category');
            const categoryMatch = category === 'all' || blockCat === category;
            let anyVisible = false;

            block.querySelectorAll('.service-panel').forEach(function (panel) {
                const panelService = panel.getAttribute('data-service-id');
                const activeCount = parseInt(panel.getAttribute('data-active-count') || '0', 10);
                const serviceMatch = service === 'all' || panelService === service;
                const activeMatch = !activeOnly || activeCount > 0;
                const visible = categoryMatch && serviceMatch && activeMatch;
                panel.classList.toggle('is-hidden', !visible);
                if (visible) anyVisible = true;
            });

            block.classList.toggle('is-hidden', !categoryMatch || !anyVisible);
        });
    }

    const filterCategory = document.getElementById('filter-category');
    const filterService = document.getElementById('filter-service');
    const filterActiveOnly = document.getElementById('filter-active-only');

    if (filterCategory) {
        filterCategory.addEventListener('change', function () {
            if (filterService) {
                const cat = filterCategory.value;
                Array.from(filterService.options).forEach(function (opt) {
                    if (opt.value === 'all') return;
                    const optCat = opt.getAttribute('data-category');
                    opt.hidden = cat !== 'all' && optCat !== cat;
                });
                if (filterService.selectedOptions[0]?.hidden) {
                    filterService.value = 'all';
                }
            }
            applyFilters();
        });
    }
    if (filterService) filterService.addEventListener('change', applyFilters);
    if (filterActiveOnly) filterActiveOnly.addEventListener('change', applyFilters);

    document.querySelectorAll('.service-panel-header').forEach(function (header) {
        const panel = header.closest('.service-panel');
        if (!panel) return;

        const activeCount = parseInt(panel.getAttribute('data-active-count') || '0', 10);
        if (activeCount === 0) {
            panel.classList.add('is-collapsed');
            header.setAttribute('aria-expanded', 'false');
        } else {
            header.setAttribute('aria-expanded', 'true');
        }

        header.addEventListener('click', function () {
            const collapsed = panel.classList.toggle('is-collapsed');
            header.setAttribute('aria-expanded', collapsed ? 'false' : 'true');
        });
    });

    async function openAdminJson(path) {
        const res = await fetch(path, { credentials: 'include' });
        const text = await res.text();
        const w = window.open();
        if (w) {
            w.document.write(
                '<pre style="font-family:monospace;white-space:pre-wrap;">' +
                    text.replace(/</g, '&lt;') +
                    '</pre>',
            );
            w.document.close();
        }
    }

    async function updateStatus(id, status) {
        console.log('Updating request status...', { id, status });
        const row = document.querySelector('tr[data-id="' + id + '"]');
        if (row) {
            const span = row.querySelector('.status');
            if (span) {
                span.className = 'status status-' + (STATUS_CSS[status] || status.toLowerCase());
                span.textContent = status.replace(/([A-Z])/g, ' $1').trim();
                span.style.opacity = '0.5';
            }
        }
        try {
            const params = new URLSearchParams();
            params.append('status', status);
            
            const res = await fetch(API_REQUESTS + '/' + encodeURIComponent(id) + '/status', {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                credentials: 'include',
                body: params.toString(),
            });
            
            if (!res.ok) {
                const text = await res.text();
                console.error('Server returned an error status:', res.status, text);
                alert('Failed to update status (' + res.status + '): ' + text);
                location.reload();
            } else {
                console.log('Status updated successfully on server, reloading...');
                location.reload();
            }
        } catch (err) {
            console.error('Network or exception during status update:', err);
            alert('Error updating status: ' + err.message);
        }
    }

    document.querySelectorAll('[data-action][data-id]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const action = btn.getAttribute('data-action');
            const id = btn.getAttribute('data-id');
            const status = STATUS_BY_ACTION[action];
            console.log('Action button clicked:', { action, id, status });
            if (id && status) {
                updateStatus(id, status);
            } else {
                console.warn('Action button clicked but missing required attributes or mapping:', { action, id, status });
            }
        });
    });

    // —— Reorganized Requests Tabs switching logic ——
    function showRequestTab(tabId) {
        document.querySelectorAll('#requests-panel .req-tab-btn').forEach(function (btn) {
            btn.classList.toggle('is-active', btn.getAttribute('data-req-tab') === tabId);
        });
        document.querySelectorAll('#requests-panel .tab-content').forEach(function (content) {
            content.classList.toggle('is-active', content.getAttribute('data-req-tab') === tabId);
        });
        const browseFilters = document.getElementById('browse-filters');
        if (browseFilters) {
            browseFilters.style.display = tabId === 'browse-service' ? 'flex' : 'none';
        }
        applyRealtimeSearch();
    }

    document.querySelectorAll('#requests-panel .req-tab-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            showRequestTab(btn.getAttribute('data-req-tab'));
        });
    });

    // —— Real-time Fuzzy Search ——
    function applyRealtimeSearch() {
        const query = (document.getElementById('req-search')?.value || '').toLowerCase().trim();
        const activeTabContent = document.querySelector('#requests-panel .tab-content.is-active');
        if (!activeTabContent) return;

        const tabId = activeTabContent.getAttribute('data-req-tab');

        if (tabId === 'browse-service') {
            document.querySelectorAll('.service-category-block').forEach(function (block) {
                let blockVisibleRows = 0;
                block.querySelectorAll('.service-panel').forEach(function (panel) {
                    let panelVisibleRows = 0;
                    panel.querySelectorAll('tbody tr.req-row').forEach(function (row) {
                        const text = row.textContent.toLowerCase();
                        const match = text.includes(query);
                        row.classList.toggle('is-filtered-out', !match);
                        if (match) {
                            panelVisibleRows++;
                            blockVisibleRows++;
                        }
                    });
                    if (query.length > 0) {
                        panel.classList.toggle('is-hidden', panelVisibleRows === 0);
                        panel.classList.toggle('is-collapsed', panelVisibleRows === 0);
                    } else {
                        const activeCount = parseInt(panel.getAttribute('data-active-count') || '0', 10);
                        panel.classList.remove('is-hidden');
                        panel.classList.toggle('is-collapsed', activeCount === 0);
                    }
                });
                block.classList.toggle('is-hidden', blockVisibleRows === 0);
            });
        } else {
            const rows = activeTabContent.querySelectorAll('tbody tr.req-row');
            let visibleCount = 0;

            rows.forEach(function (row) {
                if (row.classList.contains('empty-row')) return;
                const text = row.textContent.toLowerCase();
                const match = text.includes(query);
                row.classList.toggle('is-filtered-out', !match);
                if (match) {
                    visibleCount++;
                }
            });

            let emptyRow = activeTabContent.querySelector('tr.empty-row');
            if (!emptyRow && visibleCount === 0) {
                const tbody = activeTabContent.querySelector('tbody');
                if (tbody) {
                    emptyRow = document.createElement('tr');
                    emptyRow.className = 'empty-row';
                    emptyRow.innerHTML = `<td colspan="5" class="empty-service">No requests match "${query}"</td>`;
                    tbody.appendChild(emptyRow);
                }
            } else if (emptyRow && visibleCount > 0) {
                emptyRow.remove();
            } else if (emptyRow && visibleCount === 0) {
                const td = emptyRow.querySelector('td');
                if (td) td.textContent = `No requests match "${query}"`;
            }
        }
    }

    const searchInput = document.getElementById('req-search');
    if (searchInput) {
        searchInput.addEventListener('input', applyRealtimeSearch);
    }

    // —— Copy Student ID Helper ——
    document.querySelectorAll('.student-id-tag').forEach(function (tag) {
        tag.addEventListener('click', function (e) {
            e.stopPropagation();
            const id = tag.getAttribute('data-copy-id');
            navigator.clipboard.writeText(id).then(function () {
                const originalText = tag.innerHTML;
                tag.innerHTML = '✓ Copied';
                tag.style.borderColor = 'var(--success-border)';
                tag.style.color = 'var(--success-text)';
                setTimeout(function () {
                    tag.innerHTML = originalText;
                    tag.style.borderColor = 'transparent';
                    tag.style.color = 'var(--text-muted)';
                }, 1200);
            });
        });
    });

    // —— Compute & update badges count on request tabs ——
    function updateTabBadgeCounts() {
        const activeCount = document.querySelectorAll('.tab-content[data-req-tab="unified-active"] tbody tr.req-row').length;
        const archiveCount = document.querySelectorAll('.tab-content[data-req-tab="archive-queue"] tbody tr.req-row').length;

        const activeBadge = document.getElementById('active-queue-badge');
        if (activeBadge) activeBadge.textContent = activeCount;

        const archiveBadge = document.getElementById('archive-queue-badge');
        if (archiveBadge) archiveBadge.textContent = archiveCount;
    }

    updateTabBadgeCounts();

    const jsonBtn = document.getElementById('view-all-json');
    if (jsonBtn) {
        jsonBtn.addEventListener('click', function () {
            openAdminJson(API_REQUESTS);
        });
    }

    applyFilters();

    const panelParam = new URLSearchParams(window.location.search).get('panel');
    if (panelParam === 'requests' || window.location.hash === '#requests') {
        showPanel('requests');
    } else if (panelParam === 'attendance' || window.location.hash === '#attendance') {
        showPanel('attendance');
    } else if (panelParam === 'learning' || window.location.hash === '#learning') {
        showPanel('learning');
    } else if (panelParam === 'messages' || window.location.hash === '#messages') {
        showPanel('messages');
    }

    const inboxAudienceAll = document.getElementById('inbox-audience-all');
    const inboxTargetStudent = document.getElementById('inbox-target-student');
    if (inboxAudienceAll && inboxTargetStudent) {
        function syncInboxAudience() {
            const all = inboxAudienceAll.checked;
            inboxTargetStudent.disabled = all;
            if (all) inboxTargetStudent.value = '';
        }
        inboxAudienceAll.addEventListener('change', syncInboxAudience);
        syncInboxAudience();
    }

    const attendanceForm = document.getElementById('attendance-edit-form');
    const overallEl = document.getElementById('attendance-overall-percent');
    if (attendanceForm && overallEl) {
        function percentFromSessions(attended, total) {
            const t = Math.max(1, total);
            const a = Math.min(Math.max(0, attended), t);
            return Math.trunc((a * 100) / t);
        }
        function recalcAttendancePreview() {
            const rows = attendanceForm.querySelectorAll('tbody tr');
            let weighted = 0;
            let sumTotal = 0;
            rows.forEach(function (row) {
                const attIn = row.querySelector('input[name="sessionsAttended"]');
                const totIn = row.querySelector('input[name="sessionsTotal"]');
                const pctIn = row.querySelector('input[name="attendancePercent"]');
                if (!attIn || !totIn || !pctIn) return;
                const totalRaw = parseInt(totIn.value, 10);
                const attRaw = parseInt(attIn.value, 10);
                const attended = Number.isNaN(attRaw) ? 0 : attRaw;
                if (Number.isNaN(totalRaw) || totalRaw < 1) {
                    pctIn.value = '0';
                    return;
                }
                const total = totalRaw;
                const clamped = Math.min(Math.max(0, attended), total);
                const pct = percentFromSessions(clamped, total);
                pctIn.value = String(pct);
                weighted += pct * total;
                sumTotal += total;
            });
            overallEl.textContent = sumTotal > 0 ? String(Math.trunc(weighted / sumTotal)) : '0';
        }
        attendanceForm.addEventListener('input', recalcAttendancePreview);
        attendanceForm.addEventListener('change', recalcAttendancePreview);
        recalcAttendancePreview();
    }

    const addSlotBtn = document.getElementById('add-timetable-slot');
    const slotTemplate = document.getElementById('timetable-slot-template');
    const slotsBody = document.getElementById('timetable-slots-body');
    if (addSlotBtn && slotTemplate && slotsBody) {
        function appendTimetableRow() {
            slotsBody.appendChild(slotTemplate.content.cloneNode(true));
        }
        if (slotsBody.querySelectorAll('tr').length === 0) {
            appendTimetableRow();
        }
        addSlotBtn.addEventListener('click', appendTimetableRow);
        slotsBody.addEventListener('click', function (e) {
            const btn = e.target.closest('.btn-remove-slot');
            if (!btn) return;
            const row = btn.closest('tr');
            if (row && slotsBody.querySelectorAll('tr').length > 1) {
                row.remove();
            } else if (row) {
                row.querySelectorAll('input').forEach(function (inp) {
                    inp.value = '';
                });
            }
        });
    }
})();
