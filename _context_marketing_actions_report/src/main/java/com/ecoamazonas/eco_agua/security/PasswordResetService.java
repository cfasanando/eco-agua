package com.ecoamazonas.eco_agua.security;

import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_VALIDITY_MINUTES = 30;
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserAccountRepository userAccountRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PasswordResetRequestResult requestPasswordReset(String username) {
        String normalizedUsername = normalizeUsername(username);
        Optional<UserAccount> optionalUser = userAccountRepository.findByUsernameAndActive(normalizedUsername, 1);

        if (optionalUser.isEmpty()) {
            return new PasswordResetRequestResult(Optional.empty());
        }

        UserAccount user = optionalUser.get();
        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepository.markActiveTokensAsUsed(user, now);

        String rawToken = generateRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(now.plusMinutes(TOKEN_VALIDITY_MINUTES));
        token.setCreatedAt(now);
        passwordResetTokenRepository.save(token);

        LOG.info("Password reset token generated for user {}. The token expires in {} minutes.",
                user.getUsername(), TOKEN_VALIDITY_MINUTES);

        return new PasswordResetRequestResult(Optional.of(rawToken));
    }

    @Transactional(readOnly = true)
    public boolean isValidToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        return passwordResetTokenRepository
                .findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hashToken(rawToken), LocalDateTime.now())
                .isPresent();
    }

    @Transactional
    public PasswordResetResult resetPassword(String rawToken, String password, String confirmPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            return PasswordResetResult.error("El enlace de recuperación no es válido.");
        }

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return PasswordResetResult.error("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        if (!password.equals(confirmPassword)) {
            return PasswordResetResult.error("Las contraseñas no coinciden.");
        }

        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository
                .findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hashToken(rawToken), LocalDateTime.now());

        if (optionalToken.isEmpty()) {
            return PasswordResetResult.error("El enlace de recuperación expiró o ya fue utilizado.");
        }

        PasswordResetToken resetToken = optionalToken.get();
        UserAccount user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userAccountRepository.save(user);

        LocalDateTime now = LocalDateTime.now();
        resetToken.setUsedAt(now);
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.markActiveTokensAsUsed(user, now);

        return PasswordResetResult.success("La contraseña fue actualizada correctamente. Ya puedes iniciar sesión.");
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    public record PasswordResetRequestResult(Optional<String> rawToken) {
    }

    public record PasswordResetResult(boolean success, String message) {
        public static PasswordResetResult success(String message) {
            return new PasswordResetResult(true, message);
        }

        public static PasswordResetResult error(String message) {
            return new PasswordResetResult(false, message);
        }
    }
}
