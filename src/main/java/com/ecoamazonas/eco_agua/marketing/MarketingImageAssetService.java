package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MarketingImageAssetService {

    private static final String IMAGE_LIBRARY_UPLOAD_DIR = "uploads/marketing/library";
    private static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    private final MarketingImageAssetRepository imageAssetRepository;
    private final ProductRepository productRepository;
    private final MarketingCampaignCalendarRepository campaignRepository;
    private final PromotionRepository promotionRepository;

    public MarketingImageAssetService(MarketingImageAssetRepository imageAssetRepository,
                                      ProductRepository productRepository,
                                      MarketingCampaignCalendarRepository campaignRepository,
                                      PromotionRepository promotionRepository) {
        this.imageAssetRepository = imageAssetRepository;
        this.productRepository = productRepository;
        this.campaignRepository = campaignRepository;
        this.promotionRepository = promotionRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingImageAssetRow> findRows() {
        return imageAssetRepository.findAllForAdmin().stream()
                .map(MarketingImageAssetRow::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarketingImageAsset findForm(Long id) {
        if (id == null) {
            MarketingImageAsset asset = new MarketingImageAsset();
            asset.setStatus(MarketingImageAsset.Status.ACTIVE);
            asset.setAssetType(MarketingImageAsset.AssetType.PRODUCT_PHOTO);
            asset.setRecommendedChannel(MarketingImageAsset.RecommendedChannel.ALL);
            asset.setDescription("Recurso visual para usar en catálogo, redes sociales, blog, WhatsApp o campañas.");
            return asset;
        }
        return imageAssetRepository.findByIdForAdmin(id).orElseGet(MarketingImageAsset::new);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MarketingCampaignCalendarItem> findCampaigns() {
        return campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
    }

    @Transactional(readOnly = true)
    public List<Promotion> findPromotions() {
        return promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
    }

    @Transactional(readOnly = true)
    public Long selectedProductId(MarketingImageAsset asset) {
        return asset != null && asset.getProduct() != null ? asset.getProduct().getId() : null;
    }

    @Transactional(readOnly = true)
    public Long selectedCampaignId(MarketingImageAsset asset) {
        return asset != null && asset.getCampaign() != null ? asset.getCampaign().getId() : null;
    }

    @Transactional(readOnly = true)
    public Long selectedPromotionId(MarketingImageAsset asset) {
        return asset != null && asset.getPromotion() != null ? asset.getPromotion().getId() : null;
    }

    @Transactional
    public MarketingImageAsset save(MarketingImageAsset form,
                                    MultipartFile imageFile,
                                    Long productId,
                                    Long campaignId,
                                    Long promotionId) throws IOException {
        MarketingImageAsset target = form;

        if (form.getId() != null) {
            target = imageAssetRepository.findByIdForAdmin(form.getId()).orElse(form);
            copyEditableFields(form, target);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            target.setImagePath(storeImage(imageFile));
        } else {
            target.setImagePath(normalizeManualPath(target.getImagePath()));
        }

        target.setProduct(resolveProduct(productId));
        target.setCampaign(resolveCampaign(campaignId));
        target.setPromotion(resolvePromotion(promotionId));
        normalize(target);

        return imageAssetRepository.save(target);
    }

    @Transactional
    public void archive(Long id) {
        imageAssetRepository.findByIdForAdmin(id).ifPresent(asset -> {
            asset.setStatus(MarketingImageAsset.Status.ARCHIVED);
            imageAssetRepository.save(asset);
        });
    }

    @Transactional
    public void restore(Long id) {
        imageAssetRepository.findByIdForAdmin(id).ifPresent(asset -> {
            asset.setStatus(MarketingImageAsset.Status.ACTIVE);
            imageAssetRepository.save(asset);
        });
    }

    private void copyEditableFields(MarketingImageAsset source, MarketingImageAsset target) {
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setImagePath(source.getImagePath());
        target.setAssetType(source.getAssetType());
        target.setRecommendedChannel(source.getRecommendedChannel());
        target.setStatus(source.getStatus());
        target.setObservations(source.getObservations());
    }

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId).orElse(null);
    }

    private MarketingCampaignCalendarItem resolveCampaign(Long campaignId) {
        if (campaignId == null) {
            return null;
        }
        return campaignRepository.findById(campaignId).orElse(null);
    }

    private Promotion resolvePromotion(Long promotionId) {
        if (promotionId == null) {
            return null;
        }
        return promotionRepository.findById(promotionId).orElse(null);
    }

    private String storeImage(MultipartFile file) throws IOException {
        validateImageUpload(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        String fileName = "marketing-image-" + LocalDateTime.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + extension;

        Path uploadDir = Path.of(IMAGE_LIBRARY_UPLOAD_DIR).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        Path target = uploadDir.resolve(fileName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid image upload path.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/" + IMAGE_LIBRARY_UPLOAD_DIR + "/" + fileName;
    }

    private void validateImageUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file is too large.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image file extension.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported image file type.");
        }
    }

    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeManualPath(String imagePath) {
        if (!StringUtils.hasText(imagePath)) {
            return null;
        }
        String trimmedPath = imagePath.trim();
        return trimmedPath.startsWith("/") ? trimmedPath : "/" + trimmedPath;
    }

    private void normalize(MarketingImageAsset asset) {
        if (!StringUtils.hasText(asset.getTitle())) {
            asset.setTitle("Recurso visual de marketing");
        }
        if (!StringUtils.hasText(asset.getDescription())) {
            asset.setDescription("Imagen disponible para publicaciones, catálogo, blog, WhatsApp o campañas.");
        }
        if (!StringUtils.hasText(asset.getImagePath())) {
            throw new IllegalArgumentException("An image file or image path is required.");
        }
        if (asset.getAssetType() == null) {
            asset.setAssetType(MarketingImageAsset.AssetType.PRODUCT_PHOTO);
        }
        if (asset.getRecommendedChannel() == null) {
            asset.setRecommendedChannel(MarketingImageAsset.RecommendedChannel.ALL);
        }
        if (asset.getStatus() == null) {
            asset.setStatus(MarketingImageAsset.Status.ACTIVE);
        }
    }
}
