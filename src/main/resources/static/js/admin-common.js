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

    // Auto-init clock on every admin page
    document.addEventListener('DOMContentLoaded', function () {
        ecoAdmin.initTickingClock();
    });

    window.ecoAdmin = ecoAdmin;
})(window, window.jQuery);