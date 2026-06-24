(() => {
    const publicThemeSelect = document.getElementById("quality-public-theme");
    const adminThemeSelect = document.getElementById("quality-admin-theme");
    const publicLayoutSelect = document.getElementById("quality-public-layout");
    const adminLayoutSelect = document.getElementById("quality-admin-layout");
    const loginLayoutSelect = document.getElementById("quality-login-layout");

    if (!publicThemeSelect || !adminThemeSelect || !publicLayoutSelect || !adminLayoutSelect || !loginLayoutSelect) {
        return;
    }

    const themeCodes = Array.from(publicThemeSelect.options).map((option) => option.value);
    const components = document.getElementById("quality-components");
    const publicPreview = document.getElementById("quality-public-preview");
    const adminPreview = document.getElementById("quality-admin-preview");
    const loginPreview = document.getElementById("quality-login-preview");
    const stages = document.querySelectorAll("[data-quality-stage]");
    const viewportButtons = document.querySelectorAll("[data-quality-viewport]");

    const selectedText = (select) => select.options[select.selectedIndex]?.textContent?.trim() || "";

    const applyThemeClass = (element, themeCode) => {
        if (!element) {
            return;
        }

        themeCodes.forEach((code) => element.classList.remove(code));
        element.classList.add(themeCode);
        element.dataset.theme = themeCode;
    };

    const updatePreview = () => {
        const publicThemeCode = publicThemeSelect.value;
        const adminThemeCode = adminThemeSelect.value;

        applyThemeClass(publicPreview, publicThemeCode);
        applyThemeClass(components, adminThemeCode);
        applyThemeClass(adminPreview, adminThemeCode);
        applyThemeClass(loginPreview, adminThemeCode);

        publicPreview.dataset.publicLayout = publicLayoutSelect.value;
        adminPreview.dataset.adminLayout = adminLayoutSelect.value;
        loginPreview.dataset.loginLayout = loginLayoutSelect.value;

        document.getElementById("quality-public-theme-label").textContent = selectedText(publicThemeSelect);
        document.getElementById("quality-admin-theme-label").textContent = selectedText(adminThemeSelect);
        document.getElementById("quality-public-label").textContent = selectedText(publicLayoutSelect);
        document.getElementById("quality-admin-label").textContent = selectedText(adminLayoutSelect);
        document.getElementById("quality-login-label").textContent = selectedText(loginLayoutSelect);
    };

    const setViewport = (viewport) => {
        stages.forEach((stage) => {
            stage.dataset.viewport = viewport;
        });

        viewportButtons.forEach((button) => {
            const active = button.dataset.qualityViewport === viewport;
            button.classList.toggle("is-active", active);
            button.classList.toggle("btn-primary", active);
            button.classList.toggle("btn-outline-secondary", !active);
            button.setAttribute("aria-pressed", active ? "true" : "false");
        });
    };

    [publicThemeSelect, adminThemeSelect, publicLayoutSelect, adminLayoutSelect, loginLayoutSelect]
        .forEach((select) => select.addEventListener("change", updatePreview));

    viewportButtons.forEach((button) => {
        button.addEventListener("click", () => setViewport(button.dataset.qualityViewport));
    });

    updatePreview();
    setViewport("desktop");
})();
