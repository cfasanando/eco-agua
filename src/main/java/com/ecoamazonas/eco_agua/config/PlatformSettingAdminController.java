package com.ecoamazonas.eco_agua.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/admin/platform-settings")
public class PlatformSettingAdminController {

    private static final String PORTAL_UPLOAD_ROOT = "uploads";
    private static final long MAX_IMAGE_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PlatformSettingRepository platformSettingRepository;
    private final PlatformSettingService platformSettingService;
    private final SystemModuleService systemModuleService;
    private final BusinessProperties businessProperties;

    public PlatformSettingAdminController(PlatformSettingRepository platformSettingRepository,
                                          PlatformSettingService platformSettingService,
                                          SystemModuleService systemModuleService,
                                          BusinessProperties businessProperties) {
        this.platformSettingRepository = platformSettingRepository;
        this.platformSettingService = platformSettingService;
        this.systemModuleService = systemModuleService;
        this.businessProperties = businessProperties;
    }

    @GetMapping
    public String showSettings(Model model) {
        platformSettingService.ensureDefaultsForPublicSite();
        systemModuleService.ensureDefaults();

        List<PlatformSetting> settings = platformSettingRepository
                .findByCategoryInOrderByCategoryAscVariableAsc(List.of("platform", "public_site"));

        model.addAttribute("activePage", "platform_settings");
        model.addAttribute("settings", settings);
        return "admin/platform_settings";
    }

    @PostMapping
    public String updateSettings(
            @RequestParam("id") List<Long> ids,
            @RequestParam("value") List<String> values,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            for (int i = 0; i < ids.size(); i++) {
                Long id = ids.get(i);
                String newValue = i < values.size() ? values.get(i) : "";

                Optional<PlatformSetting> opt = platformSettingRepository.findById(id);
                if (opt.isEmpty()) {
                    continue;
                }

                PlatformSetting setting = opt.get();
                String finalValue = newValue != null ? newValue.trim() : "";

                MultipartFile imageFile = findImageFileForSetting(request, id);
                if (imageFile != null && !imageFile.isEmpty()) {
                    if (!isImageSetting(setting)) {
                        throw new IllegalArgumentException("This setting does not accept image uploads: " + setting.getVariable());
                    }

                    finalValue = storePortalImage(setting, imageFile);
                }

                setting.setValue(BrandingTextSanitizer.clean(finalValue));
                platformSettingRepository.save(setting);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Settings updated successfully.");
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/platform-settings";
    }

    private MultipartFile findImageFileForSetting(HttpServletRequest request, Long settingId) {
        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            return null;
        }

        return multipartRequest.getFile("imageFile_" + settingId);
    }

    private boolean isImageSetting(PlatformSetting setting) {
        String variable = safeLower(setting.getVariable());
        String type = safeLower(setting.getType());

        return "image".equals(type)
                || variable.contains("image")
                || variable.contains("logo")
                || variable.contains("favicon");
    }

    private String storePortalImage(PlatformSetting setting, MultipartFile file) throws IOException {
        validateImageUpload(file);

        String profileCode = sanitizePathSegment(businessProperties.getProfileCode());
        String variableSegment = sanitizeFileName(setting.getVariable());
        String extension = getExtension(file.getOriginalFilename());
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String fileName = variableSegment + "-" + timestamp + "." + extension;

        Path uploadDir = Paths.get(PORTAL_UPLOAD_ROOT, profileCode, "portal").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        Path targetFile = uploadDir.resolve(fileName).normalize();
        if (!targetFile.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid image upload path.");
        }

        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + profileCode + "/portal/" + fileName;
    }

    private void validateImageUpload(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_UPLOAD_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file is too large. Maximum allowed size is 5 MB.");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid image format. Allowed formats: JPG, PNG and WEBP.");
        }

        String contentType = safeLower(file.getContentType());
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid image content type.");
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizePathSegment(String value) {
        String sanitized = sanitizeFileName(value);
        return sanitized.isBlank() ? "default" : sanitized;
    }

    private String sanitizeFileName(String value) {
        if (value == null) {
            return "file";
        }

        String sanitized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        return sanitized.isBlank() ? "file" : sanitized;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
