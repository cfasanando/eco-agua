package com.ecoamazonas.eco_agua.client;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientProfileService {

    private final ClientProfileRepository repository;

    public ClientProfileService(ClientProfileRepository repository) {
        this.repository = repository;
    }

    public List<ClientProfile> findAll() {
        // Return profiles ordered by name
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    public void saveFromForm(ClientProfile formProfile) {
        // Normalize null price
        if (formProfile.getSuggestedPrice() == null) {
            formProfile.setSuggestedPrice(BigDecimal.ZERO);
        }

        if (formProfile.getId() == null) {
            // Create new profile
            if (!formProfile.isActive()) {
                formProfile.setActive(true);
            }
            repository.save(formProfile);
            return;
        }

        // Update existing profile
        ClientProfile existing = repository.findById(formProfile.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found with id " + formProfile.getId()
                ));

        existing.setName(formProfile.getName());
        existing.setDescription(formProfile.getDescription());
        existing.setSuggestedPrice(formProfile.getSuggestedPrice());
        // Active stays as is for now, or map from form if needed

        repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteMany(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteAllById(ids);
    }
}
