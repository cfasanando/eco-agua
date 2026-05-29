package com.ecoamazonas.eco_agua.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

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
                // Public website pages
                .requestMatchers("/", "/portal", "/catalogo", "/order/whatsapp").permitAll()

                // Blog administration must stay protected
                .requestMatchers("/blog/admin/**").authenticated()
                // Public blog pages
                .requestMatchers("/blog", "/blog/**").permitAll()

                // Static resources
                .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**", "/webjars/**")
                    .permitAll()

                // Authentication pages
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/logout").permitAll()

                // Internal administration areas
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/marketing/admin/**").authenticated()

                // Any other route requires login
                .anyRequest().authenticated()
            )
            // Custom login form
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // Logout must be performed through POST to keep CSRF protection enabled.
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Custom database-backed authentication
            .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
