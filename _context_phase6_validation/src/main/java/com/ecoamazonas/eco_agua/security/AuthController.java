package com.ecoamazonas.eco_agua.security;

import com.ecoamazonas.eco_agua.config.BusinessProperties;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final PlatformSettingService platformSettingService;
    private final BusinessProperties businessProperties;
    private final PasswordResetService passwordResetService;

    public AuthController(
            PlatformSettingService platformSettingService,
            BusinessProperties businessProperties,
            PasswordResetService passwordResetService
    ) {
        this.platformSettingService = platformSettingService;
        this.businessProperties = businessProperties;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model
    ) {
        addLoginSettings(model);

        if (error != null) {
            model.addAttribute("loginError", true);
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Has cerrado sesión correctamente.");
        }

        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }

    @GetMapping("/password-reset/request")
    public String requestPasswordReset(Model model) {
        addLoginSettings(model);
        return "password_reset_request";
    }

    @PostMapping("/password-reset/request")
    public String createPasswordResetRequest(
            @RequestParam("username") String username,
            HttpServletRequest request,
            Model model
    ) {
        addLoginSettings(model);

        PasswordResetService.PasswordResetRequestResult result = passwordResetService.requestPasswordReset(username);
        model.addAttribute("requestProcessed", true);
        model.addAttribute("requestMessage", "Si el usuario existe y está activo, se generó un enlace temporal de recuperación.");

        if (result.rawToken().isPresent() && shouldShowDevelopmentResetLink()) {
            model.addAttribute("devResetUrl", buildResetUrl(request, result.rawToken().get()));
        }

        return "password_reset_request";
    }

    @GetMapping("/password-reset")
    public String showPasswordResetForm(
            @RequestParam(value = "token", required = false) String token,
            Model model
    ) {
        addLoginSettings(model);
        model.addAttribute("token", token);

        if (token == null || token.isBlank() || !passwordResetService.isValidToken(token)) {
            model.addAttribute("resetInvalid", true);
        }

        return "password_reset_form";
    }

    @PostMapping("/password-reset")
    public String resetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        addLoginSettings(model);
        model.addAttribute("token", token);

        PasswordResetService.PasswordResetResult result = passwordResetService.resetPassword(token, password, confirmPassword);
        if (!result.success()) {
            model.addAttribute("resetError", result.message());
            return "password_reset_form";
        }

        redirectAttributes.addFlashAttribute("passwordResetSuccess", result.message());
        return "redirect:/login";
    }

    private void addLoginSettings(Model model) {
        platformSettingService.ensureDefaultsForLoginAccess();

        String defaultLogo = platformSettingService.get("platform.logo", businessProperties.getAdminLogo());
        String primaryColor = platformSettingService.get("login.primary_color", "#0f766e");

        model.addAttribute("loginBackgroundImage", platformSettingService.get("login.background_image", "/img/login-bg2.png"));
        model.addAttribute("loginLogo", platformSettingService.get("login.logo", defaultLogo));
        model.addAttribute("loginTitle", platformSettingService.get("login.title", "Acceso"));
        model.addAttribute("loginSubtitle", platformSettingService.get("login.subtitle", "Ingresa con tu usuario para continuar."));
        model.addAttribute("loginUsernameLabel", platformSettingService.get("login.username_label", "Usuario"));
        model.addAttribute("loginUsernamePlaceholder", platformSettingService.get("login.username_placeholder", "Ingresa tu usuario"));
        model.addAttribute("loginPasswordLabel", platformSettingService.get("login.password_label", "Contraseña"));
        model.addAttribute("loginPasswordPlaceholder", platformSettingService.get("login.password_placeholder", "Ingresa tu contraseña"));
        model.addAttribute("loginRememberLabel", platformSettingService.get("login.remember_label", "Recordarme"));
        model.addAttribute("loginSubmitLabel", platformSettingService.get("login.submit_label", "Acceder"));
        model.addAttribute("loginForgotLabel", platformSettingService.get("login.forgot_label", "¿Olvidaste tu contraseña?"));
        model.addAttribute("loginBackToPublicLabel", platformSettingService.get("login.back_to_public_label", "Volver al portal"));
        model.addAttribute("loginPrimaryColor", primaryColor);
        model.addAttribute("loginPrimaryHoverColor", platformSettingService.get("login.primary_hover_color", primaryColor));
        model.addAttribute("passwordResetRequestTitle", platformSettingService.get("login.password_reset.request_title", "Recuperar contraseña"));
        model.addAttribute("passwordResetRequestText", platformSettingService.get("login.password_reset.request_text", "Ingresa tu usuario. Si existe y está activo, se generará un enlace temporal de recuperación."));
        model.addAttribute("passwordResetSubmitLabel", platformSettingService.get("login.password_reset.submit_label", "Generar enlace de recuperación"));
        model.addAttribute("passwordResetFormTitle", platformSettingService.get("login.password_reset.reset_title", "Crear nueva contraseña"));
        model.addAttribute("passwordResetSaveLabel", platformSettingService.get("login.password_reset.save_label", "Guardar nueva contraseña"));
    }

    private boolean shouldShowDevelopmentResetLink() {
        return Boolean.parseBoolean(platformSettingService.get("login.password_reset.show_dev_link", "true"));
    }

    private String buildResetUrl(HttpServletRequest request, String rawToken) {
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme())
                .append("://")
                .append(request.getServerName());

        if (("http".equals(request.getScheme()) && request.getServerPort() != 80)
                || ("https".equals(request.getScheme()) && request.getServerPort() != 443)) {
            url.append(":").append(request.getServerPort());
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank()) {
            url.append(contextPath);
        }

        return url.append("/password-reset?token=").append(rawToken).toString();
    }
}
