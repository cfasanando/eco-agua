package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PersonalFinanceCurrentUserService {

    private final UserAccountRepository userAccountRepository;

    public PersonalFinanceCurrentUserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Authenticated user is required for GastoClaro.");
        }
        return userAccountRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User account not found: " + authentication.getName()));
    }

    public String currentUsername() {
        return currentUser().getUsername();
    }
}
