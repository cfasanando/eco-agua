package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center.lifecycle")
public class Matrix26LifecycleProperties {

    private boolean enabled = true;
    private Set<String> allowedInstanceCodes = new LinkedHashSet<>(Set.of("matrix26-appearance-lab"));
    private int maximumVerifiedBackupAgeHours = 72;
    private int minimumReasonLength = 10;

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

    public int getMaximumVerifiedBackupAgeHours() {
        return maximumVerifiedBackupAgeHours;
    }

    public void setMaximumVerifiedBackupAgeHours(int maximumVerifiedBackupAgeHours) {
        this.maximumVerifiedBackupAgeHours = maximumVerifiedBackupAgeHours;
    }

    public int getMinimumReasonLength() {
        return minimumReasonLength;
    }

    public void setMinimumReasonLength(int minimumReasonLength) {
        this.minimumReasonLength = minimumReasonLength;
    }
}
