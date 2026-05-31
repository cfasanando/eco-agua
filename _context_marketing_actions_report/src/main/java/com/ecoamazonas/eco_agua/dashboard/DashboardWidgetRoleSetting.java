package com.ecoamazonas.eco_agua.dashboard;

import jakarta.persistence.*;

@Entity
@Table(
        name = "dashboard_widget_role_setting",
        uniqueConstraints = @UniqueConstraint(name = "uk_dashboard_widget_role", columnNames = {"role_code", "widget_key"})
)
public class DashboardWidgetRoleSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 100)
    private String roleCode;

    @Column(name = "widget_key", nullable = false, length = 100)
    private String widgetKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public DashboardWidgetRoleSetting() {
        // Required by JPA
    }

    public DashboardWidgetRoleSetting(String roleCode, String widgetKey, boolean enabled) {
        this.roleCode = roleCode;
        this.widgetKey = widgetKey;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getWidgetKey() {
        return widgetKey;
    }

    public void setWidgetKey(String widgetKey) {
        this.widgetKey = widgetKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
