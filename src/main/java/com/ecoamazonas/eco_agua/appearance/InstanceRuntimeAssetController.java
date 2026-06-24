package com.ecoamazonas.eco_agua.appearance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

@RestController
@ConditionalOnProperty(
        name = "matrix26.control-center.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InstanceRuntimeAssetController {

    private static final Pattern SAFE_FILE = Pattern.compile("^[a-z0-9-]+\\.(png|jpg|jpeg|webp|ico)$");

    private final Path currentDirectory;

    public InstanceRuntimeAssetController(
            @Value("${ecoagua.platform.client-code:}") String clientCode,
            @Value("${matrix26.appearance-data-directory:runtime-data}") String dataDirectory
    ) {
        String safeCode = clientCode == null ? "" : clientCode.trim().toLowerCase(Locale.ROOT);
        this.currentDirectory = Path.of(dataDirectory)
                .toAbsolutePath()
                .normalize()
                .resolve(safeCode)
                .resolve("appearance")
                .resolve("current")
                .normalize();
    }

    @GetMapping("/runtime-assets/{fileName:.+}")
    public ResponseEntity<Resource> asset(@PathVariable String fileName) {
        String safeName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (!SAFE_FILE.matcher(safeName).matches()) {
            return ResponseEntity.notFound().build();
        }
        Path path = currentDirectory.resolve(safeName).normalize();
        if (!path.startsWith(currentDirectory) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(mediaType(safeName))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(new FileSystemResource(path));
    }

    private MediaType mediaType(String fileName) {
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (fileName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (fileName.endsWith(".ico")) {
            return MediaType.parseMediaType("image/x-icon");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
