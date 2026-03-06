package com.ecoamazonas.eco_agua.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, length = 20, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "active", nullable = false)
    private Integer active;

    @Column(name = "rol")
    private Integer legacyRol;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Role> roles = new HashSet<>();

    public UserAccount() {
        // Default constructor required by JPA
    }

    @PrePersist
    protected void onCreate() {
        // Ensure registrationDate is never null on insert
        if (this.registrationDate == null) {
            this.registrationDate = LocalDateTime.now();
        }

        // Ensure active has a default value if not explicitly set
        if (this.active == null) {
            this.active = 1; // 1 = active, 0 = inactive
        }
    }

    public boolean isActive() {
        return active != null && active == 1;
    }

    // getters and setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public Integer getLegacyRol() {
        return legacyRol;
    }

    public void setLegacyRol(Integer legacyRol) {
        this.legacyRol = legacyRol;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
