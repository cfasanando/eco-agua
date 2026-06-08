// static/js/admin-common.js
(function (window, $) {
    'use strict';

    const ecoAdmin = window.ecoAdmin || {};

    // Clock in topbar
    ecoAdmin.updateDateTime = function () {
        const now = new Date();
        const dateFormatter = new Intl.DateTimeFormat('es-PE', {
            weekday: 'long',
            day: '2-digit',
            month: 'long',
            year: 'numeric'
        });
        const timeFormatter = new Intl.DateTimeFormat('es-PE', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });

        const dateEl = document.getElementById('currentDate');
        const timeEl = document.getElementById('currentTime');

        if (dateEl && timeEl) {
            dateEl.textContent = dateFormatter.format(now);
            timeEl.textContent = timeFormatter.format(now);
        }
    };

    ecoAdmin.initTickingClock = function () {
        ecoAdmin.updateDateTime();
        setInterval(ecoAdmin.updateDateTime, 1000);
    };

    // Bootstrap tooltip helper
    ecoAdmin.initBootstrapTooltips = function () {
        if (typeof window.bootstrap === 'undefined' || !window.bootstrap.Tooltip) {
            return;
        }

        const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        tooltipTriggerList.forEach(function (tooltipTriggerEl) {
            new window.bootstrap.Tooltip(tooltipTriggerEl);
        });
    };

    // Standard DataTable config
    ecoAdmin.initStandardDataTable = function (selector, nonOrderableColumns) {
        if (!$.fn || !$.fn.DataTable) {
            return null;
        }

        const columnDefs = (nonOrderableColumns || []).map(function (idx) {
            return {
                targets: idx,
                orderable: false,
                searchable: false
            };
        });

        return $(selector).DataTable({
            language: {
                url: 'https://cdn.datatables.net/plug-ins/1.13.8/i18n/es-ES.json'
            },
            pageLength: 30,
            lengthMenu: [
                [10, 30, 50, 100],
                [10, 30, 50, 100]
            ],
            dom: 'Bfrtip',
            autoWidth: false,
            buttons: [
                {
                    extend: 'excelHtml5',
                    text: '<i class="bi bi-file-earmark-excel"></i> Excel',
                    className: 'btn btn-success btn-sm dt-action-btn',
                    titleAttr: 'Exportar a Excel'
                },
                {
                    extend: 'pdfHtml5',
                    text: '<i class="bi bi-file-earmark-pdf"></i> PDF',
                    className: 'btn btn-danger btn-sm dt-action-btn',
                    titleAttr: 'Exportar a PDF'
                },
                {
                    extend: 'print',
                    text: '<i class="bi bi-printer"></i> Imprimir',
                    className: 'btn btn-secondary btn-sm dt-action-btn',
                    titleAttr: 'Imprimir tabla'
                },
                {
                    extend: 'colvis',
                    text: '<i class="bi bi-layout-three-columns"></i> Columnas',
                    className: 'btn btn-dark btn-sm dt-action-btn',
                    titleAttr: 'Mostrar u ocultar columnas'
                }
            ],
            columnDefs: columnDefs
        });
    };

    // Check-all helper
    ecoAdmin.registerCheckAll = function (masterSelector, rowSelector) {
        $(document).on('change', masterSelector, function () {
            const isChecked = this.checked;
            $(rowSelector).prop('checked', isChecked);
        });
    };

    // Single delete confirm
    ecoAdmin.registerSingleDelete = function (formSelector, titleText) {
        $(document).on('submit', formSelector, function (e) {
            e.preventDefault();

            const form = this;

            Swal.fire({
                title: titleText,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Sí, eliminar',
                cancelButtonText: 'Cancelar'
            }).then(function (result) {
                if (result.isConfirmed) {
                    form.submit();
                }
            });
        });
    };

    // Bulk delete helper
    ecoAdmin.registerBulkDelete = function (options) {
        const triggerSelector = options.triggerSelector;
        const checkboxSelector = options.checkboxSelector;
        const deleteUrl = options.deleteUrl;
        const csrfParam = options.csrfParam;
        const csrfToken = options.csrfToken;
        const emptyMessage = options.emptyMessage;
        const confirmTitle = options.confirmTitle;
        const confirmTextBuilder = options.confirmTextBuilder;

        $(document).on('click', triggerSelector, function () {
            const ids = [];

            $(checkboxSelector + ':checked').each(function () {
                ids.push($(this).val());
            });

            if (ids.length === 0) {
                Swal.fire({
                    icon: 'info',
                    title: emptyMessage
                });
                return;
            }

            Swal.fire({
                title: confirmTitle,
                text: confirmTextBuilder(ids.length),
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Sí, eliminar',
                cancelButtonText: 'Cancelar'
            }).then(function (result) {
                if (result.isConfirmed) {
                    const form = document.createElement('form');
                    form.method = 'post';
                    form.action = deleteUrl;

                    if (csrfParam && csrfToken) {
                        const csrfInput = document.createElement('input');
                        csrfInput.type = 'hidden';
                        csrfInput.name = csrfParam;
                        csrfInput.value = csrfToken;
                        form.appendChild(csrfInput);
                    }

                    ids.forEach(function (id) {
                        const idInput = document.createElement('input');
                        idInput.type = 'hidden';
                        idInput.name = 'ids';
                        idInput.value = id;
                        form.appendChild(idInput);
                    });

                    document.body.appendChild(form);
                    form.submit();
                }
            });
        });
    };

    // Flash message helper
    ecoAdmin.showFlashMessage = function (message, type) {
        if (message == null) {
            return;
        }

        let icon = 'success';

        if (type === 'info') {
            icon = 'info';
        }

        if (type === 'error') {
            icon = 'error';
        }

        Swal.fire({
            icon: icon,
            title: message,
            timer: 2500,
            showConfirmButton: false
        });
    };



    // Sidebar visual behavior
    ecoAdmin.initSidebarNavigation = function () {
        const body = document.body;
        const sidebar = document.querySelector('.sidebar');
        const toggleButton = document.querySelector('[data-sidebar-toggle]');
        const storageKey = 'ecoAdmin.sidebarCollapsed';

        if (!sidebar) {
            return;
        }

        const updateToggleState = function () {
            const isCollapsed = body.classList.contains('sidebar-collapsed');
            if (toggleButton) {
                toggleButton.setAttribute('aria-label', isCollapsed ? 'Expandir menú' : 'Contraer menú');
                toggleButton.setAttribute('title', isCollapsed ? 'Expandir menú' : 'Contraer menú');
            }
        };

        if (window.localStorage && window.localStorage.getItem(storageKey) === 'true') {
            body.classList.add('sidebar-collapsed');
        }

        sidebar.querySelectorAll('.menu-link').forEach(function (link) {
            const label = Array.from(link.children)
                .filter(function (child) { return !child.classList.contains('icon') && !child.classList.contains('caret'); })
                .map(function (child) { return child.textContent ? child.textContent.trim() : ''; })
                .filter(Boolean)
                .join(' ');

            if (label && !link.getAttribute('title')) {
                link.setAttribute('title', label);
            }
        });

        if (toggleButton) {
            toggleButton.addEventListener('click', function () {
                body.classList.toggle('sidebar-collapsed');
                if (window.localStorage) {
                    window.localStorage.setItem(storageKey, body.classList.contains('sidebar-collapsed') ? 'true' : 'false');
                }
                updateToggleState();
            });
        }

        sidebar.querySelectorAll('.menu-toggle').forEach(function (checkbox) {
            checkbox.addEventListener('change', function () {
                if (!checkbox.checked || body.classList.contains('sidebar-collapsed')) {
                    return;
                }

                const currentGroup = checkbox.closest('.menu-group');
                if (!currentGroup || !currentGroup.parentElement) {
                    return;
                }

                // Keep the accordion behavior scoped to the current menu level.
                // Nested groups, such as Finance > Accounting sections, must not close their parent group.
                Array.from(currentGroup.parentElement.children).forEach(function (siblingGroup) {
                    if (siblingGroup === currentGroup || !siblingGroup.classList || !siblingGroup.classList.contains('menu-group')) {
                        return;
                    }

                    const siblingToggle = siblingGroup.querySelector(':scope > .menu-toggle');
                    if (siblingToggle) {
                        siblingToggle.checked = false;
                    }

                    siblingGroup.querySelectorAll('.menu-toggle').forEach(function (nestedToggle) {
                        nestedToggle.checked = false;
                    });
                });
            });
        });

        const activeLink = sidebar.querySelector('.menu-link.active');
        if (activeLink && !body.classList.contains('sidebar-collapsed')) {
            setTimeout(function () {
                activeLink.scrollIntoView({ block: 'nearest' });
            }, 80);
        }

        updateToggleState();
    };

    // Auto-init clock on every admin page
    document.addEventListener('DOMContentLoaded', function () {
        ecoAdmin.initTickingClock();
        ecoAdmin.initBootstrapTooltips();
        ecoAdmin.initSidebarNavigation();
    });

    window.ecoAdmin = ecoAdmin;
})(window, window.jQuery);