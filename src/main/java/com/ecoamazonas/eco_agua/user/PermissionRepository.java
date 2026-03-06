package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    default List<Permission> findAllOrdered() {
        return findAll(Sort.by("code").ascending());
    }
}
