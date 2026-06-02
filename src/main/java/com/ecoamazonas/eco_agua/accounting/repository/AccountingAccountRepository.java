package com.ecoamazonas.eco_agua.accounting.repository;

import com.ecoamazonas.eco_agua.accounting.AccountingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, Long> {

    List<AccountingAccount> findAllByOrderByCodeAsc();

    List<AccountingAccount> findByActiveTrueOrderByCodeAsc();

    Optional<AccountingAccount> findByCode(String code);

    boolean existsByCode(String code);
}
