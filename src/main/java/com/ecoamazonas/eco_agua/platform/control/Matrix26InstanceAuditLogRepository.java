package com.ecoamazonas.eco_agua.platform.control;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Matrix26InstanceAuditLogRepository extends JpaRepository<Matrix26InstanceAuditLog, Long> {

    List<Matrix26InstanceAuditLog> findTop100ByOrderByCreatedAtDesc();

    List<Matrix26InstanceAuditLog> findTop50ByInstance_IdOrderByCreatedAtDesc(Long instanceId);
}
