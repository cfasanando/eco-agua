(() => {
    const form = document.getElementById('appearance-editor-form');
    const adminPreview = document.getElementById('appearance-live-preview');
    const publicPreview = document.getElementById('appearance-public-preview');
    if (!form || !adminPreview || !publicPreview) return;

    const names = window.matrix26AppearanceNames || { themes: {}, layouts: {}, presets: {} };
    const customPaletteToggle = form.querySelector('[data-custom-palette-toggle]');
    const customPaletteFields = form.querySelector('[data-custom-palette-fields]');
    const HEX = /^#[0-9A-F]{6}$/i;
    const COLOR_VARIABLES = [
        '--theme-primary',
        '--theme-primary-hover',
        '--theme-on-primary',
        '--theme-on-primary-hover',
        '--theme-secondary',
        '--theme-on-secondary',
        '--theme-accent',
        '--theme-on-accent',
        '--theme-background',
        '--theme-surface',
        '--theme-text'
    ];

    const value = (name) => form.querySelector(`[name="${name}"]:checked`)?.value
        || form.querySelector(`[name="${name}"]`)?.value
        || '';

    const customPaletteEnabled = () => Boolean(customPaletteToggle?.checked);

    const normalizeHex = (raw) => {
        const clean = String(raw || '').trim().toUpperCase();
        if (/^[0-9A-F]{6}$/.test(clean)) return `#${clean}`;
        return clean;
    };

    const channel = (value) => {
        const normalized = value / 255;
        return normalized <= 0.03928
            ? normalized / 12.92
            : Math.pow((normalized + 0.055) / 1.055, 2.4);
    };

    const luminance = (color) => {
        if (!HEX.test(color)) return 0;
        const red = parseInt(color.slice(1, 3), 16);
        const green = parseInt(color.slice(3, 5), 16);
        const blue = parseInt(color.slice(5, 7), 16);
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue);
    };

    const contrastRatio = (first, second) => {
        const a = luminance(first);
        const b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    };

    const contrastText = (background) => {
        const white = contrastRatio('#FFFFFF', background);
        const dark = contrastRatio('#111827', background);
        return white >= dark ? '#FFFFFF' : '#111827';
    };

    const darken = (color, factor = 0.15) => {
        if (!HEX.test(color)) return color;
        const components = [1, 3, 5].map(index => Math.round(parseInt(color.slice(index, index + 2), 16) * (1 - factor)));
        return `#${components.map(item => item.toString(16).padStart(2, '0')).join('').toUpperCase()}`;
    };

    const replaceThemeClass = (element, theme) => {
        ['matrix26-classic', 'matrix26-nature', 'matrix26-warm'].forEach(item => element.classList.remove(item));
        if (theme) element.classList.add(theme);
    };

    const syncColorField = (field, source) => {
        const picker = field.querySelector('[data-color-picker-for]');
        const text = field.querySelector('[data-preview-color]');
        if (!picker || !text) return;

        if (source === picker) {
            text.value = picker.value.toUpperCase();
        } else {
            const normalized = normalizeHex(text.value);
            if (HEX.test(normalized)) {
                text.value = normalized;
                picker.value = normalized;
            }
        }
    };

    const applyThemePreset = (themeCode) => {
        const preset = names.presets?.[themeCode];
        if (!preset) return;
        const fields = ['primaryColor', 'secondaryColor', 'accentColor', 'backgroundColor', 'surfaceColor', 'textColor'];
        fields.forEach(fieldName => {
            const input = form.querySelector(`[name="${fieldName}"]`);
            if (!input || !preset[fieldName]) return;
            input.value = preset[fieldName].toUpperCase();
            const picker = form.querySelector(`[data-color-picker-for="${fieldName}"]`);
            if (picker) picker.value = input.value;
        });
    };

    const setPreviewVariable = (name, color) => {
        [adminPreview, publicPreview].forEach(preview => preview.style.setProperty(name, color));
    };

    const clearCustomPaletteVariables = () => {
        [adminPreview, publicPreview].forEach(preview => {
            COLOR_VARIABLES.forEach(variable => preview.style.removeProperty(variable));
        });
    };

    const updatePaletteFieldState = () => {
        const enabled = customPaletteEnabled();
        customPaletteFields?.classList.toggle('is-disabled', !enabled);
        customPaletteFields?.setAttribute('aria-disabled', String(!enabled));
        form.querySelectorAll('[data-color-picker-for]').forEach(input => {
            input.disabled = !enabled;
        });
        form.querySelectorAll('[data-preview-color]').forEach(input => {
            input.readOnly = !enabled;
            input.setAttribute('aria-readonly', String(!enabled));
        });
    };

    const updateColors = () => {
        updatePaletteFieldState();
        if (!customPaletteEnabled()) {
            clearCustomPaletteVariables();
            form.querySelectorAll('[data-contrast-label]').forEach(label => {
                label.textContent = 'Theme default';
                label.style.removeProperty('color');
            });
            return;
        }

        const colors = {};
        form.querySelectorAll('[data-preview-color]').forEach(input => {
            const normalized = normalizeHex(input.value);
            if (!HEX.test(normalized)) {
                input.classList.add('is-invalid');
                return;
            }
            input.classList.remove('is-invalid');
            input.value = normalized;
            colors[input.name] = normalized;
            setPreviewVariable(input.dataset.previewColor, normalized);
        });

        if (colors.primaryColor) {
            const hover = darken(colors.primaryColor);
            setPreviewVariable('--theme-primary-hover', hover);
            setPreviewVariable('--theme-on-primary', contrastText(colors.primaryColor));
            setPreviewVariable('--theme-on-primary-hover', contrastText(hover));
        }
        if (colors.secondaryColor) setPreviewVariable('--theme-on-secondary', contrastText(colors.secondaryColor));
        if (colors.accentColor) setPreviewVariable('--theme-on-accent', contrastText(colors.accentColor));

        form.querySelectorAll('[data-color-field]').forEach(field => {
            const input = field.querySelector('[data-preview-color]');
            const label = field.querySelector('[data-contrast-label]');
            if (!input || !label || !HEX.test(input.value)) return;
            const foreground = contrastText(input.value);
            label.textContent = foreground === '#FFFFFF' ? 'Automatic light text' : 'Automatic dark text';
            label.style.color = input.value;
        });
    };

    const updateOptions = () => {
        const radiusMode = value('borderRadius');
        const radiusSets = {
            SMALL: { small: '8px', medium: '10px', large: '14px' },
            MEDIUM: { small: '10px', medium: '16px', large: '20px' },
            LARGE: { small: '14px', medium: '24px', large: '30px' }
        };
        const density = { COMPACT: '8px', COMFORTABLE: '13px', SPACIOUS: '18px' }[value('tableDensity')] || '13px';
        const width = { STANDARD: '1600px', WIDE: '1800px', FULL: '100%' }[value('contentWidth')] || '1600px';
        [adminPreview, publicPreview].forEach(item => {
            if (radiusMode === 'THEME') {
                item.style.removeProperty('--theme-radius-small');
                item.style.removeProperty('--theme-radius-medium');
                item.style.removeProperty('--theme-radius-large');
            } else {
                const radii = radiusSets[radiusMode] || radiusSets.MEDIUM;
                item.style.setProperty('--theme-radius-small', radii.small);
                item.style.setProperty('--theme-radius-medium', radii.medium);
                item.style.setProperty('--theme-radius-large', radii.large);
            }
            item.style.setProperty('--theme-table-density', density);
            item.style.setProperty('--theme-content-width', width);
        });

        adminPreview.dataset.sidebarMode = value('sidebarMode');
        adminPreview.dataset.headingStyle = value('headingStyle');
        const densityLabel = document.getElementById('preview-density-label');
        if (densityLabel) densityLabel.textContent = value('tableDensity');
    };

    const updateSelections = () => {
        const publicTheme = value('publicThemeCode');
        const adminTheme = value('adminThemeCode');
        const publicLayout = value('publicLayoutCode');
        const adminLayout = value('adminLayoutCode');

        replaceThemeClass(publicPreview, publicTheme);
        replaceThemeClass(adminPreview, adminTheme);
        publicPreview.dataset.publicLayout = publicLayout;
        adminPreview.dataset.adminLayout = adminLayout;

        document.getElementById('preview-public-theme-label').textContent = names.themes[publicTheme] || publicTheme;
        document.getElementById('preview-admin-theme-label').textContent = names.themes[adminTheme] || adminTheme;
        document.getElementById('preview-public-layout-label').textContent = names.layouts[publicLayout] || publicLayout;
        document.getElementById('preview-admin-layout-label').textContent = names.layouts[adminLayout] || adminLayout;

        form.querySelectorAll('.matrix26-visual-choice, .matrix26-layout-choice').forEach(label => {
            const input = label.querySelector('input');
            label.classList.toggle('is-selected', Boolean(input?.checked));
        });
    };

    const refresh = () => {
        updateSelections();
        updateColors();
        updateOptions();
    };

    form.querySelectorAll('[data-color-field]').forEach(field => {
        const picker = field.querySelector('[data-color-picker-for]');
        const text = field.querySelector('[data-preview-color]');
        if (!picker || !text) return;
        picker.addEventListener('input', () => {
            syncColorField(field, picker);
            refresh();
        });
        text.addEventListener('input', () => {
            syncColorField(field, text);
            refresh();
        });
        text.addEventListener('blur', () => {
            syncColorField(field, text);
            refresh();
        });
    });

    form.addEventListener('input', (event) => {
        if (!event.target.closest('[data-color-field]')) refresh();
    });
    form.addEventListener('change', (event) => {
        if (event.target?.name === 'adminThemeCode' && !customPaletteEnabled()) {
            applyThemePreset(event.target.value);
        }
        refresh();
    });
    refresh();
})();
