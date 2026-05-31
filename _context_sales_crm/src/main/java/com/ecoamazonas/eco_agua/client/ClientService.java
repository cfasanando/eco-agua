package com.ecoamazonas.eco_agua.client;

import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final PromotionRepository promotionRepository;

    public ClientService(
            ClientRepository clientRepository,
            ClientProfileRepository clientProfileRepository,
            PromotionRepository promotionRepository
    ) {
        this.clientRepository = clientRepository;
        this.clientProfileRepository = clientProfileRepository;
        this.promotionRepository = promotionRepository;
    }

    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return clientRepository.findAll(Sort.by("name").ascending());
    }

    @Transactional
    public void saveFromForm(
            Long id,
            String name,
            DocumentType docType,
            String docNumber,
            String phone,
            String address,
            String reference,
            Long profileId,
            List<Long> promotionIds,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        Client client;

        if (id == null) {
            client = new Client();
        } else {
            client = clientRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Client not found with id " + id));
        }

        String normalizedName = normalizeRequired(name, "El nombre del cliente es obligatorio.");
        DocumentType normalizedDocType = docType != null ? docType : DocumentType.NONE;
        String normalizedDocNumber = normalizeDocNumber(normalizedDocType, docNumber);

        client.setName(normalizedName);
        client.setDocType(normalizedDocType);
        client.setDocNumber(normalizedDocNumber);
        client.setPhone(normalizeNullable(phone));
        client.setAddress(normalizeNullable(address));
        client.setReference(normalizeNullable(reference));
        client.setLatitude(latitude);
        client.setLongitude(longitude);

        ClientProfile profile = clientProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Client profile not found with id " + profileId));

        client.setProfile(profile);

        Set<Promotion> promotions = new HashSet<>();
        if (promotionIds != null && !promotionIds.isEmpty()) {
            promotions.addAll(promotionRepository.findAllById(promotionIds));
        }

        client.setPromotions(promotions);

        clientRepository.save(client);
    }

    @Transactional
    public void delete(Long id) {
        clientRepository.deleteById(id);
    }

    @Transactional
    public void deleteBulk(List<Long> ids) {
        clientRepository.deleteAllById(ids);
    }

    @Transactional(readOnly = true)
    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id " + id));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeDocNumber(DocumentType docType, String docNumber) {
        if (docType == DocumentType.NONE) {
            return "S/N";
        }

        String normalized = normalizeNullable(docNumber);
        if (normalized == null) {
            return "PENDIENTE";
        }

        return normalized.toUpperCase();
    }
}