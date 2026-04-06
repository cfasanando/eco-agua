package com.ecoamazonas.eco_agua.reorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReorderFollowUpRepository extends JpaRepository<ReorderFollowUp, Long> {

    List<ReorderFollowUp> findByReferenceDateOrderByUpdatedAtDescIdDesc(LocalDate referenceDate);

    Optional<ReorderFollowUp> findTopByClientIdAndReferenceDateOrderByUpdatedAtDescIdDesc(Long clientId, LocalDate referenceDate);

    List<ReorderFollowUp> findByClientIdOrderByReferenceDateDescUpdatedAtDescIdDesc(Long clientId);
}
