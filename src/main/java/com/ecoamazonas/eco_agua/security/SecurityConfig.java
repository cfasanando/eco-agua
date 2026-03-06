package com.ecoamazonas.eco_agua.security;

import com.ecoamazonas.eco_agua.security.DatabaseUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;

    public SecurityConfig(DatabaseUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages (web público)
                .requestMatchers("/", "/portal", "/catalogo", "/order/whatsapp").permitAll()

                // Primero protegemos la parte admin del blog
                .requestMatchers("/blog/admin/**").authenticated()
                // El resto del blog es público
                .requestMatchers("/blog", "/blog/**").permitAll()

                // Recursos estáticos
                .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**", "/webjars/**")
                    .permitAll()

                // Auth pages
                .requestMatchers("/login", "/error").permitAll()

                // Zonas internas de administración
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/marketing/admin/**").authenticated()

                // Cualquier otra ruta requiere login
                .anyRequest().authenticated()
            )
            // Custom login form
            .formLogin(form -> form
                .loginPage("/login")              // GET /login shows the form
                .loginProcessingUrl("/login")     // POST /login is handled by Spring Security
                .defaultSuccessUrl("/", true)     // Redirect after successful login
                .failureUrl("/login?error=true")  // Redirect on error
                .permitAll()
            )
            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            // Our custom UserDetailsService
            .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expose AuthenticationManager if needed
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
