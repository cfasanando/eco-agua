package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByCode(String code);

    default List<Permission> findAllOrdered() {
        return findAll(Sort.by("code").ascending());
    }
}
