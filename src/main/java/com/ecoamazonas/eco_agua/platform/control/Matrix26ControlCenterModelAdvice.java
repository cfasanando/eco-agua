package com.ecoamazonas.eco_agua.platform.control;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlCenterModelAdvice {

    private final Matrix26ControlCenterProperties properties;

    public Matrix26ControlCenterModelAdvice(Matrix26ControlCenterProperties properties) {
        this.properties = properties;
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
    }
}
