(() => {
    const form = document.getElementById('appearance-editor-form');
    const adminPreview = document.getElementById('appearance-live-preview');
    const publicPreview = document.getElementById('appearance-public-preview');
    if (!form || !adminPreview || !publicPreview) return;

    const names = window.matrix26AppearanceNames || { themes: {}, layouts: {}, presets: {} };
    const value = (name) => form.querySelector(`[name="${name}"]:checked`)?.value
        || form.querySelector(`[name="${name}"]`)?.value
        || '';

    const replaceThemeClass = (element, theme) => {
        ['matrix26-classic', 'matrix26-nature', 'matrix26-warm'].forEach(item => element.classList.remove(item));
        if (theme) element.classList.add(theme);
    };


    const applyThemePreset = (themeCode) => {
        const preset = names.presets?.[themeCode];
        if (!preset) return;
        const mapping = {
            primaryColor: 'primaryColor',
            secondaryColor: 'secondaryColor',
            accentColor: 'accentColor',
            backgroundColor: 'backgroundColor',
            surfaceColor: 'surfaceColor',
            textColor: 'textColor'
        };
        Object.entries(mapping).forEach(([presetKey, fieldName]) => {
            const input = form.querySelector(`[name="${fieldName}"]`);
            if (input && preset[presetKey]) input.value = preset[presetKey];
        });
    };

    const updateColors = () => {
        form.querySelectorAll('[data-preview-color]').forEach(input => {
            const cssVariable = input.dataset.previewColor;
            adminPreview.style.setProperty(cssVariable, input.value);
            publicPreview.style.setProperty(cssVariable, input.value);
            const code = input.parentElement?.querySelector('code');
            if (code) code.textContent = input.value.toUpperCase();
        });
    };

    const updateOptions = () => {
        const radius = { SMALL: '10px', MEDIUM: '16px', LARGE: '24px' }[value('borderRadius')] || '16px';
        const density = { COMPACT: '8px', COMFORTABLE: '13px', SPACIOUS: '18px' }[value('tableDensity')] || '13px';
        const width = { STANDARD: '1600px', WIDE: '1800px', FULL: '100%' }[value('contentWidth')] || '1600px';
        [adminPreview, publicPreview].forEach(item => {
            item.style.setProperty('--theme-radius-medium', radius);
            item.style.setProperty('--theme-radius-large', radius);
            item.style.setProperty('--theme-table-density', density);
            item.style.setProperty('--theme-content-width', width);
        });

        const sidebarMode = value('sidebarMode');
        adminPreview.dataset.sidebarMode = sidebarMode;
        adminPreview.dataset.headingStyle = value('headingStyle');
        document.getElementById('preview-density-label').textContent = value('tableDensity');
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

    form.addEventListener('input', refresh);
    form.addEventListener('change', (event) => {
        if (event.target?.name === 'adminThemeCode') {
            applyThemePreset(event.target.value);
        }
        refresh();
    });
    refresh();
})();
