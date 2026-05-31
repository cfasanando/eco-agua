package com.ecoamazonas.eco_agua.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByActiveTrueOrderByNameAsc();
}
