package com.ecoamazonas.eco_agua.security;

import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User is inactive: " + username);
        }

        // Use a more specific type here
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getCode()))
            .toList();

        return new User(
            user.getUsername(),
            user.getPassword(),
            authorities   // OK: constructor accepts Collection<? extends GrantedAuthority>
        );
    }
}
