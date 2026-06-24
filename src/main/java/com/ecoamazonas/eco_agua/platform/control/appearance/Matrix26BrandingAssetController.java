package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;

@RestController
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BrandingAssetController {

    private final Matrix26BrandingService brandingService;

    public Matrix26BrandingAssetController(Matrix26BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping("/control-center/branding-assets/{instanceId}/{assetCode}")
    public ResponseEntity<Resource> asset(
            @PathVariable Long instanceId,
            @PathVariable String assetCode
    ) {
        Path path = brandingService.draftAssetPath(instanceId, assetCode)
                .orElseThrow(() -> new IllegalArgumentException("El recurso visual no existe."));
        FileSystemResource resource = new FileSystemResource(path);
        MediaType mediaType = mediaType(path.getFileName().toString());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(path.getFileName().toString()).build().toString()
                )
                .body(resource);
    }

    private MediaType mediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".ico")) {
            return MediaType.parseMediaType("image/x-icon");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
