package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByUsernameAndActive(String username, Integer active);
}
