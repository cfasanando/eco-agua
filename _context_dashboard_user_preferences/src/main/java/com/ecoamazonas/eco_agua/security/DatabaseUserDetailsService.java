package com.ecoamazonas.eco_agua.security;

import com.ecoamazonas.eco_agua.user.Permission;
import com.ecoamazonas.eco_agua.user.Role;
import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User is inactive: " + username);
        }

        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            addAuthority(authorities, role.getCode());
            for (Permission permission : role.getPermissions()) {
                addAuthority(authorities, permission.getCode());
            }
        }

        return new User(user.getUsername(), user.getPassword(), authorities);
    }

    private void addAuthority(Set<SimpleGrantedAuthority> authorities, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        authorities.add(new SimpleGrantedAuthority(value.trim()));
    }
}
