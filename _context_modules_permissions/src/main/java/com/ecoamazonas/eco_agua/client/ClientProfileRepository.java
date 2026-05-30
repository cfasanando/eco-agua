package com.ecoamazonas.eco_agua.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {
    // Default JPA methods are enough (findAll, save, deleteById, deleteAllById, etc.)
}
