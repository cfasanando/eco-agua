package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center.lifecycle.decommission")
public class Matrix26DecommissionProperties {

    private boolean enabled = true;
    private Set<String> allowedInstanceCodes = new LinkedHashSet<>(Set.of("matrix26-appearance-lab"));
    private int minimumReasonLength = 10;
    private int defaultRetentionDays = 30;
    private int minimumRetentionDays = 1;
    private int maximumRetentionDays = 3650;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getAllowedInstanceCodes() {
        return allowedInstanceCodes;
    }

    public void setAllowedInstanceCodes(Set<String> allowedInstanceCodes) {
        this.allowedInstanceCodes = allowedInstanceCodes == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(allowedInstanceCodes);
    }

    public int getMinimumReasonLength() {
        return minimumReasonLength;
    }

    public void setMinimumReasonLength(int minimumReasonLength) {
        this.minimumReasonLength = minimumReasonLength;
    }

    public int getDefaultRetentionDays() {
        return defaultRetentionDays;
    }

    public void setDefaultRetentionDays(int defaultRetentionDays) {
        this.defaultRetentionDays = defaultRetentionDays;
    }

    public int getMinimumRetentionDays() {
        return minimumRetentionDays;
    }

    public void setMinimumRetentionDays(int minimumRetentionDays) {
        this.minimumRetentionDays = minimumRetentionDays;
    }

    public int getMaximumRetentionDays() {
        return maximumRetentionDays;
    }

    public void setMaximumRetentionDays(int maximumRetentionDays) {
        this.maximumRetentionDays = maximumRetentionDays;
    }
}
