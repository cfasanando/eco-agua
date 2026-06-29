package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.control.security.Matrix26ControlSecurityService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlCenterModelAdvice {

    private final Matrix26ControlCenterProperties properties;
    private final ObjectProvider<Matrix26ControlSecurityService> securityServiceProvider;

    public Matrix26ControlCenterModelAdvice(
            Matrix26ControlCenterProperties properties,
            ObjectProvider<Matrix26ControlSecurityService> securityServiceProvider
    ) {
        this.properties = properties;
        this.securityServiceProvider = securityServiceProvider;
    }

    @ModelAttribute
    public void addControlCenterAttributes(Model model) {
        model.addAttribute("matrix26ControlCenter", true);
        model.addAttribute("matrix26ControlCenterName", properties.getDisplayName());
        model.addAttribute("businessProfileCode", properties.getRuntimeProfile());
        model.addAttribute("businessName", properties.getDisplayName());
        model.addAttribute("businessShortName", "Matrix26");
        model.addAttribute("businessTagline", "Administración central de instancias empresariales");
        model.addAttribute("businessType", "platform_control");
        model.addAttribute("businessAdminTitle", "Control Center");
        model.addAttribute("businessLogo", "/img/matrix26-mark.svg");
        model.addAttribute("businessAdminLogo", "/img/matrix26-mark.svg");
        model.addAttribute("adminBrandTitle", "Matrix26");
        model.addAttribute("adminBrandSubtitle", "Control Center");
        model.addAttribute("adminBrandLogo", "/img/matrix26-mark.svg");
        model.addAttribute("restaurantRuntime", false);

        Matrix26ControlSecurityService securityService = securityServiceProvider.getIfAvailable();
        model.addAttribute("matrix26CanView", securityService == null || securityService.canView());
        model.addAttribute("matrix26CanManageAlerts", securityService == null || securityService.canManageAlerts());
        model.addAttribute("matrix26CanControlRuntimes", securityService == null || securityService.canControlRuntimes());
        model.addAttribute("matrix26CanManageBackups", securityService == null || securityService.canManageBackups());
        model.addAttribute("matrix26CanManageRestores", securityService == null || securityService.canManageRestores());
        model.addAttribute("matrix26CanManageLifecycle", securityService == null || securityService.canManageLifecycle());
        model.addAttribute("matrix26CanManagePurge", securityService == null || securityService.canManagePurge());
        model.addAttribute("matrix26CanManageAppearance", securityService == null || securityService.canManageAppearance());
        model.addAttribute("matrix26CanManageProvisioning", securityService == null || securityService.canManageProvisioning());
        model.addAttribute("matrix26CanManageModules", securityService == null || securityService.canManageModules());
        model.addAttribute("matrix26CanAdministerSecurity", securityService == null || securityService.canAdministerSecurity());
        model.addAttribute("matrix26CanAdministerSettings", securityService == null || securityService.canAdministerSettings());
    }
}
