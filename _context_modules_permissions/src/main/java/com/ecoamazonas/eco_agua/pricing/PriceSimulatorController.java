package com.ecoamazonas.eco_agua.pricing;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.client.ClientService;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductService;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionProduct;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/price-simulator")
public class PriceSimulatorController {

    private final ClientService clientService;
    private final ProductService productService;
    private final PriceSimulationService simulationService;

    public PriceSimulatorController(
            ClientService clientService,
            ProductService productService,
            PriceSimulationService simulationService
    ) {
        this.clientService = clientService;
        this.productService = productService;
        this.simulationService = simulationService;
    }

    @GetMapping
    public String showSimulator(
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "promotionId", required = false) Long promotionId,
            Model model
    ) {
        // Load base lists
        List<Client> clients = clientService.findAll();
        List<Product> products = productService.findAll();

        model.addAttribute("clients", clients);
        model.addAttribute("products", products);

        List<Promotion> availablePromotions = new ArrayList<>();
        PriceSimulationResult simulation = null;

        if (clientId != null && productId != null) {
            Client client = clientService.findById(clientId);
            Product product = productService.findById(productId);

            LocalDate today = LocalDate.now();

            // Promotions assigned to the client and valid for the product
            availablePromotions = client.getPromotions().stream()
                    .filter(Promotion::isEnabled)
                    .filter(p -> (p.getStartDate() == null || !p.getStartDate().isAfter(today))
                            && (p.getEndDate() == null || !p.getEndDate().isBefore(today)))
                    .filter(p -> p.getPromotionProducts().stream()
                            .anyMatch(pp -> pp.getProduct().getId().equals(productId)))
                    .sorted(Comparator.comparing(Promotion::getName))
                    .collect(Collectors.toList());

            Promotion selectedPromotion = null;
            PromotionProduct selectedPromotionProduct = null;

            if (promotionId != null) {
                selectedPromotion = availablePromotions.stream()
                        .filter(p -> p.getId().equals(promotionId))
                        .findFirst()
                        .orElse(null);

                if (selectedPromotion != null) {
                    selectedPromotionProduct = selectedPromotion.getPromotionProducts().stream()
                            .filter(pp -> pp.getProduct().getId().equals(productId))
                            .findFirst()
                            .orElse(null);
                }
            }

            simulation = simulationService.simulate(product, client, selectedPromotion, selectedPromotionProduct);

            model.addAttribute("selectedClientId", clientId);
            model.addAttribute("selectedProductId", productId);
            model.addAttribute("selectedPromotionId", promotionId);
        }

        model.addAttribute("availablePromotions", availablePromotions);
        model.addAttribute("simulation", simulation);

        return "admin/price_simulator";
    }
}
