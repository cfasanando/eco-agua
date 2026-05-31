package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MarketingFeaturedProductService {

    private final MarketingFeaturedProductRepository featuredProductRepository;
    private final ProductRepository productRepository;

    public MarketingFeaturedProductService(MarketingFeaturedProductRepository featuredProductRepository,
                                           ProductRepository productRepository) {
        this.featuredProductRepository = featuredProductRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingFeaturedProductRow> findRows() {
        return featuredProductRepository.findAllForAdmin().stream()
                .map(MarketingFeaturedProductRow::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarketingFeaturedProduct findForm(Long id) {
        if (id == null) {
            MarketingFeaturedProduct item = new MarketingFeaturedProduct();
            item.setStatus(MarketingFeaturedProduct.Status.PLANNED);
            item.setDisplayPlace(MarketingFeaturedProduct.DisplayPlace.HOME);
            item.setPriority(5);
            item.setStartDate(LocalDate.now());
            item.setCallToAction("Consultar disponibilidad por WhatsApp");
            return item;
        }
        return featuredProductRepository.findByIdForAdmin(id).orElseGet(MarketingFeaturedProduct::new);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public MarketingFeaturedProduct save(MarketingFeaturedProduct form, Long productId) {
        MarketingFeaturedProduct target = form;
        Long previousProductId = null;

        if (form.getId() != null) {
            target = featuredProductRepository.findByIdForAdmin(form.getId()).orElse(form);
            previousProductId = target.getProduct() != null ? target.getProduct().getId() : null;
            copyEditableFields(form, target);
        }

        normalize(target);
        target.setProduct(resolveProduct(productId));

        MarketingFeaturedProduct saved = featuredProductRepository.save(target);

        synchronizeProductFeatured(previousProductId);
        synchronizeProductFeatured(saved.getProduct() != null ? saved.getProduct().getId() : null);

        return saved;
    }

    @Transactional
    public void activate(Long id) {
        updateStatus(id, MarketingFeaturedProduct.Status.ACTIVE);
    }

    @Transactional
    public void pause(Long id) {
        updateStatus(id, MarketingFeaturedProduct.Status.PAUSED);
    }

    @Transactional
    public void finish(Long id) {
        updateStatus(id, MarketingFeaturedProduct.Status.FINISHED);
    }

    @Transactional
    public void archive(Long id) {
        updateStatus(id, MarketingFeaturedProduct.Status.ARCHIVED);
    }

    @Transactional(readOnly = true)
    public Long selectedProductId(MarketingFeaturedProduct item) {
        if (item == null || item.getProduct() == null) {
            return null;
        }
        return item.getProduct().getId();
    }

    private void updateStatus(Long id, MarketingFeaturedProduct.Status status) {
        featuredProductRepository.findByIdForAdmin(id).ifPresent(item -> {
            item.setStatus(status);
            featuredProductRepository.save(item);
            synchronizeProductFeatured(item.getProduct() != null ? item.getProduct().getId() : null);
        });
    }

    private void copyEditableFields(MarketingFeaturedProduct source, MarketingFeaturedProduct target) {
        target.setTitle(source.getTitle());
        target.setShortText(source.getShortText());
        target.setStatus(source.getStatus());
        target.setDisplayPlace(source.getDisplayPlace());
        target.setPriority(source.getPriority());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setCallToAction(source.getCallToAction());
        target.setObservations(source.getObservations());
    }

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId).orElse(null);
    }

    private void normalize(MarketingFeaturedProduct item) {
        if (item.getStatus() == null) {
            item.setStatus(MarketingFeaturedProduct.Status.PLANNED);
        }
        if (item.getDisplayPlace() == null) {
            item.setDisplayPlace(MarketingFeaturedProduct.DisplayPlace.HOME);
        }
        if (item.getPriority() == null || item.getPriority() < 1) {
            item.setPriority(1);
        }
        if (item.getPriority() > 99) {
            item.setPriority(99);
        }
        if (item.getStartDate() == null) {
            item.setStartDate(LocalDate.now());
        }
        if (isBlank(item.getTitle())) {
            item.setTitle("Producto destacado");
        }
        if (isBlank(item.getShortText())) {
            item.setShortText("Producto seleccionado para impulsar en campañas, catálogo y publicaciones.");
        }
        if (isBlank(item.getCallToAction())) {
            item.setCallToAction("Consultar disponibilidad por WhatsApp");
        }
    }

    private void synchronizeProductFeatured(Long productId) {
        if (productId == null) {
            return;
        }

        productRepository.findById(productId).ifPresent(product -> {
            boolean shouldBeFeatured = featuredProductRepository.existsByProductIdAndStatus(
                    productId,
                    MarketingFeaturedProduct.Status.ACTIVE
            );
            if (!Objects.equals(product.isFeatured(), shouldBeFeatured)) {
                product.setFeatured(shouldBeFeatured);
                productRepository.save(product);
            }
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
