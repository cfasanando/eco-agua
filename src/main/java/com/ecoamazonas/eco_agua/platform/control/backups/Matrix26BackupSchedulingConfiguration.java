package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "matrix26.control-center.backups.scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class Matrix26BackupSchedulingConfiguration {
}
