package com.ecoamazonas.eco_agua.security;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash,
            LocalDateTime now
    );

    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :usedAt where t.user = :user and t.usedAt is null")
    int markActiveTokensAsUsed(@Param("user") UserAccount user, @Param("usedAt") LocalDateTime usedAt);
}
